package logisticspipes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import logisticspipes.items.ItemBlankModule;
import logisticspipes.items.ItemDisk;
import logisticspipes.items.ItemHUDArmor;
import logisticspipes.items.ItemLogisticsChips;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemPipeController;
import logisticspipes.items.LogisticsBrokenItem;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.items.RemoteOrderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;

/**
 * Holds DeferredHolder references to all registered LP items.
 * Access via .get() — e.g. LPItems.pipeBasic.get()
 */
public class LPItems {

	// Logistics Pipes
	public static final DeferredItem<ItemLogisticsPipe> pipeUnrouted             = LPRegistries.PIPE_UNROUTED;
	public static final DeferredItem<ItemLogisticsPipe> pipeBasic                = LPRegistries.PIPE_BASIC;
	public static final DeferredItem<ItemLogisticsPipe> pipeRequest              = LPRegistries.PIPE_REQUEST;
	public static final DeferredItem<ItemLogisticsPipe> pipeRequestMk2           = LPRegistries.PIPE_REQUEST_MK2;
	public static final DeferredItem<ItemLogisticsPipe> pipeProvider             = LPRegistries.PIPE_PROVIDER;
	public static final DeferredItem<ItemLogisticsPipe> pipeCrafting             = LPRegistries.PIPE_CRAFTING;
	public static final DeferredItem<ItemLogisticsPipe> pipeSatellite            = LPRegistries.PIPE_SATELLITE;
	public static final DeferredItem<ItemLogisticsPipe> pipeSupplier             = LPRegistries.PIPE_SUPPLIER;
	public static final DeferredItem<ItemLogisticsPipe> pipeChassisMk1           = LPRegistries.PIPE_CHASSIS_MK1;
	public static final DeferredItem<ItemLogisticsPipe> pipeChassisMk2           = LPRegistries.PIPE_CHASSIS_MK2;
	public static final DeferredItem<ItemLogisticsPipe> pipeChassisMk3           = LPRegistries.PIPE_CHASSIS_MK3;
	public static final DeferredItem<ItemLogisticsPipe> pipeChassisMk4           = LPRegistries.PIPE_CHASSIS_MK4;
	public static final DeferredItem<ItemLogisticsPipe> pipeChassisMk5           = LPRegistries.PIPE_CHASSIS_MK5;
	public static final DeferredItem<ItemLogisticsPipe> pipeInvSystemConnector   = LPRegistries.PIPE_INV_SYS_CONNECTOR;
	public static final DeferredItem<ItemLogisticsPipe> pipeSystemEntrance       = LPRegistries.PIPE_SYSTEM_ENTRANCE;
	public static final DeferredItem<ItemLogisticsPipe> pipeSystemDestination    = LPRegistries.PIPE_SYSTEM_DESTINATION;
	public static final DeferredItem<ItemLogisticsPipe> pipeFirewall             = LPRegistries.PIPE_FIREWALL;
	public static final DeferredItem<ItemLogisticsPipe> pipeRemoteOrderer        = LPRegistries.PIPE_REMOTE_ORDERER;
	public static final DeferredItem<ItemLogisticsPipe> requestTable             = LPRegistries.PIPE_REQUEST_TABLE;

	// Logistics Fluid Pipes
	// NOTE: pipeFluidBasic and pipeFluidTerminus have no corresponding pipe class (removed upstream).
	public static final DeferredItem<? extends Item>    pipeFluidBasic           = null; // unregistered — PipeFluidBasic removed
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidRequest         = LPRegistries.PIPE_FLUID_REQUEST;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidProvider        = LPRegistries.PIPE_FLUID_PROVIDER;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidSatellite       = LPRegistries.PIPE_FLUID_SATELLITE;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidSupplier        = LPRegistries.PIPE_FLUID_SUPPLIER;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidSupplierMk2     = LPRegistries.PIPE_FLUID_SUPPLIER_MK2;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidInsertion       = LPRegistries.PIPE_FLUID_INSERTION;
	public static final DeferredItem<ItemLogisticsPipe> pipeFluidExtractor       = LPRegistries.PIPE_FLUID_EXTRACTOR;
	public static final DeferredItem<? extends Item>    pipeFluidTerminus        = null; // unregistered — PipeFluidTerminus removed

	// Modules / Upgrades
	public static final DeferredItem<ItemBlankModule>      blankModule          = LPRegistries.MODULE_BLANK;
	public static BiMap<String, net.minecraft.resources.ResourceLocation> modules  = HashBiMap.create();
	public static BiMap<String, net.minecraft.resources.ResourceLocation> upgrades = HashBiMap.create();

	// Miscellaneous Items
	public static final DeferredItem<ItemGuideBook>          itemGuideBook        = LPRegistries.GUIDE_BOOK;
	public static final DeferredItem<RemoteOrderer>          remoteOrderer        = LPRegistries.REMOTE_ORDERER;
	public static final DeferredItem<ItemDisk>               disk                 = LPRegistries.DISK;
	public static final DeferredItem<LogisticsItemCard>      itemCard             = LPRegistries.ITEM_CARD;
	public static final DeferredItem<ItemHUDArmor>           hudGlasses           = LPRegistries.HUD_GLASSES;
	public static final DeferredItem<LogisticsFluidContainer> fluidContainer      = LPRegistries.FLUID_CONTAINER;
	public static final DeferredItem<ItemPipeController>     pipeController       = LPRegistries.PIPE_CONTROLLER;
	public static final DeferredItem<ItemLogisticsProgrammer> logisticsProgrammer = LPRegistries.LOGISTICS_PROGRAMMER;
	public static final DeferredItem<ItemLogisticsChips>     chipBasic            = LPRegistries.CHIP_BASIC;
	public static final DeferredItem<ItemLogisticsChips>     chipBasicRaw         = LPRegistries.CHIP_BASIC_RAW;
	public static final DeferredItem<ItemLogisticsChips>     chipAdvanced         = LPRegistries.CHIP_ADVANCED;
	public static final DeferredItem<ItemLogisticsChips>     chipAdvancedRaw      = LPRegistries.CHIP_ADVANCED_RAW;
	public static final DeferredItem<ItemLogisticsChips>     chipFPGA             = LPRegistries.CHIP_FPGA;
	public static final DeferredItem<ItemLogisticsChips>     chipFPGARaw          = LPRegistries.CHIP_FPGA_RAW;
	public static final DeferredItem<LogisticsBrokenItem>    brokenItem           = LPRegistries.BROKEN_ITEM;

	// Typed helpers for Kotlin callers
	public static ItemGuideBook getItemGuideBook() { return itemGuideBook.get(); }
	public static LogisticsBrokenItem getBrokenItem() { return brokenItem.get(); }

}
