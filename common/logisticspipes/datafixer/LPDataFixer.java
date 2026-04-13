package logisticspipes.datafixer;

import net.minecraftforge.eventbus.api.IEventBus;
// Full DFU (DataFixerUpper) registration is not feasible for the 1.12.2→1.20.1 gap:
// Minecraft's own chunk format requires passing through every intermediate MC version.
// What IS covered by MissingMappingsEvent (fired on mod event bus):
//   • Item registry renames        — MissingMappingHandler (ITEMS, namespace "logisticspipes")
//   • Block registry renames       — MissingMappingHandler (BLOCKS, namespace "logisticspipes")
//   • Block entity type renames    — MissingMappingHandler (BLOCK_ENTITY_TYPES, namespace "minecraft")
// What remains unhandled:
//   • Item NBT damage→id migration — DataFixerSolidBlockItems.fixTagCompound() exists but is
//     not called; requires a DFU DataFixTypes.ITEM_STACK fixer or ChunkDataEvent.Load walk.

public class LPDataFixer {

	public static final LPDataFixer INSTANCE = new LPDataFixer();

	public static final int VERSION = 1;

	private LPDataFixer() {}

	public void init(IEventBus modEventBus) {
		// MissingMappingsEvent fires on the mod event bus in NeoForge 47.x.
		modEventBus.register(new MissingMappingHandler());
	}

}
