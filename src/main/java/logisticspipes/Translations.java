package logisticspipes;

import java.util.Locale;

import net.minecraft.resources.Identifier;

/**
 * Every translation key the mod looks up, so a key is spelt once. The English text for each lives
 * in {@code LPLanguageProvider}.
 */
public final class Translations {

    private Translations() {
    }

    public static final class Tab {

        public static final String LOGISTICS_PIPES = "itemGroup." + LPConstants.ID;

        private Tab() {
        }
    }

    public static final class Tooltip {

        public static final String HOLD = makeKey("tooltip", "hold");
        public static final String SHIFT = makeKey("tooltip", "shift");
        public static final String FOR_DETAILS = makeKey("tooltip", "for_details");
        public static final String ITEM_CARD_INVALID = makeKey("tooltip", "item_card.invalid");
        public static final String PROGRAMMER_MODULE = makeKey("tooltip", "logistics_programmer.module");
        public static final String PROGRAMMER_UPGRADE = makeKey("tooltip", "logistics_programmer.upgrade");
        public static final String PROGRAMMER_PIPE = makeKey("tooltip", "logistics_programmer.pipe");
        public static final String PROGRAMMER_EMPTY_1 = makeKey("tooltip", "logistics_programmer.empty.1");
        public static final String PROGRAMMER_EMPTY_2 = makeKey("tooltip", "logistics_programmer.empty.2");
        public static final String PROGRAMMER_EMPTY_3 = makeKey("tooltip", "logistics_programmer.empty.3");
        public static final String UPGRADE_PIPES = makeKey("tooltip", "upgrade.pipes");
        public static final String UPGRADE_MODULES = makeKey("tooltip", "upgrade.modules");
        public static final String UPGRADE_AND_MODULES = makeKey("tooltip", "upgrade.and_modules");
        public static final String UPGRADE_TARGETS_AND = makeKey("tooltip", "upgrade.targets_and");
        public static final String ROUTED_ITEM = makeKey("tooltip", "routed_item");
        public static final String ROUTED_ITEM_INFO = makeKey("tooltip", "routed_item.info");
        public static final String ROUTED_ITEM_STORED = makeKey("tooltip", "routed_item.stored");

        private Tooltip() {
        }

        /** The name of what an upgrade applies to, as {@code IPipeUpgrade} lists it. */
        public static String upgradeTarget(String target) {
            return makeKey("tooltip", "upgrade.target." + target);
        }

        /** Line {@code line} (from 1) of the extended tooltip of the item with this description id. */
        public static String itemTip(String descriptionId, int line) {
            return descriptionId + ".tip" + line;
        }
    }

    public static final class Chat {

        public static final String PERMISSION_DENIED = makeKey("chat", "permission_denied");
        public static final String SLOT_NOT_FOUND = makeKey("chat", "slot_not_found");
        public static final String CONNECTED_TO_PIPE = makeKey("chat", "connected_to_pipe");
        public static final String NO_ENERGY = makeKey("chat", "no_energy");
        public static final String NO_PIPE_NEXT_TO_TABLE = makeKey("chat", "no_pipe_next_to_table");

        private Chat() {
        }

        /** The message shown when one HUD glasses switch is flipped. */
        public static String hudSetting(Enum<?> setting, boolean enabled) {
            return makeKey("chat", "hud." + setting.name().toLowerCase(Locale.ROOT)
                + (enabled ? ".enabled" : ".disabled"));
        }
    }

    public static final class Screen {

