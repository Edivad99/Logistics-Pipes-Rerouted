package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ClearCraftingGridPacket extends CoordinatesPacket {

	public ClearCraftingGridPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity table = this.getTileAs(player.level(), BlockEntity.class);
		if (table instanceof LogisticsCraftingTableTileEntity) {
		} else if (table instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) table).pipe instanceof PipeBlockRequestTable) {
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) table).pipe).matrix.clearGrid();
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) table).pipe).cacheRecipe();
		}
	}

	@Override
	public ModernPacket template() {
		return new ClearCraftingGridPacket(getId());
	}
}
