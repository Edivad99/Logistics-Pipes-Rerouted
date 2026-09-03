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
import logisticspipes.gui.GuiFirewall;
import logisticspipes.gui.GuiChassisPipe;
import logisticspipes.gui.orderer.FluidGuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.gui.orderer.NormalGuiOrderer;
import logisticspipes.gui.orderer.NormalMk2GuiOrderer;
import logisticspipes.gui.GuiCraftingPipe;
import logisticspipes.gui.GuiFreqCardContent;
import logisticspipes.gui.GuiSupplierPipe;
import logisticspipes.gui.modules.GuiAdvancedExtractor;
import logisticspipes.gui.modules.GuiFluidSupplier;
import logisticspipes.gui.modules.GuiOreDictItemSink;
import logisticspipes.gui.modules.GuiStringBasedItemSink;
import logisticspipes.gui.modules.GuiSimpleFilter;
import logisticspipes.gui.modules.GuiSneakyConfigurator;
import logisticspipes.gui.hud.GuiHUDSettings;
import logisticspipes.logic.gui.LogicLayoutGui;
import logisticspipes.gui.GuiFluidBasic;
import logisticspipes.gui.GuiLogisticsSettings;
import logisticspipes.gui.ItemAmountSignCreationGui;
import logisticspipes.gui.GuiFluidSupplierMk2Pipe;
import logisticspipes.gui.GuiFluidSupplierPipe;
import logisticspipes.gui.GuiFluidTerminus;
import logisticspipes.gui.GuiLogisticsCraftingTable;
import logisticspipes.gui.GuiSatellitePipe;
import logisticspipes.gui.GuiPowerJunction;
import logisticspipes.gui.GuiStatistics;
import logisticspipes.gui.GuiPowerProvider;
import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.world.inventory.AutoCraftingMenu;
import logisticspipes.world.inventory.ChassisMenu;
import logisticspipes.world.inventory.FluidOrdererMenu;
import logisticspipes.world.inventory.OrdererMenu;
import logisticspipes.world.inventory.OrdererMk2Menu;
import logisticspipes.world.inventory.ItemAmountSignMenu;
import logisticspipes.world.inventory.PlayerSettingsMenu;
import logisticspipes.world.inventory.RequestTableMenu;
import logisticspipes.world.inventory.FirewallMenu;
import logisticspipes.world.inventory.ActiveSupplierMenu;
import logisticspipes.world.inventory.AdvancedExtractorMenu;
import logisticspipes.world.inventory.CraftingModuleMenu;
import logisticspipes.world.inventory.FreqCardMenu;
import logisticspipes.world.inventory.SimpleFilterMenu;
import logisticspipes.world.inventory.SneakyDirectionMenu;
import logisticspipes.world.inventory.HudSettingsMenu;
import logisticspipes.world.inventory.LogicControllerMenu;
import logisticspipes.world.inventory.FluidSinkMenu;
import logisticspipes.world.inventory.FluidSupplierMenu;
import logisticspipes.world.inventory.FluidSupplierMk2Menu;
import logisticspipes.world.inventory.FluidTerminusMenu;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.inventory.ModuleAnalysisMenu;