        public static final String YES = makeKey("screen", "yes");
        public static final String NO = makeKey("screen", "no");
        public static final String INVENTORY = makeKey("screen", "inventory");
        public static final String IMPORT = makeKey("screen", "import");
        public static final String SELECT = makeKey("screen", "select");
        public static final String OPEN = makeKey("screen", "open");
        public static final String SAVE = makeKey("screen", "save");
        public static final String INCLUDE = makeKey("screen", "include");
        public static final String EXCLUDE = makeKey("screen", "exclude");
        public static final String STORED_ENERGY = makeKey("screen", "stored_energy");
        public static final String SELECT_TYPE = makeKey("screen", "select_type");
        public static final String SELECT_ORE_DICT = makeKey("screen", "select_ore_dict");
        public static final String ADD = makeKey("screen", "add");
        public static final String REMOVE = makeKey("screen", "remove");
        public static final String EDIT = makeKey("screen", "edit");
        public static final String DELETE = makeKey("screen", "delete");
        public static final String CLOSE = makeKey("screen", "close");
        public static final String EXIT = makeKey("screen", "exit");
        public static final String CANCEL = makeKey("screen", "cancel");
        public static final String OK = makeKey("screen", "ok");
        public static final String DONE = makeKey("screen", "done");
        public static final String REFRESH = makeKey("screen", "refresh");
        public static final String REQUEST = makeKey("screen", "request");
        public static final String SORT = makeKey("screen", "sort");
        public static final String SET = makeKey("screen", "set");
        public static final String INCLUDED = makeKey("screen", "included");
        public static final String EXCLUDED = makeKey("screen", "excluded");

        public static final String ITEM_SINK_DEFAULT_ROUTE = makeKey("screen", "item_sink.default_route");

        public static final String ADVANCED_EXTRACTOR_SNEAKY = makeKey("screen", "advanced_extractor.sneaky");
        public static final String ADVANCED_EXTRACTOR_SIDE = makeKey("screen", "advanced_extractor.side");
        public static final String ADVANCED_EXTRACTOR_INVENTORY = makeKey("screen", "advanced_extractor.inventory");

        public static final String ORDERER_CONTENT = makeKey("screen", "orderer.content");
        public static final String ORDERER_HIDE = makeKey("screen", "orderer.hide");
        public static final String ORDERER_SHOW = makeKey("screen", "orderer.show");
        public static final String ORDERER_DISK = makeKey("screen", "orderer.disk");
        public static final String ORDERER_MORE = makeKey("screen", "orderer.more");
        public static final String ORDERER_LOG = makeKey("screen", "orderer.log");
        public static final String ORDERER_DISK_ADD_EDIT = makeKey("screen", "orderer.disk.add_edit");
        public static final String ORDERER_SAVE_AS_IMAGE = makeKey("screen", "orderer.save_as_image");

        public static final String RECIPE_IMPORT_MOST_LIKELY = makeKey("screen", "recipe_import.most_likely");

        public static final String CRAFTING_INPUTS = makeKey("screen", "crafting_pipe.inputs");
        public static final String CRAFTING_OUTPUT = makeKey("screen", "crafting_pipe.output");
        public static final String CRAFTING_SATELLITE = makeKey("screen", "crafting_pipe.satellite");
        public static final String CRAFTING_OFF = makeKey("screen", "crafting_pipe.off");
        public static final String CRAFTING_PRIORITY = makeKey("screen", "crafting_pipe.priority");
        public static final String CRAFTING_SELECT_SHORT = makeKey("screen", "crafting_pipe.select_short");

        public static final String FIREWALL_TITLE = makeKey("screen", "firewall.title");
        public static final String FIREWALL_FILTER = makeKey("screen", "firewall.filter");
        public static final String FIREWALL_FILTERED_ITEMS_ARE = makeKey("screen", "firewall.filtered_items_are");
        public static final String FIREWALL_BLOCKED = makeKey("screen", "firewall.blocked");
        public static final String FIREWALL_ALLOWED = makeKey("screen", "firewall.allowed");
        public static final String FIREWALL_PROVIDING = makeKey("screen", "firewall.providing");
        public static final String FIREWALL_CRAFTING = makeKey("screen", "firewall.crafting");
        public static final String FIREWALL_SORTING = makeKey("screen", "firewall.sorting");
        public static final String FIREWALL_POWER_FLOW = makeKey("screen", "firewall.power_flow");

        public static final String FLUID_BASIC_EMPTY = makeKey("screen", "fluid_basic.empty");

        public static final String FLUID_SUPPLIER_TARGET = makeKey("screen", "fluid_supplier.target");
        public static final String FLUID_SUPPLIER_PARTIAL_REQUESTS =
            makeKey("screen", "fluid_supplier.partial_requests");
        public static final String FLUID_SUPPLIER_MK2_TARGET = makeKey("screen", "fluid_supplier_mk2.target");
        public static final String FLUID_SUPPLIER_MK2_FLUID = makeKey("screen", "fluid_supplier_mk2.fluid");
        public static final String FLUID_SUPPLIER_MK2_PARTIAL = makeKey("screen", "fluid_supplier_mk2.partial");
        public static final String FLUID_SUPPLIER_MK2_MIN_MODE = makeKey("screen", "fluid_supplier_mk2.min_mode");

