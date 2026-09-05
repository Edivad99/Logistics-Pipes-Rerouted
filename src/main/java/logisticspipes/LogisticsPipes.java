package logisticspipes;

import java.util.function.Consumer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.client.ClientManager;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.data.LPParticleProvider;
import logisticspipes.data.LPSpriteSourceProvider;
import logisticspipes.data.models.LPModelProvider;
import logisticspipes.data.recipes.LPRecipeProvider;
import logisticspipes.logistics.LogisticsFluidManager;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.network.PacketHandler;
import logisticspipes.particle.LPParticleTypes;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.PowerProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.SpecialInventoryHandlerManager;
import logisticspipes.proxy.SpecialTankHandlerManager;
import logisticspipes.proxy.progressprovider.MachineProgressProvider;
import logisticspipes.proxy.recipeproviders.LogisticsCraftingTable;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection;
import logisticspipes.proxy.specialconnection.SpecialTileConnection;
import logisticspipes.proxy.specialtankhandler.SpecialTankHandler;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.channels.ChannelManagerProvider;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.routing.pathfinder.changedetection.BlockChangeListener;
import logisticspipes.ticks.HudUpdateTick;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.ticks.RoutingTableUpdateThread;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.InventoryUtilFactory;
import logisticspipes.utils.RoutedItemHelper;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.item.ItemPipeSignCreator;
import logisticspipes.world.item.LPCreativeModeTabs;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.world.item.crafting.LPRecipeSerializers;
import logisticspipes.world.item.crafting.RegisterProgrammerRecipes;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.ServerConfigurationManager;
import network.rs485.logisticspipes.property.PropertyUpdaterEventListener;
import network.rs485.util.SystemUtilKt;

@Mod(LPConstants.ID)
public class LogisticsPipes {

    @Getter
    private static final boolean DEBUG = !FMLEnvironment.isProduction();
    public static final Logger LOG = LogUtils.getLogger();

    @Nullable
    private static ClientConfiguration playerConfig;
    @Nullable
    private static ServerConfigurationManager serverConfigManager;

    @Nullable
    private Consumer<ServerStartedEvent> minecraftTestStartMethod = null;

    public LogisticsPipes(ModContainer modContainer, Dist dist) {
        NeoForge.EVENT_BUS.register(this);

        LPConfigs.registerConfig(modContainer);

        var modEventBus = modContainer.getEventBus();
        modEventBus.addListener(this::handleRegisterCapabilities);
        modEventBus.addListener(this::handleCommonSetup);
        modEventBus.addListener(this::handleLoadComplete);
        modEventBus.addListener(this::handleGatherData);

        if (dist.isClient()) {
            ClientManager.init(modEventBus);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        PacketHandler.register(modEventBus);
        LPBlocks.register(modEventBus);
        LPItems.register(modEventBus);
        LPCreativeModeTabs.register(modEventBus);
        LPBlockEntityTypes.register(modEventBus);
        LPMenuTypes.register(modEventBus);
        LPParticleTypes.register(modEventBus);
        LPRecipeSerializers.register(modEventBus);
        LPDataComponents.register(modEventBus);
    }

    // Mod Events
    private void handleRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK,
            LPBlockEntityTypes.PIPE.get(), LogisticsTileGenericPipe::getItemCap);
        event.registerBlockEntity(Capabilities.Item.BLOCK,
            LPBlockEntityTypes.CRAFTING_TABLE.get(), LogisticsCraftingTableBlockEntity::getItemCap);

        event.registerBlockEntity(Capabilities.Fluid.BLOCK,
            LPBlockEntityTypes.PIPE.get(), LogisticsTileGenericPipe::getFluidCap);

