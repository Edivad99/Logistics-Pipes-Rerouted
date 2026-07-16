package logisticspipes.world.item;

import java.util.Collection;
import java.util.function.Supplier;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import logisticspipes.LPConstants;
import logisticspipes.items.ItemBlankModule;
import logisticspipes.items.ItemDisk;
import logisticspipes.items.ItemHUDArmor;
import logisticspipes.items.ItemLogisticsChips;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemParts;
import logisticspipes.items.ItemPipeController;
import logisticspipes.items.ItemPipeManager;
import logisticspipes.items.ItemPipeSignCreator;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsBrokenItem;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.items.LogisticsSolidBlockItem;
import logisticspipes.items.RemoteOrderer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleEnchantmentSink;
import logisticspipes.modules.ModuleEnchantmentSinkMK2;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeFluidExtractor;
import logisticspipes.pipes.PipeFluidInsertion;
import logisticspipes.pipes.PipeFluidProvider;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.pipes.PipeLogisticsChassisMk1;
import logisticspipes.pipes.PipeLogisticsChassisMk2;
import logisticspipes.pipes.PipeLogisticsChassisMk3;
import logisticspipes.pipes.PipeLogisticsChassisMk4;
import logisticspipes.pipes.PipeLogisticsChassisMk5;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.tubes.HSTubeCurve;
import logisticspipes.pipes.tubes.HSTubeGain;
import logisticspipes.pipes.tubes.HSTubeLine;
import logisticspipes.pipes.tubes.HSTubeSCurve;
import logisticspipes.pipes.tubes.HSTubeSpeedup;
import logisticspipes.pipes.unrouted.PipeItemsBasicTransport;
import logisticspipes.pipes.upgrades.ActionSpeedUpgrade;
import logisticspipes.pipes.upgrades.AdvancedSatelliteUpgrade;
import logisticspipes.pipes.upgrades.CCRemoteControlUpgrade;
import logisticspipes.pipes.upgrades.CombinedSneakyUpgrade;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.pipes.upgrades.CraftingMonitoringUpgrade;
import logisticspipes.pipes.upgrades.FluidCraftingUpgrade;
import logisticspipes.pipes.upgrades.FuzzyUpgrade;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import logisticspipes.pipes.upgrades.LogicControllerUpgrade;
import logisticspipes.pipes.upgrades.OpaqueUpgrade;
import logisticspipes.pipes.upgrades.PatternUpgrade;
import logisticspipes.pipes.upgrades.PowerTransportationUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.pipes.upgrades.SpeedUpgrade;
import logisticspipes.pipes.upgrades.UpgradeModuleUpgrade;
import logisticspipes.pipes.upgrades.power.BCPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2EVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2HVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2LVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2MVPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.RFPowerSupplierUpgrade;
import logisticspipes.world.level.block.LPBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.module.AsyncExtractorModule;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;

public class LPItems {

  private static final DeferredRegister.Items deferredRegister =
      DeferredRegister.createItems(LPConstants.ID);

  public static void register(IEventBus modEventBus) {
    deferredRegister.register(modEventBus);
  }

  public static Collection<DeferredHolder<Item, ? extends Item>> entries() {
    return deferredRegister.getEntries();
  }

  public static BiMap<String, ResourceLocation> modules = HashBiMap.create();
  public static BiMap<String, ResourceLocation> upgrades = HashBiMap.create();

