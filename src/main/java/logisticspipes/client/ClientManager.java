package logisticspipes.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.ProgramCompilerScreen;
import logisticspipes.client.gui.tooltip.ClientModuleInventoryTooltip;
import logisticspipes.client.gui.tooltip.ModuleTooltipPlacement;
import logisticspipes.client.model.ObjModelManager;
import logisticspipes.client.model.pipe.PipeModelRegistration;
import logisticspipes.client.particle.GlowGeometryParticle;
import logisticspipes.client.particle.GlowParticleGroup;
import logisticspipes.client.particle.SparkParticle;
import logisticspipes.client.renderer.LPRenderTypes;
import logisticspipes.client.renderer.blockentity.LPBlockEntityRenderers;
import logisticspipes.client.renderer.item.LogisticsPipeItemRenderer;
import logisticspipes.client.renderer.item.LogisticsSolidBlockItemRenderer;
import logisticspipes.client.renderer.item.properties.CreatorMode;
import logisticspipes.client.renderer.item.properties.FluidTint;
import logisticspipes.client.renderer.item.properties.HasFluid;
import logisticspipes.particle.LPParticleTypes;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.inventory.ProgramCompilerMenu;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;
import network.rs485.logisticspipes.gui.WidgetScreenHudSuppressor;

public class ClientManager {

    private ClientManager() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientManager::handleRegisterRenderers);
        modEventBus.addListener(ClientManager::handleParticleRegistration);
        modEventBus.addListener(ClientManager::handleRegisterParticleGroups);
        modEventBus.addListener(LPRenderTypes::register);
        modEventBus.addListener(ClientManager::handleRegisterSpecialModelRenderers);
        modEventBus.addListener(ClientManager::handleRegisterMenuScreens);
        modEventBus.addListener(ClientManager::handleRegisterReloadListeners);
        modEventBus.addListener(ClientManager::handleRegisterTooltipComponents);
        modEventBus.addListener(ClientManager::handleRegisterRangeSelectItemModelProperties);
        modEventBus.addListener(ClientManager::handleRegisterConditionalItemModelProperties);
        modEventBus.addListener(ClientManager::handleRegisterItemTintSources);

        modEventBus.register(TextureRegistrar.class);
        modEventBus.register(PipeModelRegistration.class);

        //NeoForge.EVENT_BUS.register(ClientManager.class);

        NeoForge.EVENT_BUS.register(new RenderTickHandler());
        NeoForge.EVENT_BUS.register(ModuleTooltipPlacement.class);
        NeoForge.EVENT_BUS.register(WidgetScreenHudSuppressor.INSTANCE);
        SimpleServiceLocator.setClientPacketBufferHandlerThread(new ClientPacketBufferHandlerThread());
        //LPFontRenderer.Factory.asyncPreload();
    }

    // Mod events
    private static void handleRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(LPConstants.rl("obj_models"), new ObjModelManager());
    }

    private static void handleRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LPBlockEntityRenderers.register(event);
    }

    private static void handleParticleRegistration(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LPParticleTypes.SPARKLE.get(), SparkParticle.Provider::new);
    }

    /**
     * LP's laser effects draw untextured geometry, which the textured-quad group an unregistered
     * render type falls back to cannot express; registering here also places them in the frame's
     * particle draw order.
     */
    private static void handleRegisterParticleGroups(RegisterParticleGroupsEvent event) {
        event.register(GlowGeometryParticle.GROUP, GlowParticleGroup::new);
    }

    private static void handleRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(LPConstants.rl("solid_block"), LogisticsSolidBlockItemRenderer.Unbaked.MAP_CODEC);
        event.register(LPConstants.rl("pipe"), LogisticsPipeItemRenderer.Unbaked.MAP_CODEC);
    }

    private static void handleRegisterRangeSelectItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(LPConstants.rl("creator_mode"), CreatorMode.MAP_CODEC);
    }

    private static void handleRegisterConditionalItemModelProperties(RegisterConditionalItemModelPropertyEvent event) {
        // Whether the fluid container holds anything, picking the filled model.
        event.register(LPConstants.rl("has_fluid"), HasFluid.MAP_CODEC);
    }

    private static void handleRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(LPConstants.rl("fluid"), FluidTint.MAP_CODEC);
    }

    private static void handleRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ModuleInventoryTooltip.class, ClientModuleInventoryTooltip::new);
    }

    private static void handleRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LPMenuTypes.PROGRAM_COMPILER.get(),
            (MenuScreens.ScreenConstructor<ProgramCompilerMenu, AbstractContainerScreen<ProgramCompilerMenu>>) ProgramCompilerScreen::new);
    }
}
