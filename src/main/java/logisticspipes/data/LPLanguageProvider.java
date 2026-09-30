package logisticspipes.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.common.data.LanguageProvider;

import logisticspipes.LPConstants;
import logisticspipes.Translations;
import logisticspipes.Translations.Chat;
import logisticspipes.Translations.Screen;
import logisticspipes.Translations.Tooltip;
import logisticspipes.inventory.ProviderMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.to_server.config.SetHudSettingMessage.HudSetting;
import logisticspipes.pipes.PipeFluidSupplierMk2.MinMode;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.request.RequestHandler.DisplayOptions;
import logisticspipes.utils.FuzzyFlag;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity.ProgramCategories;

public class LPLanguageProvider extends LanguageProvider {

    private final Set<String> keys = new HashSet<>();

    public LPLanguageProvider(PackOutput output) {
        super(output, LPConstants.ID, "en_us");
    }

    @Override
    public void add(String key, Component value) {
        super.add(key, value);
        keys.add(key);
    }

    @Override
    protected void addTranslations() {
        addItems();
        addBlocks();
        addTooltips();
        addChat();
        addScreens();
        checkEveryItemIsNamed();
    }

    private void addItems() {
        add(Translations.Tab.LOGISTICS_PIPES, LPConstants.NAME);

        addItem(LPItems.ITEM_CARD, "Logistics Item Card");
        addItem(LPItems.SECURITY_CARD, "Logistics Security Card");
        addItem(LPItems.REMOTE_ORDERER, "Remote Orderer");
        addItem(LPItems.SIGN_CREATOR, "Sign Creator");
        addItem(LPItems.HUD_GLASSES, "Logistics HUD Glasses");
        addItem(LPItems.PARTS, "Logistics Parts");
        addItem(LPItems.MODULE_BLANK, "Blank Module");
        addItem(LPItems.DISK, "Logistics Disk");
        addItem(LPItems.FLUID_CONTAINER, "Logistics Fluid Container");
        addItem(LPItems.BROKEN_ITEM, "Logistics Broken Item",
            "This is an useless item",
            "You get this by trying to break a protected pipe");
        addItem(LPItems.PIPE_CONTROLLER, "Pipe Controller");
        addItem(LPItems.PIPE_MANAGER, "Pipe Manager");
        addItem(LPItems.LOGISTICS_PROGRAMMER, "Logistics Programmer");
        addItem(LPItems.CHIP_BASIC, "Basic Chip");
        addItem(LPItems.CHIP_BASIC_RAW, "Raw Basic Chip");
        addItem(LPItems.CHIP_ADVANCED, "Advanced Chip");
        addItem(LPItems.CHIP_ADVANCED_RAW, "Raw Advanced Chip");
        addItem(LPItems.CHIP_FPGA, "FPGA");
        addItem(LPItems.CHIP_FPGA_RAW, "Raw FPGA");

        addItem(LPItems.PIPE_BASIC, "Basic Logistics Pipe",
            "Type: Passive push destination",
            "- Routes items around the network.",
            "- Puts specific types of items into chests.",
            "- Can be a default route.");
        addItem(LPItems.PIPE_REQUEST, "Request Logistics Pipe",
            "Type: Active requester",
            "- Lets you manually request items",
            "- Items can come from any Item Source pipe",
            "- Put a chest on the pipe to catch items coming out.");
        addItem(LPItems.PIPE_REQUEST_MK2, "Request Logistics Pipe Mk2",
            "Type: Active requester",
            "- Can have a Logistics Disk for macro requests",
            "- Lets you manually request items",
            "- Items can come from any Source pipe",
            "- Put a chest on the pipe to catch items coming out.");
        addItem(LPItems.PIPE_PROVIDER, "Provider Logistics Pipe",
            "Type: Item Source",
            "- Attaches to an inventory",
            "- Sends 16 items into the network on request",
            "- Ignores Suppliers on the same block.");
        addItem(LPItems.PIPE_CRAFTING, "Crafting Logistics Pipe",
            "Type: Active Requester + Source",
            "- Crafts one item at a time.",
            "- Attaches to an Automatic Crafting Table.",
            "- Automatically crafts the item inside when requested.",
            "- Can use a satellite pipe to send inputs elsewhere.");
        addItem(LPItems.PIPE_SATELLITE, "Satellite Logistics Pipe",
            "Type: Active request destination",
            "- A crafting pipe can send additional inputs here");
        addItem(LPItems.PIPE_SUPPLIER, "Supplier Logistics Pipe",
            "Type: Active item requester",
            "- Automatically fills a chest or machine",
            "- Can get items from any Item Source.",
            "- Ignores Providers on the same block.");
        addChassis(LPItems.PIPE_CHASSIS_MK1, 1, "1 module");
        addChassis(LPItems.PIPE_CHASSIS_MK2, 2, "2 modules");
        addChassis(LPItems.PIPE_CHASSIS_MK3, 3, "3 modules");
        addChassis(LPItems.PIPE_CHASSIS_MK4, 4, "4 modules");
        addChassis(LPItems.PIPE_CHASSIS_MK5, 5, "8 modules");
        addItem(LPItems.PIPE_REMOTE_ORDERER, "Remote Orderer Logistics Pipe",
            "Type: Active requester",
            "- Lets you manually request items *remotely*",
            "- Can pull from any Item Source",
            "- You must right click a Remote Orderer on this pipe.");
        addItem(LPItems.PIPE_INV_SYS_CONNECTOR, "Logistics Inventory System Connector",
            "Type: Bridge",
            "- Connects two logistics networks together",
            "over a shared inventory",
            "- Usually used with an ender chest but you",
            "can get clever with trains too...");
        addItem(LPItems.PIPE_SYSTEM_ENTRANCE, "Logistics System Entrance Pipe",
            "Type: Router",
            "- Sends un-routable items to a matched",
            "Destination Pipe.",
            "- Both Entrance and Destination pipe",
            "must share a coded frequency card.");
        addItem(LPItems.PIPE_SYSTEM_DESTINATION, "Logistics System Destination Pipe",
            "Type: Special Destination",
            "- Receives un-routeable items from a",
            "matched Entrance Pipe",
            "- Both Entrance and Destination pipe",
            "must share a coded frequency card.");
        addItem(LPItems.PIPE_FIREWALL, "Logistics Firewall Pipe",
            "Type: Request Filter",
            "- Allows connecting 'hostile' networks",
            "while allowing only certain items to pass");
        addItem(LPItems.PIPE_REQUEST_TABLE, "Logistics Request Table");
        addItem(LPItems.PIPE_UNROUTED, "Unrouted Transport Pipe",
            "Type: Simple Transport Pipe",
            "- Serves for the Transport of Items",
            "- Doesn't route Stuff",
            "- Can´t access Inventories");

        addItem(LPItems.PIPE_FLUID_BASIC, "Logistics Fluid Basic Pipe");
        addItem(LPItems.PIPE_FLUID_TERMINUS, "Logistics Fluid Terminus Pipe");
        addItem(LPItems.PIPE_FLUID_SUPPLIER, "Logistics Fluid Container Supplier",
            "Uses buckets or other fluid containers to fill the connected tank");
        addItem(LPItems.PIPE_FLUID_SUPPLIER_MK2, "Logistics Fluid Supplier",
            "Pulls Fluids from a Logistics Fluid Provider Pipe");
        addItem(LPItems.PIPE_FLUID_INSERTION, "Logistics Fluid Insertion Pipe");
        addItem(LPItems.PIPE_FLUID_PROVIDER, "Logistics Fluid Provider Pipe");
        addItem(LPItems.PIPE_FLUID_REQUEST, "Logistics Fluid Request Pipe");
        addItem(LPItems.PIPE_FLUID_EXTRACTOR, "Logistics Fluid Extractor Pipe");
        addItem(LPItems.PIPE_FLUID_SATELLITE, "Logistics Fluid Satellite Pipe");

        addItem(LPItems.PIPE_HS_CURVE, "Highspeed Curve Tube");
        addItem(LPItems.PIPE_HS_SPEEDUP, "Highspeed Speedup Tube");
        addItem(LPItems.PIPE_HS_S_CURVE, "Highspeed S-Curve Tube");
        addItem(LPItems.PIPE_HS_LINE, "Highspeed Line Tube");
        addItem(LPItems.PIPE_HS_GAIN, "Highspeed Gain Tube");

        addItem(LPItems.MODULE_ITEM_SINK, "ItemSink Module");
        addItem(LPItems.MODULE_PASSIVE_SUPPLIER, "Passive Supplier Module");
        addItem(LPItems.MODULE_EXTRACTOR, "Extractor Module");
        addItem(LPItems.MODULE_POLYMORPHIC_SINK, "Polymorphic ItemSink Module");
        addItem(LPItems.MODULE_QUICKSORT, "QuickSort Module");
        addItem(LPItems.MODULE_TERMINUS, "Terminus Module");
        addItem(LPItems.MODULE_EXTRACTOR_ADVANCED, "Advanced Extractor Module");
        addItem(LPItems.MODULE_PROVIDER, "Provider Module");
        addItem(LPItems.MODULE_MOD_SINK, "Mod Based ItemSink Module");
        addItem(LPItems.MODULE_OREDICT_SINK, "OreDict ItemSink Module");
        addItem(LPItems.MODULE_ENCHANTMENT_SINK, "Enchantment Sink Module");
        addItem(LPItems.MODULE_ENCHANTMENT_SINK_MK2, "Enchantment Sink Module MK2");
        addItem(LPItems.MODULE_CRAFTER, "Crafting Module");
        addItem(LPItems.MODULE_ACTIVE_SUPPLIER, "Active Supplier Module");
        addItem(LPItems.MODULE_CREATIVETAB_SINK, "Creative Tab Based ItemSink Module");

        addItem(LPItems.UPGRADE_SNEAKY_COMBINATION, "Sneaky Combination Upgrade");
        addItem(LPItems.UPGRADE_SNEAKY, "Sneaky Upgrade");
        addItem(LPItems.UPGRADE_SPEED, "Item Speed Upgrade");
        addItem(LPItems.UPGRADE_DISCONNECTION, "Disconnection Upgrade");
        addItem(LPItems.UPGRADE_SATELLITE_ADVANCED, "Advanced Satellite Upgrade");
        addItem(LPItems.UPGRADE_FLUID_CRAFTING, "Fluid Crafting Upgrade");
        addItem(LPItems.UPGRADE_CRAFTING_BYPRODUCT, "Crafting Byproduct Extraction Upgrade");
        addItem(LPItems.UPGRADE_PATTERN, "Placement Rules Upgrade");
        addItem(LPItems.UPGRADE_FUZZY, "Fuzzy Upgrade");
        addItem(LPItems.UPGRADE_POWER_TRANSPORTATION, "Power Transportation Upgrade");
        addItem(LPItems.UPGRADE_POWER_FE, "FE Power Supplier Upgrade");
        addItem(LPItems.UPGRADE_CC_REMOTE_CONTROL, "CC Remote Control Upgrade");
        addItem(LPItems.UPGRADE_CRAFTING_MONITORING, "Crafting Monitoring Upgrade");
        addItem(LPItems.UPGRADE_OPAQUE, "Opaque Upgrade");
        addItem(LPItems.UPGRADE_CRAFTING_CLEANUP, "Crafting Cleanup Upgrade");
        addItem(LPItems.UPGRADE_LOGIC_CONTROLLER, "Logic Controller Upgrade");
        addItem(LPItems.UPGRADE_MODULE_UPGRADE, "Module Upgrade");
        addItem(LPItems.UPGRADE_ACTION_SPEED, "Action Speed Upgrade");
        addItem(LPItems.UPGRADE_ITEM_EXTRACTION, "Item Extraction Upgrade");
        addItem(LPItems.UPGRADE_ITEM_STACK_EXTRACTION, "ItemStack Extraction Upgrade");
    }

