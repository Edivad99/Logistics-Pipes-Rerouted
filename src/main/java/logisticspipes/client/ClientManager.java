package logisticspipes.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.ProgramCompilerScreen;
import logisticspipes.client.gui.tooltip.ClientModuleInventoryTooltip;
import logisticspipes.client.gui.tooltip.ModuleTooltipPlacement;
import logisticspipes.client.model.ObjModelManager;
import logisticspipes.client.model.pipe.PipeModelRegistration;
import logisticspipes.client.particle.SparkParticle;
import logisticspipes.client.renderer.blockentity.LPBlockEntityRenderers;
import logisticspipes.client.renderer.item.LogisticsPipeItemRenderer;
import logisticspipes.client.renderer.item.LogisticsSolidBlockItemRenderer;
import logisticspipes.particle.LPParticleTypes;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.FluidContainerRenderer;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.inventory.ProgramCompilerMenu;
import logisticspipes.world.item.ItemPipeSignCreator;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;
import logisticspipes.world.level.block.LPBlocks;
import network.rs485.logisticspipes.gui.WidgetScreenHudSuppressor;

public class ClientManager {

    private ClientManager() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientManager::handleClientSetup);
        modEventBus.addListener(ClientManager::handleRegisterRenderers);
        modEventBus.addListener(ClientManager::handleParticleRegistration);
        modEventBus.addListener(ClientManager::handleClientExtensions);
        modEventBus.addListener(ClientManager::handleRegisterMenuScreens);
        modEventBus.addListener(ClientManager::handleRegisterReloadListeners);
        modEventBus.addListener(ClientManager::handleRegisterTooltipComponents);

        modEventBus.register(TextureRegistrar.class);
        modEventBus.register(PipeModelRegistration.class);
        modEventBus.register(FluidContainerRenderer.class);

        //NeoForge.EVENT_BUS.register(ClientManager.class);

        NeoForge.EVENT_BUS.register(new RenderTickHandler());
        NeoForge.EVENT_BUS.register(ModuleTooltipPlacement.class);
        NeoForge.EVENT_BUS.register(WidgetScreenHudSuppressor.INSTANCE);
        SimpleServiceLocator.setClientPacketBufferHandlerThread(new ClientPacketBufferHandlerThread());
        //LPFontRenderer.Factory.asyncPreload();
    }

    // Mod events
    private static void handleClientSetup(FMLClientSetupEvent event) {
        // Texture atlas sprites and item/block models are supplied declaratively via
        // JSON in assets/logisticspipes/models/** in 1.20.1 — no code registration.
        // BlockEntityRenderer for the pipe BE is registered via
        // EntityRenderersEvent.RegisterRenderers (see registerRenderers below).
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                LPBlocks.PIPE.get(),
                RenderType.cutout());
        });

        // Fluid container "filled" model predicate (client-only class, stays in the guard).
        FluidContainerRenderer.registerItemProperties();

        // Which sign the creator will place. LP1 carried this on the stack's metadata, which
        // picked the item model on its own; a predicate is how that is expressed now, and
        // without it the tool looks identical whatever type is selected.
        ItemProperties.register(
            LPItems.SIGN_CREATOR.get(),
            LPConstants.rl("creator_mode"),
            (stack, level, entity, seed) -> ItemPipeSignCreator.getMode(stack));

        // OBJ geometry is no longer preloaded here: ObjModelManager parses it as a resource
        // reload listener (see handleRegisterReloadListeners), off the render thread and
        // after the texture atlases exist.
    }

    private static void handleRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // Parses the OBJ geometry on every resource reload, off the render thread. Replaces
        // the safeLoadModels(...) block in handleClientSetup, which read the files straight
        // off the classpath at mod init — before the texture atlases existed and beyond the
        // reach of resource packs.
        event.registerReloadListener(new ObjModelManager());
    }

    private static void handleRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LPBlockEntityRenderers.register(event);
    }

    private static void handleParticleRegistration(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LPParticleTypes.SPARKLE.get(), SparkParticle.Provider::new);
    }

    private static void handleClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {

                               @Override
                               public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                                   return LogisticsSolidBlockItemRenderer.INSTANCE;
                               }
                           },
            LPItems.ITEM_FRAME,
            LPItems.ITEM_POWER_JUNCTION,
            LPItems.ITEM_SECURITY_STATION,
            LPItems.ITEM_CRAFTER,
            LPItems.ITEM_CRAFTER_FUZZY,
            LPItems.ITEM_STATISTICS_TABLE,
            LPItems.ITEM_POWER_PROVIDER_RF,
            LPItems.ITEM_PROGRAM_COMPILER);

        event.registerItem(new IClientItemExtensions() {

                               @Override
                               public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                                   return LogisticsPipeItemRenderer.INSTANCE;
                               }
                           },
            LPItems.PIPE_BASIC,
            LPItems.PIPE_REQUEST,
            LPItems.PIPE_REQUEST_MK2,
            LPItems.PIPE_PROVIDER,
            LPItems.PIPE_CRAFTING,
            LPItems.PIPE_SATELLITE,
            LPItems.PIPE_SUPPLIER,
            LPItems.PIPE_CHASSIS_MK1,
            LPItems.PIPE_CHASSIS_MK2,
            LPItems.PIPE_CHASSIS_MK3,
            LPItems.PIPE_CHASSIS_MK4,
            LPItems.PIPE_CHASSIS_MK5,
            LPItems.PIPE_REMOTE_ORDERER,
            LPItems.PIPE_INV_SYS_CONNECTOR,
            LPItems.PIPE_SYSTEM_ENTRANCE,
            LPItems.PIPE_SYSTEM_DESTINATION,
            LPItems.PIPE_FIREWALL,
            LPItems.PIPE_REQUEST_TABLE,
            LPItems.PIPE_UNROUTED,
            LPItems.PIPE_FLUID_SUPPLIER,
            LPItems.PIPE_FLUID_INSERTION,
            LPItems.PIPE_FLUID_PROVIDER,
            LPItems.PIPE_FLUID_REQUEST,
            LPItems.PIPE_FLUID_EXTRACTOR,
            LPItems.PIPE_FLUID_SATELLITE,
            LPItems.PIPE_FLUID_SUPPLIER_MK2,
            LPItems.PIPE_HS_CURVE,
            LPItems.PIPE_HS_SPEEDUP,
            LPItems.PIPE_HS_S_CURVE,
            LPItems.PIPE_HS_LINE,
            LPItems.PIPE_HS_GAIN
        );
    }

    private static void handleRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ModuleInventoryTooltip.class, ClientModuleInventoryTooltip::new);
    }

    private static void handleRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LPMenuTypes.PROGRAM_COMPILER.get(),
            (MenuScreens.ScreenConstructor<ProgramCompilerMenu, AbstractContainerScreen<ProgramCompilerMenu>>) ProgramCompilerScreen::new);
    }
}
