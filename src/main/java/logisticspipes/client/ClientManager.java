package logisticspipes.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
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
import logisticspipes.client.renderer.pip.SideConfigSceneRenderer;
import logisticspipes.client.renderer.pip.SideConfigSceneState;
import logisticspipes.client.renderer.item.LogisticsSolidBlockItemRenderer;
import logisticspipes.client.renderer.item.properties.CreatorMode;
import logisticspipes.client.renderer.item.properties.FluidTint;
import logisticspipes.client.renderer.item.properties.HasFluid;
import logisticspipes.particle.LPParticleTypes;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.client.gui.screen.FirewallScreen;
import logisticspipes.client.gui.screen.ChassisPipeScreen;
import logisticspipes.client.gui.screen.FluidOrdererScreen;
import logisticspipes.client.gui.screen.RequestTableScreen;
import logisticspipes.client.gui.screen.NormalOrdererScreen;
import logisticspipes.client.gui.screen.NormalMk2OrdererScreen;
import logisticspipes.client.gui.screen.CraftingPipeScreen;
import logisticspipes.client.gui.screen.FreqCardContentScreen;
import logisticspipes.client.gui.screen.InvSysConnectorScreen;
import logisticspipes.client.gui.screen.SupplierPipeScreen;
import logisticspipes.client.gui.screen.AdvancedExtractorScreen;
import logisticspipes.client.gui.screen.FluidSupplierModuleScreen;
import logisticspipes.client.gui.screen.OreDictItemSinkScreen;
import logisticspipes.client.gui.screen.StringBasedItemSinkScreen;
import logisticspipes.client.gui.screen.SimpleFilterScreen;
import logisticspipes.client.gui.screen.SneakyConfiguratorScreen;
import logisticspipes.client.gui.screen.HUDSettingsScreen;
import logisticspipes.logic.gui.LogicLayoutGui;
import logisticspipes.client.gui.screen.FluidBasicScreen;
import logisticspipes.client.gui.screen.LogisticsSettingsScreen;
import logisticspipes.client.gui.screen.PipeControllerScreen;
import logisticspipes.client.gui.screen.ItemAmountSignCreationScreen;
import logisticspipes.client.gui.screen.FluidSupplierMk2PipeScreen;
import logisticspipes.client.gui.screen.FluidSupplierPipeScreen;
import logisticspipes.client.gui.screen.FluidTerminusScreen;
import logisticspipes.client.gui.screen.LogisticsCraftingTableScreen;
import logisticspipes.client.gui.screen.SatellitePipeScreen;
import logisticspipes.client.gui.screen.PowerJunctionScreen;
import logisticspipes.client.gui.screen.StatisticsScreen;
import logisticspipes.client.gui.screen.PowerProviderScreen;
import logisticspipes.client.gui.screen.SecurityStationScreen;
import logisticspipes.world.inventory.OrdererMenu;
import logisticspipes.world.inventory.LPMenuTypes;

import network.rs485.logisticspipes.gui.module.ItemSinkGui;
import network.rs485.logisticspipes.gui.module.ProviderGui;
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
        modEventBus.addListener(ClientManager::handleRegisterPictureInPictureRenderers);
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
    }

    // Mod events
    private static void handleRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(LPConstants.rl("obj_models"), new ObjModelManager());
    }

    private static void handleRegisterPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(SideConfigSceneState.class, SideConfigSceneRenderer::new);
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
        event.register(LPMenuTypes.PROGRAM_COMPILER.get(), ProgramCompilerScreen::new);
        event.register(LPMenuTypes.POWER_JUNCTION.get(), PowerJunctionScreen::new);
        event.register(LPMenuTypes.POWER_PROVIDER.get(), PowerProviderScreen::new);
        event.register(LPMenuTypes.SECURITY_STATION.get(), SecurityStationScreen::new);
        event.register(LPMenuTypes.AUTO_CRAFTING.get(), LogisticsCraftingTableScreen::new);
        event.register(LPMenuTypes.STATISTICS.get(), StatisticsScreen::new);
        event.register(LPMenuTypes.FLUID_SINK.get(), FluidBasicScreen::new);
        event.register(LPMenuTypes.FLUID_TERMINUS.get(), FluidTerminusScreen::new);
        event.register(LPMenuTypes.SATELLITE.get(), SatellitePipeScreen::new);
        event.register(LPMenuTypes.FIREWALL.get(), FirewallScreen::new);
        event.register(LPMenuTypes.FREQ_CARD.get(), FreqCardContentScreen::new);
        event.register(LPMenuTypes.SIMPLE_FILTER.get(), SimpleFilterScreen::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER_MODULE.get(), FluidSupplierModuleScreen::new);
        event.register(LPMenuTypes.CRAFTING_MODULE.get(), CraftingPipeScreen::new);
        event.register(LPMenuTypes.SNEAKY_DIRECTION.get(), SneakyConfiguratorScreen::new);
        event.register(LPMenuTypes.ACTIVE_SUPPLIER.get(), SupplierPipeScreen::new);
        event.register(LPMenuTypes.INV_SYS_CON.get(), InvSysConnectorScreen::new);
        event.register(LPMenuTypes.PIPE_CONTROLLER.get(), PipeControllerScreen::new);
        event.register(LPMenuTypes.PLAYER_SETTINGS.get(), LogisticsSettingsScreen::new);
        event.register(LPMenuTypes.ITEM_AMOUNT_SIGN.get(), ItemAmountSignCreationScreen::new);
        event.register(LPMenuTypes.REQUEST_TABLE.get(), RequestTableScreen::new);
        // The screen is generic in its menu -- the MK2 subclass narrows it -- so the plain
        // orderer has to name the type argument that a bare reference cannot infer.
        event.register(LPMenuTypes.ORDERER.get(), NormalOrdererScreen<OrdererMenu>::new);
        event.register(LPMenuTypes.ORDERER_MK2.get(), NormalMk2OrdererScreen::new);
        event.register(LPMenuTypes.FLUID_ORDERER.get(), FluidOrdererScreen::new);
        event.register(LPMenuTypes.CHASSIS.get(), ChassisPipeScreen::new);
        event.register(LPMenuTypes.ITEM_SINK.get(), ItemSinkGui::new);
        event.register(LPMenuTypes.PROVIDER.get(), ProviderGui::new);
        event.register(LPMenuTypes.ORE_DICT_ITEM_SINK.get(), OreDictItemSinkScreen::new);
        event.register(LPMenuTypes.STRING_BASED_ITEM_SINK.get(), StringBasedItemSinkScreen::new);
        event.register(LPMenuTypes.ADVANCED_EXTRACTOR.get(), AdvancedExtractorScreen::new);
        event.register(LPMenuTypes.HUD_SETTINGS.get(), HUDSettingsScreen::new);
        event.register(LPMenuTypes.LOGIC_CONTROLLER.get(), LogicLayoutGui::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER.get(), FluidSupplierPipeScreen::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER_MK2.get(), FluidSupplierMk2PipeScreen::new);
    }
}