    private void addBlocks() {
        addBlock(LPBlocks.FRAME, "Logistics Block Frame");
        addBlock(LPBlocks.POWER_JUNCTION, "Logistics Power Junction");
        addBlock(LPBlocks.SECURITY_STATION, "Logistics Security Station");
        addBlock(LPBlocks.CRAFTER, "Logistics Crafting Table");
        addBlock(LPBlocks.CRAFTER_FUZZY, "Logistics Fuzzy Crafting Table");
        addBlock(LPBlocks.STATISTICS_TABLE, "Logistics Statistics Table");
        addBlock(LPBlocks.POWER_PROVIDER_FE, "Logistics FE Power Provider");
        addBlock(LPBlocks.PROGRAM_COMPILER, "Logistics Program Compiler");
    }

    private void addTooltips() {
        add(Tooltip.HOLD, "Hold");
        add(Tooltip.SHIFT, "Shift");
        add(Tooltip.FOR_DETAILS, "for details");
        add(Tooltip.ITEM_CARD_INVALID, "This is no valid Card");
        add(Tooltip.PROGRAMMER_MODULE, "Module program loaded");
        add(Tooltip.PROGRAMMER_UPGRADE, "Upgrade program loaded");
        add(Tooltip.PROGRAMMER_PIPE, "Pipe program loaded");
        add(Tooltip.PROGRAMMER_EMPTY_1, "No valid program loaded");
        add(Tooltip.PROGRAMMER_EMPTY_2, "You need to flash a program");
        add(Tooltip.PROGRAMMER_EMPTY_3, "Before you can use this for crafting");
        add(Tooltip.UPGRADE_PIPES, "Can be applied to %s pipes");
        add(Tooltip.UPGRADE_MODULES, "Can be applied to %s modules");
        add(Tooltip.UPGRADE_AND_MODULES, "and %s modules");
        add(Tooltip.UPGRADE_TARGETS_AND, "%s and %s");
        add(Tooltip.upgradeTarget("all"), "All");
        add(Tooltip.upgradeTarget("basic"), "Basic");
        add(Tooltip.upgradeTarget("crafting"), "Crafting");
        add(Tooltip.upgradeTarget("provider"), "Provider");
        add(Tooltip.upgradeTarget("supplier"), "Supplier");
        add(Tooltip.upgradeTarget("chassis"), "Chassis");
        add(Tooltip.upgradeTarget("requestblock"), "Request Block");
        add(Tooltip.upgradeTarget("itemsink"), "Item Sink");
        add(Tooltip.upgradeTarget("extractor"), "Extractor");
        add(Tooltip.upgradeTarget("aextractor"), "Advanced Extractor");
        add(Tooltip.ROUTED_ITEM, "Item with stored LogisticsPipes routing information");
        add(Tooltip.ROUTED_ITEM_INFO, "Drop to remove routing information and get original items back");
        add(Tooltip.ROUTED_ITEM_STORED, "Stored item: %s");
    }