        public static final String INV_SYS_CON_TITLE = makeKey("screen", "inv_sys_con.title");
        public static final String INV_SYS_CON_CONNECTION_INFORMATION =
            makeKey("screen", "inv_sys_con.connection_information");
        public static final String INV_SYS_CON_CHANNEL = makeKey("screen", "inv_sys_con.channel");
        public static final String INV_SYS_CON_RESISTANCE = makeKey("screen", "inv_sys_con.resistance");
        public static final String INV_SYS_CON_WAITING_FOR = makeKey("screen", "inv_sys_con.waiting_for");
        public static final String INV_SYS_CON_CHANGE = makeKey("screen", "inv_sys_con.change");

        public static final String PROVIDER_EXCESS_INVENTORY = makeKey("screen", "provider_pipe.excess_inventory");
        public static final String PROVIDER_SWITCH = makeKey("screen", "provider_pipe.switch");

        public static final String SUPPLIER_TARGET = makeKey("screen", "supplier_pipe.target");
        public static final String SUPPLIER_TARGET_PATTERN = makeKey("screen", "supplier_pipe.target_pattern");
        public static final String SUPPLIER_REQUEST_MODE = makeKey("screen", "supplier_pipe.request_mode");
        public static final String SUPPLIER_LIMITED = makeKey("screen", "supplier_pipe.limited");
        public static final String SUPPLIER_UNLIMITED = makeKey("screen", "supplier_pipe.unlimited");

        public static final String PIPE_CONTROLLER_UPGRADES = makeKey("screen", "pipe_controller.upgrades");
        public static final String PIPE_CONTROLLER_SNEAKY_UPGRADES =
            makeKey("screen", "pipe_controller.sneaky_upgrades");
        public static final String PIPE_CONTROLLER_SECURITY = makeKey("screen", "pipe_controller.security");
        public static final String PIPE_CONTROLLER_SESSION = makeKey("screen", "pipe_controller.session");
        public static final String PIPE_CONTROLLER_LIFETIME = makeKey("screen", "pipe_controller.lifetime");
        public static final String PIPE_CONTROLLER_SENT = makeKey("screen", "pipe_controller.sent");
        public static final String PIPE_CONTROLLER_RECEIVED = makeKey("screen", "pipe_controller.received");
        public static final String PIPE_CONTROLLER_RELAYED = makeKey("screen", "pipe_controller.relayed");
        public static final String PIPE_CONTROLLER_ROUTING_TABLE_SIZE =
            makeKey("screen", "pipe_controller.routing_table_size");
        public static final String PIPE_CONTROLLER_DISCONNECTION_TITLE =
            makeKey("screen", "pipe_controller.disconnection_title");
        public static final String PIPE_CONTROLLER_SNEAKY_TITLE = makeKey("screen", "pipe_controller.sneaky_title");
        public static final String PIPE_CONTROLLER_EDIT_LOGIC =
            makeKey("screen", "pipe_controller.edit_logic");

        public static final String SATELLITE_NAME = makeKey("screen", "satellite_pipe.name");