        event.registerBlockEntity(Capabilities.Energy.BLOCK,
            LPBlockEntityTypes.POWER_JUNCTION.get(), LogisticsPowerJunctionBlockEntity::getEnergyStorageCap);
        event.registerBlockEntity(Capabilities.Energy.BLOCK,
            LPBlockEntityTypes.POWER_PROVIDER_RF.get(), LogisticsRFPowerProviderTileEntity::getEnergyStorageCap);
    }

    private void handleCommonSetup(FMLCommonSetupEvent event) {

        SimpleServiceLocator.setPowerProxy(new PowerProxy());
        ItemPipeSignCreator.registerPipeSignTypes();

        SimpleServiceLocator.setPipeInformationManager(new PipeInformationManager());
        SimpleServiceLocator.setLogisticsFluidManager(new LogisticsFluidManager());

        /*if (ModList.get().isLoaded(LPConstants.theOneProbeModID)) {
            InterModComms.sendTo(LPConstants.theOneProbeModID, "getTheOneProbe",
                TheOneProbeIntegration.class::getName);
        }*/

        RegisterProgrammerRecipes.loadRecipes();

        RouterManager manager = new RouterManager();
        SimpleServiceLocator.setRouterManager(manager);
        SimpleServiceLocator.setChannelConnectionManager(manager);
        SimpleServiceLocator.setSecurityStationManager(manager);
        SimpleServiceLocator.setLogisticsManager(new LogisticsManager());
        SimpleServiceLocator.setInventoryUtilFactory(new InventoryUtilFactory());
        SimpleServiceLocator.setSpecialConnectionHandler(new SpecialPipeConnection());
        SimpleServiceLocator.setSpecialConnectionHandler(new SpecialTileConnection());
        SimpleServiceLocator.setSpecialTankHandler(new SpecialTankHandler());
        SimpleServiceLocator.setMachineProgressProvider(new MachineProgressProvider());
        SimpleServiceLocator.setRoutedItemHelper(new RoutedItemHelper());
        SimpleServiceLocator.setChannelManagerProvider(new ChannelManagerProvider());

        NeoForge.EVENT_BUS.register(new LPTickHandler());
        NeoForge.EVENT_BUS.register(new QueuedTasks());
        NeoForge.EVENT_BUS.register(new LogisticsEventListener());
        NeoForge.EVENT_BUS.register(new LPChatListener());
        NeoForge.EVENT_BUS.register(new BlockChangeListener());
        NeoForge.EVENT_BUS.register(PropertyUpdaterEventListener.INSTANCE);

        for (int i = 0; i < LPConfigs.COMMON.MULTI_THREAD_NUMBER.getAsInt(); i++) {
            new RoutingTableUpdateThread(i);
        }

        if (isTesting()) {
            final Class<?> testClass;
            try {
                testClass = Class.forName("network.rs485.logisticspipes.integration.MinecraftTest");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Error loading minecraft test class", e);
            }
            try {
                final Object minecraftTestInstance = testClass.getDeclaredField("INSTANCE").get(null);
                final java.lang.reflect.Method serverStartMethod = testClass
                    .getDeclaredMethod("serverStart", ServerStartedEvent.class);
                minecraftTestStartMethod = (ServerStartedEvent serverStartedEvent) -> {
                    try {
                        serverStartMethod.invoke(minecraftTestInstance, serverStartedEvent);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Could not run server started hook in " + minecraftTestInstance, e);
                    }
                };
                NeoForge.EVENT_BUS.register(minecraftTestInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Error accessing minecraft test instance", e);
            }
        }
    }

    private void handleLoadComplete(FMLLoadCompleteEvent event) {
        SpecialInventoryHandlerManager.load();
        SpecialTankHandlerManager.load();

        // Dead-mod integrations removed (BuildCraft, Thermal Dynamics/Expansion, IC2, EnderCore,
        // MCMultiPart) — none of these mods exist on 1.20.1.
        SimpleServiceLocator.addCraftingRecipeProvider(new LogisticsCraftingTable());

        FluidIdentifier.initFromNeoForge(false);
    }

    private void handleGatherData(GatherDataEvent.Client event) {
        event.createProvider(LPRecipeProvider.Runner::new);
        event.createProvider(LPParticleProvider::new);
        event.createProvider(LPSpriteSourceProvider::new);
        event.createProvider(LPModelProvider::new);
    }

    // NeoForge Events
    @SubscribeEvent
    public void beforeStart(ServerAboutToStartEvent event) {
        ServerTickDispatcher.INSTANCE.serverStart();
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        if (minecraftTestStartMethod != null) {
            minecraftTestStartMethod.accept(event);
        }
    }

    @SubscribeEvent
    public void cleanup(ServerStoppingEvent event) {
        SimpleServiceLocator.routerManager.serverStopClean();
        QueuedTasks.clearAllTasks();
        HudUpdateTick.clearUpdateFlags();
        PipeItemsSatelliteLogistics.cleanup();
        PipeFluidSatellite.cleanup();
        ServerRouter.cleanup();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            LogisticsHUDRenderer.instance().clear();
        }
        ServerTickDispatcher.INSTANCE.cleanup();
        LogisticsPipes.serverConfigManager = null;
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        new LogisticsPipesCommand().register(event.getDispatcher());
    }

    public static boolean isTesting() {
        return SystemUtilKt.checkBooleanProperty("logisticspipes.test");
    }

    // ── Config accessors ─────────────────────────────────────────────────────

    public static ClientConfiguration getClientPlayerConfig() {
        if (LogisticsPipes.playerConfig == null) {
            LogisticsPipes.playerConfig = new ClientConfiguration();
        }
        return LogisticsPipes.playerConfig;
    }

    public static ServerConfigurationManager getServerConfigManager() {
        if (LogisticsPipes.serverConfigManager == null) {
            LogisticsPipes.serverConfigManager = new ServerConfigurationManager();
        }
        return LogisticsPipes.serverConfigManager;
    }
}