    private void addChat() {
        add(Chat.PERMISSION_DENIED, "Permission denied");
        add(Chat.SLOT_NOT_FOUND, "Couldn't find that slot internally. Sorry. Please try again.");
        add(Chat.CONNECTED_TO_PIPE, "Connected to pipe");
        add(Chat.NO_ENERGY, "No Energy");
        add(Chat.NO_PIPE_NEXT_TO_TABLE, "No logistics pipe next to this table.");
        for (HudSetting setting : HudSetting.values()) {
            String name = switch (setting) {
                case CHASSIS -> "Chassis";
                case CRAFTING -> "Crafting";
                case INV_SYS_CON -> "InvSysCon";
                case POWER_JUNCTION -> "Power Junction";
                case PROVIDER -> "Provider";
                case SATELLITE -> "Satellite";
            };
            add(Chat.hudSetting(setting, true), "Enabled " + name + ".");
            add(Chat.hudSetting(setting, false), "Disabled " + name + ".");
        }
    }

    private void addScreens() {
        add(Screen.YES, "Yes");
        add(Screen.NO, "No");
        add(Screen.INVENTORY, "Inventory");
        add(Screen.IMPORT, "Import");
        add(Screen.SELECT, "Select");
        add(Screen.OPEN, "Open");
        add(Screen.SAVE, "Save");
        add(Screen.INCLUDE, "Include");
        add(Screen.EXCLUDE, "Exclude");
        add(Screen.STORED_ENERGY, "Stored Energy");
        add(Screen.SELECT_TYPE, "Select Type");
        add(Screen.SELECT_ORE_DICT, "Select OreDict Type");
        add(Screen.ADD, "Add");
        add(Screen.REMOVE, "Remove");
        add(Screen.EDIT, "Edit");
        add(Screen.DELETE, "Delete");
        add(Screen.CLOSE, "Close");
        add(Screen.EXIT, "Exit");
        add(Screen.CANCEL, "Cancel");
        add(Screen.OK, "OK");
        add(Screen.DONE, "Done");
        add(Screen.REFRESH, "Refresh");
        add(Screen.REQUEST, "Request");
        add(Screen.SORT, "Sort");
        add(Screen.SET, "Set");
        add(Screen.INCLUDED, "Included");
        add(Screen.EXCLUDED, "Excluded");

        for (FuzzyFlag flag : FuzzyFlag.values()) {
            add(Screen.enumValue("fuzzy", flag), switch (flag) {
                case USE_ORE_DICT -> "OreDict";
                case IGNORE_DAMAGE -> "IgnDamage";
                case IGNORE_NBT -> "IgnNBT";
                case USE_ORE_CATEGORY -> "OrePrefix";
            });
        }
        for (ProviderMode mode : ProviderMode.values()) {
            add(Screen.enumValue("provider_mode", mode), switch (mode) {
                case DEFAULT -> "Normal";
                case LEAVE_FIRST -> "Leave 1st stack";
                case LEAVE_LAST -> "Leave last stack";
                case LEAVE_FIRST_AND_LAST -> "Leave first & last stack";
                case LEAVE_ONE_PER_STACK -> "Leave 1 item per stack";
                case LEAVE_ONE_PER_TYPE -> "Leave 1 item per type";
            });
        }
        for (MinMode mode : MinMode.values()) {
            add(Screen.enumValue("fluid_supplier_mk2.min_amount", mode), switch (mode) {
                case NONE -> "None";
                case ONEBUCKET -> "1 Bucket";
                case TWOBUCKET -> "2 Buckets";
                case FIVEBUCKET -> "5 Buckets";
            });
        }
        for (SatelliteNamingResult result : SatelliteNamingResult.values()) {
            add(Screen.enumValue("satellite_pipe.naming_result", result), switch (result) {
                case SUCCESS -> "Success";
                case DUPLICATE_NAME -> "Name already used.";
                case BLANK_NAME -> "Name can't be blank.";
            });
        }

        add(Screen.ITEM_SINK_DEFAULT_ROUTE, "Default route");

        add(Screen.ADVANCED_EXTRACTOR_SNEAKY, "Sneaky");
        add(Screen.ADVANCED_EXTRACTOR_SIDE, "Side");
        add(Screen.ADVANCED_EXTRACTOR_INVENTORY, "Inv");

        add(Screen.ORDERER_CONTENT, "Content");
        add(Screen.ORDERER_HIDE, "Hide");
        add(Screen.ORDERER_SHOW, "Show");
        add(Screen.ORDERER_DISK, "Disk");
        add(Screen.ORDERER_MORE, "more");
        add(Screen.ORDERER_LOG, "Log");
        add(Screen.ORDERER_DISK_ADD_EDIT, "Add/Edit");
        add(Screen.ORDERER_SAVE_AS_IMAGE, "Save as Image");
        for (DisplayOptions option : DisplayOptions.values()) {
            add(Screen.enumValue("orderer.display", option), switch (option) {
                case Both -> "Both";
                case CraftOnly -> "Craft";
                case SupplyOnly -> "Supply";
            });
        }

        add(Screen.RECIPE_IMPORT_MOST_LIKELY, "Most likely");

        add(Screen.CRAFTING_INPUTS, "Inputs");
        add(Screen.CRAFTING_OUTPUT, "Output");
        add(Screen.CRAFTING_SATELLITE, "Satellite");
        add(Screen.CRAFTING_OFF, "Off");
        add(Screen.CRAFTING_PRIORITY, "Priority");
        add(Screen.CRAFTING_SELECT_SHORT, "Sel");

        add(Screen.FIREWALL_TITLE, "Firewall");
        add(Screen.FIREWALL_FILTER, "Filter");
        add(Screen.FIREWALL_FILTERED_ITEMS_ARE, "Filtered items are");
        add(Screen.FIREWALL_BLOCKED, "Blocked");
        add(Screen.FIREWALL_ALLOWED, "Allowed");
        add(Screen.FIREWALL_PROVIDING, "Providing");
        add(Screen.FIREWALL_CRAFTING, "Crafting");
        add(Screen.FIREWALL_SORTING, "Sorting");
        add(Screen.FIREWALL_POWER_FLOW, "Powerflow");

        add(Screen.FLUID_BASIC_EMPTY, "Empty");

        add(Screen.FLUID_SUPPLIER_TARGET, "Fluids to keep stocked");
        add(Screen.FLUID_SUPPLIER_PARTIAL_REQUESTS, "Partial requests");
        add(Screen.FLUID_SUPPLIER_MK2_TARGET, "Fluid to keep stocked");
        add(Screen.FLUID_SUPPLIER_MK2_FLUID, "Fluid");
        add(Screen.FLUID_SUPPLIER_MK2_PARTIAL, "Partial");
        add(Screen.FLUID_SUPPLIER_MK2_MIN_MODE, "Min. Mode");

        add(Screen.INV_SYS_CON_TITLE, "Inventory System Connector");
        add(Screen.INV_SYS_CON_CONNECTION_INFORMATION, "Connection Information");
        add(Screen.INV_SYS_CON_CHANNEL, "Channel");
        add(Screen.INV_SYS_CON_RESISTANCE, "Resistance");
        add(Screen.INV_SYS_CON_WAITING_FOR, "Waiting for");
        add(Screen.INV_SYS_CON_CHANGE, "Change");

        add(Screen.PROVIDER_EXCESS_INVENTORY, "Excess Inventory:");
        add(Screen.PROVIDER_SWITCH, "Switch");

        add(Screen.SUPPLIER_TARGET, "Items to keep stocked");
        add(Screen.SUPPLIER_TARGET_PATTERN, "Items to keep in the specified slots");
        add(Screen.SUPPLIER_REQUEST_MODE, "Request Mode");
        add(Screen.SUPPLIER_LIMITED, "Limited");
        add(Screen.SUPPLIER_UNLIMITED, "Unlimited");
        for (SupplyMode mode : SupplyMode.values()) {
            add(Screen.enumValue("supplier_pipe.mode", mode), mode.name());
        }

        add(Screen.PIPE_CONTROLLER_UPGRADES, "Upgrades");
        add(Screen.PIPE_CONTROLLER_SNEAKY_UPGRADES, "Sneaky Upgrades");
        add(Screen.PIPE_CONTROLLER_SECURITY, "Security");
        add(Screen.PIPE_CONTROLLER_SESSION, "Session");
        add(Screen.PIPE_CONTROLLER_LIFETIME, "Lifetime");
        add(Screen.PIPE_CONTROLLER_SENT, "Sent");
        add(Screen.PIPE_CONTROLLER_RECEIVED, "Received");
        add(Screen.PIPE_CONTROLLER_RELAYED, "Relayed");
        add(Screen.PIPE_CONTROLLER_ROUTING_TABLE_SIZE, "RoutingTableSize");
        add(Screen.PIPE_CONTROLLER_DISCONNECTION_TITLE, "Disconnection Configuration");
        add(Screen.PIPE_CONTROLLER_SNEAKY_TITLE, "Sneaky Configuration");
        add(Screen.PIPE_CONTROLLER_EDIT_LOGIC, "Edit Logic Controller");

        add(Screen.SATELLITE_NAME, "Satellite Name");

        add(Screen.SECURITY_STATION_TITLE, "Security Station");
        add(Screen.SECURITY_STATION_EDIT_TABLE, "Edit Table");
        add(Screen.SECURITY_STATION_AUTHORIZE, "Authorize");
        add(Screen.SECURITY_STATION_DEAUTHORIZE, "Deauthorize");
        add(Screen.SECURITY_STATION_CHANNEL_MANAGER, "Channel Manager");
        add(Screen.SECURITY_STATION_ALLOW_CC_ACCESS, "Allow ComputerCraft Access");
        add(Screen.SECURITY_STATION_EXCLUDED_IDS, "Excluded ComputerCraft IDs");
        add(Screen.SECURITY_STATION_PIPE_REMOVE, "Allow automated Pipe remove");
        add(Screen.SECURITY_STATION_PLAYER, "Player");
        add(Screen.SECURITY_STATION_SECURITY_CARDS, "Security Cards");
        add(Screen.SECURITY_STATION_PLAYER_CONFIGURE_SETTINGS, "Configure Settings");
        add(Screen.SECURITY_STATION_PLAYER_ACTIVE_REQUESTING, "Active Requesting");
        add(Screen.SECURITY_STATION_PLAYER_UPGRADE_PIPES, "Upgrade Pipes");
        add(Screen.SECURITY_STATION_PLAYER_CHECK_NETWORK, "Check Network");
        add(Screen.SECURITY_STATION_PLAYER_REMOVE_PIPES, "Remove Pipes");
        add(Screen.SECURITY_STATION_PLAYER_ACCESS_ROUTING_CHANNELS, "Access Routing Channels");

        add(Screen.STATISTICS_TRACK_AMOUNT, "Track amount");
        add(Screen.STATISTICS_CRAFTING, "View active crafting tasks");
        add(Screen.STATISTICS_GET_TASKS, "Acquire tasks");
        add(Screen.STATISTICS_ALREADY_TRACKED, "This item type is already tracked");

        add(Screen.PROGRAM_COMPILER_PROCESSING, "Processing...");
        add(Screen.PROGRAM_COMPILER_NO_POWER, "No power");
        add(Screen.PROGRAM_COMPILER_CONNECT, "Connect to LP network");
        add(Screen.PROGRAM_COMPILER_UNLOCK, "Unlock");
        add(Screen.PROGRAM_COMPILER_COMPILE, "Compile");
        add(Screen.PROGRAM_COMPILER_FLASH, "Flash");
        addCompilerCategory(ProgramCategories.BASIC, "Basic");
        addCompilerCategory(ProgramCategories.TIER_2, "Tier 2");
        addCompilerCategory(ProgramCategories.FLUID, "Fluid");
        addCompilerCategory(ProgramCategories.TIER_3, "Tier 3");
        addCompilerCategory(ProgramCategories.CHASSIS, "Chassis");
        addCompilerCategory(ProgramCategories.CHASSIS_2, "Chassis 2");
        addCompilerCategory(ProgramCategories.CHASSIS_3, "Chassis 3");
        addCompilerCategory(ProgramCategories.MODDED, "Modded");

        add(Screen.SETTINGS_PIPE_RENDER_DISTANCE, "Max. Distance for Renderer");
        add(Screen.SETTINGS_PIPE_CONTENT_RENDER_DISTANCE, "Max. Distance for Pipe Content");

        add(Screen.CHANNEL_SELECT_TITLE, "Channel Selection");
        add(Screen.CHANNEL_ADD_TITLE, "Add Channel");
        add(Screen.CHANNEL_EDIT_TITLE, "Edit Channel");
        add(Screen.CHANNEL_MANAGE_TITLE, "Channel Manager");
        add(Screen.CHANNEL_NAME, "Name");
        add(Screen.CHANNEL_OWNER, "Owner");
        add(Screen.CHANNEL_ACCESS, "Access rights");
        add(Screen.CHANNEL_ACCESS_PUBLIC, "Public");
        add(Screen.CHANNEL_ACCESS_SECURITY, "Security Station");
        add(Screen.CHANNEL_ACCESS_PRIVATE, "Private");
        add(Screen.CHANNEL_DELETE_CONFIRM, "Do you really want to delete this channel?");

        add(Screen.SATELLITE_SELECT_TITLE, "Select Satellite Pipe");
        add(Screen.SATELLITE_SELECT_UNSET, "Unset");
    }

    private void addItem(Supplier<? extends Item> item, String name, String... tips) {
        add(item.get(), name);
        String descriptionId = item.get().getDescriptionId();
        for (int i = 0; i < tips.length; i++) {
            add(Tooltip.itemTip(descriptionId, i + 1), tips[i]);
        }
    }

    private void addChassis(Supplier<? extends Item> item, int tier, String capacity) {
        addItem(item, "Logistics Chassis Mk" + tier, "Type: Mixed", "- Can hold " + capacity + " by default.");
    }

    private void addCompilerCategory(Identifier category, String name) {
        add(Screen.compilerCategory(category), name);
    }

    /** A new item without a name would otherwise only show up as a raw key in game. */
    private void checkEveryItemIsNamed() {
        List<String> missing = LPItems.entries().stream()
            .map(holder -> holder.get().getDescriptionId())
            .filter(key -> !keys.contains(key))
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Items without an English name: " + missing);
        }
    }
}