import network.rs485.logisticspipes.gui.module.ItemSinkGui;
import network.rs485.logisticspipes.gui.module.ProviderGui;
import network.rs485.logisticspipes.inventory.container.ItemSinkContainer;
import network.rs485.logisticspipes.inventory.container.ProviderContainer;
import logisticspipes.world.inventory.SatelliteMenu;
import logisticspipes.world.inventory.PowerJunctionMenu;
import logisticspipes.world.inventory.PowerProviderMenu;
import logisticspipes.world.inventory.SecurityStationMenu;
import logisticspipes.world.inventory.StatisticsMenu;
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
        event.register(LPMenuTypes.POWER_JUNCTION.get(),
            (MenuScreens.ScreenConstructor<PowerJunctionMenu, AbstractContainerScreen<PowerJunctionMenu>>) GuiPowerJunction::new);
        event.register(LPMenuTypes.POWER_PROVIDER.get(),
            (MenuScreens.ScreenConstructor<PowerProviderMenu, AbstractContainerScreen<PowerProviderMenu>>) GuiPowerProvider::new);
        event.register(LPMenuTypes.SECURITY_STATION.get(),
            (MenuScreens.ScreenConstructor<SecurityStationMenu, AbstractContainerScreen<SecurityStationMenu>>) GuiSecurityStation::new);
        event.register(LPMenuTypes.AUTO_CRAFTING.get(),
            (MenuScreens.ScreenConstructor<AutoCraftingMenu, AbstractContainerScreen<AutoCraftingMenu>>) GuiLogisticsCraftingTable::new);
        event.register(LPMenuTypes.STATISTICS.get(),
            (MenuScreens.ScreenConstructor<StatisticsMenu, AbstractContainerScreen<StatisticsMenu>>) GuiStatistics::new);
        event.register(LPMenuTypes.FLUID_SINK.get(),
            (MenuScreens.ScreenConstructor<FluidSinkMenu, AbstractContainerScreen<FluidSinkMenu>>) GuiFluidBasic::new);
        event.register(LPMenuTypes.FLUID_TERMINUS.get(),
            (MenuScreens.ScreenConstructor<FluidTerminusMenu, AbstractContainerScreen<FluidTerminusMenu>>) GuiFluidTerminus::new);
        event.register(LPMenuTypes.SATELLITE.get(),
            (MenuScreens.ScreenConstructor<SatelliteMenu, AbstractContainerScreen<SatelliteMenu>>) GuiSatellitePipe::new);
        event.register(LPMenuTypes.FIREWALL.get(),
            (MenuScreens.ScreenConstructor<FirewallMenu, AbstractContainerScreen<FirewallMenu>>) GuiFirewall::new);
        event.register(LPMenuTypes.FREQ_CARD.get(),
            (MenuScreens.ScreenConstructor<FreqCardMenu, AbstractContainerScreen<FreqCardMenu>>) GuiFreqCardContent::new);
        event.register(LPMenuTypes.SIMPLE_FILTER.get(),
            (MenuScreens.ScreenConstructor<SimpleFilterMenu, AbstractContainerScreen<SimpleFilterMenu>>) GuiSimpleFilter::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER_MODULE.get(),
            (MenuScreens.ScreenConstructor<SimpleFilterMenu, AbstractContainerScreen<SimpleFilterMenu>>) GuiFluidSupplier::new);
        event.register(LPMenuTypes.CRAFTING_MODULE.get(),
            (MenuScreens.ScreenConstructor<CraftingModuleMenu, AbstractContainerScreen<CraftingModuleMenu>>) GuiCraftingPipe::new);
        event.register(LPMenuTypes.SNEAKY_DIRECTION.get(),
            (MenuScreens.ScreenConstructor<SneakyDirectionMenu, AbstractContainerScreen<SneakyDirectionMenu>>) GuiSneakyConfigurator::new);
        event.register(LPMenuTypes.ACTIVE_SUPPLIER.get(),
            (MenuScreens.ScreenConstructor<ActiveSupplierMenu, AbstractContainerScreen<ActiveSupplierMenu>>) GuiSupplierPipe::new);
        event.register(LPMenuTypes.PLAYER_SETTINGS.get(),
            (MenuScreens.ScreenConstructor<PlayerSettingsMenu, AbstractContainerScreen<PlayerSettingsMenu>>) GuiLogisticsSettings::new);
        event.register(LPMenuTypes.ITEM_AMOUNT_SIGN.get(),
            (MenuScreens.ScreenConstructor<ItemAmountSignMenu, AbstractContainerScreen<ItemAmountSignMenu>>) ItemAmountSignCreationGui::new);
        event.register(LPMenuTypes.REQUEST_TABLE.get(),
            (MenuScreens.ScreenConstructor<RequestTableMenu, AbstractContainerScreen<RequestTableMenu>>) GuiRequestTable::new);
        event.register(LPMenuTypes.ORDERER.get(),
            (MenuScreens.ScreenConstructor<OrdererMenu, AbstractContainerScreen<OrdererMenu>>) NormalGuiOrderer::new);
        event.register(LPMenuTypes.ORDERER_MK2.get(),
            (MenuScreens.ScreenConstructor<OrdererMk2Menu, AbstractContainerScreen<OrdererMk2Menu>>) NormalMk2GuiOrderer::new);
        event.register(LPMenuTypes.FLUID_ORDERER.get(),
            (MenuScreens.ScreenConstructor<FluidOrdererMenu, AbstractContainerScreen<FluidOrdererMenu>>) FluidGuiOrderer::new);
        event.register(LPMenuTypes.CHASSIS.get(),
            (MenuScreens.ScreenConstructor<ChassisMenu, AbstractContainerScreen<ChassisMenu>>) GuiChassisPipe::new);
        event.register(LPMenuTypes.ITEM_SINK.get(),
            (MenuScreens.ScreenConstructor<ItemSinkContainer, AbstractContainerScreen<ItemSinkContainer>>) ItemSinkGui::new);
        event.register(LPMenuTypes.PROVIDER.get(),
            (MenuScreens.ScreenConstructor<ProviderContainer, AbstractContainerScreen<ProviderContainer>>) ProviderGui::new);
        event.register(LPMenuTypes.ORE_DICT_ITEM_SINK.get(),
            (MenuScreens.ScreenConstructor<ModuleAnalysisMenu, AbstractContainerScreen<ModuleAnalysisMenu>>) GuiOreDictItemSink::new);
        event.register(LPMenuTypes.STRING_BASED_ITEM_SINK.get(),
            (MenuScreens.ScreenConstructor<ModuleAnalysisMenu, AbstractContainerScreen<ModuleAnalysisMenu>>) GuiStringBasedItemSink::new);
        event.register(LPMenuTypes.ADVANCED_EXTRACTOR.get(),
            (MenuScreens.ScreenConstructor<AdvancedExtractorMenu, AbstractContainerScreen<AdvancedExtractorMenu>>) GuiAdvancedExtractor::new);
        event.register(LPMenuTypes.HUD_SETTINGS.get(),
            (MenuScreens.ScreenConstructor<HudSettingsMenu, AbstractContainerScreen<HudSettingsMenu>>) GuiHUDSettings::new);
        event.register(LPMenuTypes.LOGIC_CONTROLLER.get(),
            (MenuScreens.ScreenConstructor<LogicControllerMenu, AbstractContainerScreen<LogicControllerMenu>>) LogicLayoutGui::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER.get(),
            (MenuScreens.ScreenConstructor<FluidSupplierMenu, AbstractContainerScreen<FluidSupplierMenu>>) GuiFluidSupplierPipe::new);
        event.register(LPMenuTypes.FLUID_SUPPLIER_MK2.get(),
            (MenuScreens.ScreenConstructor<FluidSupplierMk2Menu, AbstractContainerScreen<FluidSupplierMk2Menu>>) GuiFluidSupplierMk2Pipe::new);
    }
}
