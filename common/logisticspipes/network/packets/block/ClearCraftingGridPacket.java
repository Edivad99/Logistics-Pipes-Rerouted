package logisticspipes.network.packets.block;

import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@StaticResolve
public class ClearCraftingGridPacket extends CoordinatesPacket {

	public ClearCraftingGridPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity table = this.getTileAs(player.level(), BlockEntity.class);
		if (table instanceof LogisticsCraftingTableBlockEntity) {
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
