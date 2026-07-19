package logisticspipes;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.client.ClientManager;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.data.recipes.LPRecipeProvider;
import logisticspipes.datafixer.LPDataFixer;
import logisticspipes.logistics.LogisticsFluidManager;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.ProxyManager;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.SpecialInventoryHandlerManager;
import logisticspipes.proxy.SpecialTankHandlerManager;
import logisticspipes.proxy.progressprovider.MachineProgressProvider;
import logisticspipes.proxy.recipeproviders.LogisticsCraftingTable;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection;
import logisticspipes.proxy.specialconnection.SpecialTileConnection;
import logisticspipes.proxy.specialtankhandler.SpecialTankHandler;
import logisticspipes.recipes.CraftingRecipes;
import logisticspipes.recipes.LPChipRecipes;
import logisticspipes.recipes.ModuleChippedCraftingRecipes;
import logisticspipes.recipes.PipeChippedCraftingRecipes;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.UpgradeChippedCraftingRecipes;
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
import logisticspipes.ticks.ServerPacketBufferHandlerThread;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.InventoryUtilFactory;
import logisticspipes.utils.RoutedItemHelper;
import logisticspipes.world.item.LPCreativeModeTabs;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.world.item.crafting.LPRecipeSerializers;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
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
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.compat.TheOneProbeIntegration;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.ServerConfigurationManager;
import network.rs485.logisticspipes.property.PropertyUpdaterEventListener;
import network.rs485.util.SystemUtilKt;

@Mod(LPConstants.ID)
public class LogisticsPipes {

  @Getter
  private static final boolean DEBUG = !FMLEnvironment.production;
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
    modEventBus.addListener(this::handleAddPackFinders);
    modEventBus.addListener(this::handleGatherData);

    if (dist.isClient()) {
      ClientManager.init(modEventBus);
      modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    PacketHandler.register(modEventBus);
//    RailcraftEntityTypes.register(modEventBus);
    LPBlocks.register(modEventBus);
    LPItems.register(modEventBus);
//    RailcraftMobEffects.register(modEventBus);
    LPCreativeModeTabs.register(modEventBus);
    LPBlockEntityTypes.register(modEventBus);
//    TrackTypes.register(modEventBus);
//    RailcraftFluids.register(modEventBus);
//    RailcraftFluidTypes.register(modEventBus);
//    RailcraftMenuTypes.register(modEventBus);
//    RailcraftSoundEvents.register(modEventBus);
//    RailcraftParticleTypes.register(modEventBus);
    LPRecipeSerializers.register(modEventBus);
//    RailcraftRecipeTypes.register(modEventBus);
//    RailcraftGameEvents.register(modEventBus);
//    RailcraftDataSerializers.register(modEventBus);
//    RailcraftPoiTypes.register(modEventBus);
//    RailcraftVillagerProfession.register(modEventBus);
//    RailcraftLootModifiers.register(modEventBus);
//    RailcraftFeatures.register(modEventBus);
//    RailcraftStructureTypes.register(modEventBus);
//    RailcraftStructurePieces.register(modEventBus);
//    RailcraftCriteriaTriggers.register(modEventBus);
//    RailcraftAttachmentTypes.register(modEventBus);
//    RailcraftDataMaps.register(modEventBus);
      LPDataComponents.register(modEventBus);

    LPDataFixer.INSTANCE.init();
  }

  // Mod Events
  private void handleRegisterCapabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
        LPBlockEntityTypes.PIPE.get(), LogisticsTileGenericPipe::getItemCap);

    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
        LPBlockEntityTypes.PIPE.get(), LogisticsTileGenericPipe::getFluidCap);

    event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
        LPBlockEntityTypes.POWER_PROVIDER_RF.get(), LogisticsRFPowerProviderTileEntity::getEnergyStorageCap);
  }

  private void handleCommonSetup(FMLCommonSetupEvent event) {
    PacketHandler.initialize();
    NewGuiHandler.initialize();

    ProxyManager.load();

    SimpleServiceLocator.setPipeInformationManager(new PipeInformationManager());
    SimpleServiceLocator.setLogisticsFluidManager(new LogisticsFluidManager());

    if (ModList.get().isLoaded(LPConstants.theOneProbeModID)) {
      InterModComms.sendTo(LPConstants.theOneProbeModID, "getTheOneProbe",
          TheOneProbeIntegration.class::getName);
    }

    MainProxy.proxy.initModelLoader();

    registerRecipes();

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

    SimpleServiceLocator.setServerPacketBufferHandlerThread(new ServerPacketBufferHandlerThread());
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

    // BlockEntityTypes are registered via DeferredRegister in LPRegistries.
    MainProxy.proxy.registerTileEntities();
    MainProxy.proxy.registerParticles();

    FluidIdentifier.initFromForge(false);
  }

  private void handleAddPackFinders(AddPackFindersEvent event) {
//		if (event.getPackType() != PackType.SERVER_DATA) return;
//		event.addRepositorySource(consumer -> {
//			var test = new LPRecipePack()
//			Pack pack = Pack.readMetaAndCreate(
//					"logisticspipes:virtual_recipes",
//					Component.literal("LogisticsPipes virtual recipes"),
//					true,
//					id -> new LPRecipePack(),
//					PackType.SERVER_DATA,
//					Pack.Position.TOP,
//					PackSource.BUILT_IN);
//			if (pack != null) consumer.accept(pack);
//		});
  }

  private void handleGatherData(GatherDataEvent event) {
    var generator = event.getGenerator();
    var packOutput = generator.getPackOutput();
    var lookupProvider = event.getLookupProvider();
    var fileHelper = event.getExistingFileHelper();

    generator.addProvider(event.includeServer(),
        new LPRecipeProvider(packOutput, lookupProvider));
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
    if (FMLEnvironment.dist == Dist.CLIENT) {
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

  private void registerRecipes() {
    // Register the NBT ingredient serializer so programmer-based recipes can be
    // parsed from JSON (generated by the recipe providers into LPRecipePack).
    //TODO fix
//		CraftingHelper.register(NBTIngredient.ID, NBTIngredient.SERIALIZER);

    RecipeManager.recipeProvider.add(new LPChipRecipes());
    RecipeManager.recipeProvider.add(new UpgradeChippedCraftingRecipes());
    RecipeManager.recipeProvider.add(new ModuleChippedCraftingRecipes());
    RecipeManager.recipeProvider.add(new PipeChippedCraftingRecipes());
    RecipeManager.recipeProvider.add(new CraftingRecipes());
    RecipeManager.loadRecipes();
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
