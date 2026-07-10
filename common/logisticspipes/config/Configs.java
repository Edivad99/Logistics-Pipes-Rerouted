package logisticspipes.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Configs {

	public static final String CATEGORY_MULTITHREAD = "multithread";
	public static final String CATEGORY_PERFORMANCE = "performance";

	public static final Common COMMON;
	private static final ModConfigSpec COMMON_SPEC;

	static {
		final var commonPair = new ModConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = commonPair.getRight();
		COMMON = commonPair.getLeft();
	}

	public static void registerConfig(ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, Configs.COMMON_SPEC);
	}

	public static class Common {
		// Value holders (package-private, read in load())
		public final ModConfigSpec.IntValue     LOGISTICS_DETECTION_LENGTH;
		public final ModConfigSpec.IntValue     LOGISTICS_DETECTION_COUNT;
		public final ModConfigSpec.IntValue     LOGISTICS_DETECTION_FREQUENCY;
		public final ModConfigSpec.BooleanValue LOGISTICS_ORDERER_COUNT_INVERTWHEEL;
		public final ModConfigSpec.BooleanValue LOGISTICS_ORDERER_PAGE_INVERTWHEEL;
		public final ModConfigSpec.BooleanValue DISPLAY_POPUP;
		public final ModConfigSpec.IntValue     MAX_UNROUTED_CONNECTIONS;
		public final ModConfigSpec.IntValue     LOGISTICS_HUD_RENDER_DISTANCE;
		public final ModConfigSpec.DoubleValue PIPE_DURABILITY;
		public final ModConfigSpec.BooleanValue LOGISTICS_POWER_USAGE_DISABLED;
		public final ModConfigSpec.DoubleValue POWER_USAGE_MULTIPLIER;
		public final ModConfigSpec.DoubleValue COMPILER_SPEED;
		public final ModConfigSpec.BooleanValue ENABLE_RESEARCH_SYSTEM;
		public final ModConfigSpec.IntValue LOGISTICS_CRAFTING_TABLE_POWER_USAGE;
		public final ModConfigSpec.BooleanValue TOOLTIP_INFO;
		public final ModConfigSpec.BooleanValue ENABLE_PARTICLE_FX;
		public final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES;
		public final ModConfigSpec.BooleanValue EASTER_EGGS;
		public final ModConfigSpec.BooleanValue OPAQUE;
		public final ModConfigSpec.IntValue MAX_ROBOT_DISTANCE;
		public final ModConfigSpec.IntValue MULTI_THREAD_NUMBER;
		public final ModConfigSpec.IntValue MULTI_THREAD_PRIORITY;
		public final ModConfigSpec.BooleanValue DISABLE_ASYNC_WORK;
		public final ModConfigSpec.IntValue MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK;
		public final ModConfigSpec.IntValue MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK;
		public final ModConfigSpec.IntValue MINIMUM_JOB_TICK_LENGTH;
		public final ModConfigSpec.EnumValue<PowerSourceMode> POWER_SOURCE_MODE;

		private Common(ModConfigSpec.Builder b) {
			b.comment("Pipe network detection settings").push("detection");
			LOGISTICS_DETECTION_LENGTH         = b.comment("Max detection length for pipe network scan").defineInRange("detectionLength", 50, 1, 1000);
			LOGISTICS_DETECTION_COUNT          = b.comment("Max pipes counted during detection").defineInRange("detectionCount", 100, 1, 10000);
			LOGISTICS_DETECTION_FREQUENCY      = b.comment("Detection frequency in ticks").defineInRange("detectionFrequency", 20 * 30, 1, Integer.MAX_VALUE);
			MAX_UNROUTED_CONNECTIONS = b.comment("Max unrouted connections per pipe").defineInRange("maxUnroutedConnections", 32, 1, 512);
			b.pop();
			b.comment("Orderer GUI settings").push("orderer");
			LOGISTICS_ORDERER_COUNT_INVERTWHEEL = b.comment("Invert mouse wheel for item count in orderer").define("invertCountWheel", false);
			LOGISTICS_ORDERER_PAGE_INVERTWHEEL  = b.comment("Invert mouse wheel for page navigation in orderer").define("invertPageWheel", false);
			DISPLAY_POPUP = b.comment("Set the default configuration for the popup of the Orderer Gui. Should it be used?").define("displayPopup", true);
			b.pop();

			b.comment("HUD settings").push("hud");
			LOGISTICS_HUD_RENDER_DISTANCE = b.comment("HUD render distance in blocks").defineInRange("hudRenderDistance", 15, 1, 256);
			TOOLTIP_INFO = b.comment("Show extra tooltip info").define("tooltipInfo", false);
			OPAQUE = b.comment("Make pipes opaque").define("opaque", false);
			b.pop();

			b.comment("Power settings").push("power");
			PIPE_DURABILITY = b.comment("Pipe block durability").defineInRange("pipeDurability", 0.25, 0.0, 1.0);
			LOGISTICS_POWER_USAGE_DISABLED       = b.comment("Disable power usage entirely").define("powerUsageDisabled", false);
			POWER_USAGE_MULTIPLIER = b.comment("Power usage multiplier").defineInRange("powerUsageMultiplier", 1.0, 0.0, 100.0);
			LOGISTICS_CRAFTING_TABLE_POWER_USAGE = b.comment("Power used per crafting operation (RF)").defineInRange("craftingTablePowerUsage", 250, 0, Integer.MAX_VALUE);
			POWER_SOURCE_MODE = b.comment("How the RF power junction acquires FE. ADJACENT: pulls from any neighbouring IEnergyStorage each tick. CABLE: passive — FE cables push into the junction.").defineEnum("powerSourceMode", PowerSourceMode.ADJACENT);
			b.pop();

			b.comment("Logistics system settings").push("logistics");
			COMPILER_SPEED = b.comment("Program compiler speed multiplier").defineInRange("compilerSpeed", 1.0, 0.01, 100.0);
			ENABLE_RESEARCH_SYSTEM = b.comment("Enable research system").define("enableResearchSystem", false);
			ENABLE_PARTICLE_FX = b.comment("Enable particle effects").define("enableParticleFx", true);
			CHECK_FOR_UPDATES = b.comment("Check for mod updates on startup").define("checkForUpdates", true);
			EASTER_EGGS = b.comment("Enable easter eggs").define("easterEggs", true);
			MAX_ROBOT_DISTANCE = b.comment("Max robot operation distance in blocks").defineInRange("maxRobotDistance", 64, 1, 512);
			b.pop();

			b.comment("Multithreading settings").push(CATEGORY_MULTITHREAD);
			MULTI_THREAD_NUMBER = b.comment("Number of routing worker threads").defineInRange("threadCount", 4, 1, 32);
			MULTI_THREAD_PRIORITY = b.comment("Worker thread priority").defineInRange("threadPriority", Thread.NORM_PRIORITY, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
			b.pop();

			b.comment("Performance settings").push(CATEGORY_PERFORMANCE);
			DISABLE_ASYNC_WORK = b.comment("Disable async work processing (use main thread only)").define("disableAsyncWork", false);
			MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = b.comment("Minimum inventory slot accesses per tick").defineInRange("minSlotAccess", 10, 1, Integer.MAX_VALUE);
			MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = b.comment("Maximum inventory slot accesses per tick (0 = unlimited)").defineInRange("maxSlotAccess", 0, 0, Integer.MAX_VALUE);
			MINIMUM_JOB_TICK_LENGTH = b.comment("Minimum ticks between async jobs").defineInRange("minJobTickLength", 1, 1, Integer.MAX_VALUE);
			b.pop();
		}
	}

	// ── Public static fields (populated from spec in load()) ──────────────────
	public static final float LOGISTICS_ROUTED_SPEED_MULTIPLIER = 20F;
	public static final float LOGISTICS_DEFAULTROUTED_SPEED_MULTIPLIER = 10F;
	public static float pipeDurability = 0.25F;

	public static int[] CHASSIS_SLOTS_ARRAY = {1, 2, 3, 4, 8};

	public static void savePopupState() {
		// Mirror LP1: write the current popup preference back to the config file and persist it
		// immediately so the toggle survives a restart.
//		DISPLAY_POPUP_V.set(DISPLAY_POPUP);
//		DISPLAY_POPUP_V.save();
		COMMON_SPEC.save();
	}

	public enum PowerSourceMode {
		/** Pull FE from any adjacent IEnergyStorage each tick (active). */
		ADJACENT,
		/** Accept FE pushed in by cables (passive, IEnergyStorage capability only). */
		CABLE
	}
}