  // MISC
  public static final DeferredItem<LogisticsItemCard> ITEM_CARD =
      deferredRegister.register("item_card", LogisticsItemCard::new);
  public static final DeferredItem<RemoteOrderer> REMOTE_ORDERER =
      deferredRegister.register("remote_orderer", RemoteOrderer::new);
  public static final DeferredItem<ItemPipeSignCreator> SIGN_CREATOR =
      deferredRegister.register("sign_creator", ItemPipeSignCreator::new);
  public static final DeferredItem<ItemHUDArmor> HUD_GLASSES =
      deferredRegister.register("hud_glasses", ItemHUDArmor::new);
  public static final DeferredItem<ItemParts> PARTS =
      deferredRegister.register("parts", ItemParts::new);
  public static final DeferredItem<ItemBlankModule> MODULE_BLANK =
      deferredRegister.register("module_blank", ItemBlankModule::new);
  public static final DeferredItem<ItemDisk> DISK =
      deferredRegister.register("disk", ItemDisk::new);
  public static final DeferredItem<LogisticsFluidContainer> FLUID_CONTAINER =
      deferredRegister.register("fluid_container", LogisticsFluidContainer::new);
  public static final DeferredItem<LogisticsBrokenItem> BROKEN_ITEM =
      deferredRegister.register("broken_item", LogisticsBrokenItem::new);
  public static final DeferredItem<ItemGuideBook> GUIDE_BOOK =
      deferredRegister.register("guide_book", ItemGuideBook::new);
  public static final DeferredItem<ItemPipeController> PIPE_CONTROLLER =
      deferredRegister.register("pipe_controller", ItemPipeController::new);
  public static final DeferredItem<ItemPipeManager> PIPE_MANAGER =
      deferredRegister.register("pipe_manager", ItemPipeManager::new);
  public static final DeferredItem<ItemLogisticsProgrammer> LOGISTICS_PROGRAMMER =
      deferredRegister.register("logistics_programmer", ItemLogisticsProgrammer::new);
  public static final DeferredItem<ItemLogisticsChips> CHIP_BASIC =
      deferredRegister.register("chip_basic",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC));
  public static final DeferredItem<ItemLogisticsChips> CHIP_BASIC_RAW =
      deferredRegister.register("chip_basic_raw",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_BASIC_RAW));
  public static final DeferredItem<ItemLogisticsChips> CHIP_ADVANCED =
      deferredRegister.register("chip_advanced",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED));
  public static final DeferredItem<ItemLogisticsChips> CHIP_ADVANCED_RAW =
      deferredRegister.register("chip_advanced_raw",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_ADVANCED_RAW));
  public static final DeferredItem<ItemLogisticsChips> CHIP_FPGA =
      deferredRegister.register("chip_fpga",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA));
  public static final DeferredItem<ItemLogisticsChips> CHIP_FPGA_RAW =
      deferredRegister.register("chip_fpga_raw",
          () -> new ItemLogisticsChips(ItemLogisticsChips.ITEM_CHIP_FPGA_RAW));

  // BLOCKITEM
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_FRAME =
      deferredRegister.register("frame",
          () -> new LogisticsSolidBlockItem(LPBlocks.FRAME.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_POWER_JUNCTION =
      deferredRegister.register("power_junction",
          () -> new LogisticsSolidBlockItem(LPBlocks.POWER_JUNCTION.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_SECURITY_STATION =
      deferredRegister.register("security_station",
          () -> new LogisticsSolidBlockItem(LPBlocks.SECURITY_STATION.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_CRAFTER =
      deferredRegister.register("crafting_table",
          () -> new LogisticsSolidBlockItem(LPBlocks.CRAFTER.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_CRAFTER_FUZZY =
      deferredRegister.register("crafting_table_fuzzy",
          () -> new LogisticsSolidBlockItem(LPBlocks.CRAFTER_FUZZY.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_STATISTICS_TABLE =
      deferredRegister.register("statistics_table",
          () -> new LogisticsSolidBlockItem(LPBlocks.STATISTICS_TABLE.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_RF =
      deferredRegister.register("power_provider_rf",
          () -> new LogisticsSolidBlockItem(LPBlocks.POWER_PROVIDER_RF.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_EU =
      deferredRegister.register("power_provider_eu",
          () -> new LogisticsSolidBlockItem(LPBlocks.POWER_PROVIDER_EU.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_POWER_PROVIDER_MJ =
      deferredRegister.register("power_provider_mj",
          () -> new LogisticsSolidBlockItem(LPBlocks.POWER_PROVIDER_MJ.get()));
  public static final DeferredItem<LogisticsSolidBlockItem> ITEM_PROGRAM_COMPILER =
      deferredRegister.register("program_compiler",
          () -> new LogisticsSolidBlockItem(LPBlocks.PROGRAM_COMPILER.get()));

  // Items — item pipes (declared before modules/upgrades to avoid circular static-init:
  // registerModule/registerUpgrade call LPItems.modules/upgrades.put() which triggers LPItems
  // class loading; LPItems copies these RegistryObject refs at init time, so they must be
  // set in LPRegistries before the first registerModule/registerUpgrade call executes.)
  public static final DeferredItem<ItemLogisticsPipe> PIPE_BASIC =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "basic",
          PipeItemsBasicLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_REQUEST =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "request",
          PipeItemsRequestLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_REQUEST_MK2 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "request_mk2",
          PipeItemsRequestLogisticsMk2::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_PROVIDER =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "provider",
          PipeItemsProviderLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CRAFTING =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "crafting",
          PipeItemsCraftingLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_SATELLITE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "satellite",
          PipeItemsSatelliteLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_SUPPLIER =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "supplier",
          PipeItemsSupplierLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CHASSIS_MK1 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "chassis_mk1",
          PipeLogisticsChassisMk1::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CHASSIS_MK2 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "chassis_mk2",
          PipeLogisticsChassisMk2::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CHASSIS_MK3 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "chassis_mk3",
          PipeLogisticsChassisMk3::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CHASSIS_MK4 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "chassis_mk4",
          PipeLogisticsChassisMk4::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_CHASSIS_MK5 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "chassis_mk5",
          PipeLogisticsChassisMk5::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_REMOTE_ORDERER =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "remote_orderer",
          PipeItemsRemoteOrdererLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_INV_SYS_CONNECTOR =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "inventory_system_connector",
          PipeItemsInvSysConnector::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_SYSTEM_ENTRANCE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "system_entrance",
          PipeItemsSystemEntranceLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_SYSTEM_DESTINATION =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "system_destination",
          PipeItemsSystemDestinationLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FIREWALL =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "firewall", PipeItemsFirewall::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_REQUEST_TABLE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "request_table",
          PipeBlockRequestTable::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_UNROUTED =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "transport_basic",
          PipeItemsBasicTransport::new);
  // Fluid pipes
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_supplier",
          PipeItemsFluidSupplier::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_INSERTION =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_insertion",
          PipeFluidInsertion::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_PROVIDER =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_provider",
          PipeFluidProvider::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_REQUEST =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_request",
          PipeFluidRequestLogistics::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_EXTRACTOR =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_extractor",
          PipeFluidExtractor::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_SATELLITE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_satellite",
          PipeFluidSatellite::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_FLUID_SUPPLIER_MK2 =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "fluid_supplier_mk2",
          PipeFluidSupplierMk2::new);
  // High-speed tubes
  public static final DeferredItem<ItemLogisticsPipe> PIPE_HS_CURVE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "hs_curve", HSTubeCurve::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_HS_SPEEDUP =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "hs_speedup", HSTubeSpeedup::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_HS_S_CURVE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "hs_s_curve", HSTubeSCurve::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_HS_LINE =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "hs_line", HSTubeLine::new);
  public static final DeferredItem<ItemLogisticsPipe> PIPE_HS_GAIN =
      LogisticsBlockGenericPipe.registerPipe(deferredRegister, "hs_gain", HSTubeGain::new);


  public static final DeferredItem<ItemModule> MODULE_ITEM_SINK =
      registerModule(ModuleItemSink.getName(), ModuleItemSink::new);
  public static final DeferredItem<ItemModule> MODULE_PASSIVE_SUPPLIER =
      registerModule(ModulePassiveSupplier.getName(), ModulePassiveSupplier::new);
  public static final DeferredItem<ItemModule> MODULE_EXTRACTOR =
      registerModule(AsyncExtractorModule.getName(), AsyncExtractorModule::new);
  public static final DeferredItem<ItemModule> MODULE_POLYMORPHIC_SINK =
      registerModule(ModulePolymorphicItemSink.getName(), ModulePolymorphicItemSink::new);
  public static final DeferredItem<ItemModule> MODULE_QUICKSORT =
      registerModule(AsyncQuicksortModule.getName(), AsyncQuicksortModule::new);
  public static final DeferredItem<ItemModule> MODULE_TERMINUS =
      registerModule(ModuleTerminus.getName(), ModuleTerminus::new);
  public static final DeferredItem<ItemModule> MODULE_EXTRACTOR_ADVANCED =
      registerModule(AsyncAdvancedExtractor.getName(), AsyncAdvancedExtractor::new);
  public static final DeferredItem<ItemModule> MODULE_PROVIDER =
      registerModule(ModuleProvider.getName(), ModuleProvider::new);
  public static final DeferredItem<ItemModule> MODULE_MOD_SINK =
      registerModule(ModuleModBasedItemSink.getName(), ModuleModBasedItemSink::new);
  public static final DeferredItem<ItemModule> MODULE_OREDICT_SINK =
      registerModule(ModuleOreDictItemSink.getName(), ModuleOreDictItemSink::new);
  public static final DeferredItem<ItemModule> MODULE_ENCHANTMENT_SINK =
      registerModule(ModuleEnchantmentSink.getName(), ModuleEnchantmentSink::new);
  public static final DeferredItem<ItemModule> MODULE_ENCHANTMENT_SINK_MK2 =
      registerModule(ModuleEnchantmentSinkMK2.getName(), ModuleEnchantmentSinkMK2::new);
  public static final DeferredItem<ItemModule> MODULE_CRAFTER =
      registerModule(ModuleCrafter.getName(), ModuleCrafter::new);
  public static final DeferredItem<ItemModule> MODULE_ACTIVE_SUPPLIER =
      registerModule(ModuleActiveSupplier.getName(), ModuleActiveSupplier::new);
  public static final DeferredItem<ItemModule> MODULE_CREATIVETAB_SINK =
      registerModule(ModuleCreativeTabBasedItemSink.getName(), ModuleCreativeTabBasedItemSink::new);

  // ── Upgrades ──────────────────────────────────────────────────────────────

  public static final DeferredItem<ItemUpgrade> UPGRADE_SNEAKY_COMBINATION =
      registerUpgrade(CombinedSneakyUpgrade.getName(), CombinedSneakyUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_SNEAKY = registerUpgrade(
      SneakyUpgradeConfig.getName(), SneakyUpgradeConfig::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_SPEED =
      registerUpgrade(SpeedUpgrade.getName(), SpeedUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_DISCONNECTION =
      registerUpgrade(ConnectionUpgradeConfig.getName(), ConnectionUpgradeConfig::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_SATELLITE_ADVANCED =
      registerUpgrade(AdvancedSatelliteUpgrade.getName(), AdvancedSatelliteUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_FLUID_CRAFTING =
      registerUpgrade(FluidCraftingUpgrade.getName(), FluidCraftingUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_CRAFTING_BYPRODUCT =
      registerUpgrade(CraftingByproductUpgrade.getName(), CraftingByproductUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_PATTERN =
      registerUpgrade(PatternUpgrade.getName(), PatternUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_FUZZY =
      registerUpgrade(FuzzyUpgrade.getName(), FuzzyUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_TRANSPORTATION =
      registerUpgrade(PowerTransportationUpgrade.getName(), PowerTransportationUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_MJ =
      registerUpgrade(BCPowerSupplierUpgrade.getName(), BCPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_RF =
      registerUpgrade(RFPowerSupplierUpgrade.getName(), RFPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_EU_LV =
      registerUpgrade(IC2LVPowerSupplierUpgrade.getName(), IC2LVPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_EU_MV =
      registerUpgrade(IC2MVPowerSupplierUpgrade.getName(), IC2MVPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_EU_HV =
      registerUpgrade(IC2HVPowerSupplierUpgrade.getName(), IC2HVPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_POWER_EU_EV =
      registerUpgrade(IC2EVPowerSupplierUpgrade.getName(), IC2EVPowerSupplierUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_CC_REMOTE_CONTROL =
      registerUpgrade(CCRemoteControlUpgrade.getName(), CCRemoteControlUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_CRAFTING_MONITORING =
      registerUpgrade(CraftingMonitoringUpgrade.getName(), CraftingMonitoringUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_OPAQUE =
      registerUpgrade(OpaqueUpgrade.getName(), OpaqueUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_CRAFTING_CLEANUP =
      registerUpgrade(CraftingCleanupUpgrade.getName(), CraftingCleanupUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_LOGIC_CONTROLLER =
      registerUpgrade(LogicControllerUpgrade.getName(), LogicControllerUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_MODULE_UPGRADE =
      registerUpgrade(UpgradeModuleUpgrade.getName(), UpgradeModuleUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_ACTION_SPEED =
      registerUpgrade(ActionSpeedUpgrade.getName(), ActionSpeedUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_ITEM_EXTRACTION =
      registerUpgrade(ItemExtractionUpgrade.getName(), ItemExtractionUpgrade::new);
  public static final DeferredItem<ItemUpgrade> UPGRADE_ITEM_STACK_EXTRACTION =
      registerUpgrade(ItemStackExtractionUpgrade.getName(), ItemStackExtractionUpgrade::new);

  private static DeferredItem<ItemModule> registerModule(String name, Supplier<? extends LogisticsModule> ctor) {
    String regName = "module_" + name;
    LPItems.modules.put(name, LPConstants.rl(regName));
    return deferredRegister.register(regName, () -> ItemModule.of(ctor));
  }

  private static DeferredItem<ItemUpgrade> registerUpgrade(String name, Supplier<? extends IPipeUpgrade> ctor) {
    String regName = "upgrade_" + name;
    LPItems.upgrades.put(name, LPConstants.rl(regName));
    return deferredRegister.register(regName, () -> ItemUpgrade.of(ctor));
  }
}