        public static final String SECURITY_STATION_TITLE = makeKey("screen", "security_station.title");
        public static final String SECURITY_STATION_EDIT_TABLE = makeKey("screen", "security_station.edit_table");
        public static final String SECURITY_STATION_AUTHORIZE = makeKey("screen", "security_station.authorize");
        public static final String SECURITY_STATION_DEAUTHORIZE = makeKey("screen", "security_station.deauthorize");
        public static final String SECURITY_STATION_CHANNEL_MANAGER =
            makeKey("screen", "security_station.channel_manager");
        public static final String SECURITY_STATION_ALLOW_CC_ACCESS =
            makeKey("screen", "security_station.allow_cc_access");
        public static final String SECURITY_STATION_EXCLUDED_IDS = makeKey("screen", "security_station.excluded_ids");
        public static final String SECURITY_STATION_PIPE_REMOVE = makeKey("screen", "security_station.pipe_remove");
        public static final String SECURITY_STATION_PLAYER = makeKey("screen", "security_station.player");
        public static final String SECURITY_STATION_SECURITY_CARDS =
            makeKey("screen", "security_station.security_cards");
        public static final String SECURITY_STATION_PLAYER_CONFIGURE_SETTINGS =
            makeKey("screen", "security_station.player.configure_settings");
        public static final String SECURITY_STATION_PLAYER_ACTIVE_REQUESTING =
            makeKey("screen", "security_station.player.active_requesting");
        public static final String SECURITY_STATION_PLAYER_UPGRADE_PIPES =
            makeKey("screen", "security_station.player.upgrade_pipes");
        public static final String SECURITY_STATION_PLAYER_CHECK_NETWORK =
            makeKey("screen", "security_station.player.check_network");
        public static final String SECURITY_STATION_PLAYER_REMOVE_PIPES =
            makeKey("screen", "security_station.player.remove_pipes");
        public static final String SECURITY_STATION_PLAYER_ACCESS_ROUTING_CHANNELS =
            makeKey("screen", "security_station.player.access_routing_channels");

        public static final String STATISTICS_TRACK_AMOUNT = makeKey("screen", "statistics.track_amount");
        public static final String STATISTICS_CRAFTING = makeKey("screen", "statistics.crafting");
        public static final String STATISTICS_GET_TASKS = makeKey("screen", "statistics.get_tasks");
        public static final String STATISTICS_ALREADY_TRACKED = makeKey("screen", "statistics.already_tracked");

        public static final String PROGRAM_COMPILER_PROCESSING = makeKey("screen", "program_compiler.processing");
        public static final String PROGRAM_COMPILER_NO_POWER = makeKey("screen", "program_compiler.no_power");
        public static final String PROGRAM_COMPILER_CONNECT = makeKey("screen", "program_compiler.connect");
        public static final String PROGRAM_COMPILER_UNLOCK = makeKey("screen", "program_compiler.unlock");
        public static final String PROGRAM_COMPILER_COMPILE = makeKey("screen", "program_compiler.compile");
        public static final String PROGRAM_COMPILER_FLASH = makeKey("screen", "program_compiler.flash");

        public static final String SETTINGS_PIPE_RENDER_DISTANCE = makeKey("screen", "settings.pipe_render_distance");
        public static final String SETTINGS_PIPE_CONTENT_RENDER_DISTANCE =
            makeKey("screen", "settings.pipe_content_render_distance");

        public static final String CHANNEL_SELECT_TITLE = makeKey("screen", "channel.select.title");
        public static final String CHANNEL_ADD_TITLE = makeKey("screen", "channel.add.title");
        public static final String CHANNEL_EDIT_TITLE = makeKey("screen", "channel.edit.title");
        public static final String CHANNEL_MANAGE_TITLE = makeKey("screen", "channel.manage.title");
        public static final String CHANNEL_NAME = makeKey("screen", "channel.name");
        public static final String CHANNEL_OWNER = makeKey("screen", "channel.owner");
        public static final String CHANNEL_ACCESS = makeKey("screen", "channel.access");
        public static final String CHANNEL_ACCESS_PUBLIC = makeKey("screen", "channel.access.public");
        public static final String CHANNEL_ACCESS_SECURITY = makeKey("screen", "channel.access.security");
        public static final String CHANNEL_ACCESS_PRIVATE = makeKey("screen", "channel.access.private");
        public static final String CHANNEL_DELETE_CONFIRM = makeKey("screen", "channel.delete_confirm");

        public static final String SATELLITE_SELECT_TITLE = makeKey("screen", "satellite_select.title");
        public static final String SATELLITE_SELECT_UNSET = makeKey("screen", "satellite_select.unset");

        private Screen() {
        }

        /** Keyed by the constant's name, so {@code SatelliteNamingResult} and friends need no key field. */
        public static String enumValue(String type, Enum<?> value) {
            return makeKey("screen", type + "." + value.name().toLowerCase(Locale.ROOT));
        }

        /** Program compiler categories are identifiers, and addons may add their own. */
        public static String compilerCategory(Identifier category) {
            return category.toLanguageKey("compiler_category");
        }
    }

    public static String makeKey(String type, String name) {
        return type + "." + LPConstants.ID + "." + name;
    }
}
