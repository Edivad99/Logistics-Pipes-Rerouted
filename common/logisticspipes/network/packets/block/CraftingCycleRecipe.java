package logisticspipes.network.packets.block;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CraftingCycleRecipe extends CoordinatesPacket {

	@Getter
	@Setter
	private boolean down;

	public CraftingCycleRecipe(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity table = this.getTileAs(player.level(), BlockEntity.class);
		if (table instanceof LogisticsCraftingTableTileEntity) {
			((LogisticsCraftingTableTileEntity) table).cycleRecipe(down);
		} else if (table instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) table).pipe instanceof PipeBlockRequestTable) {
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) table).pipe).cycleRecipe(down);
		}
	}

	@Override
	public ModernPacket template() {
		return new CraftingCycleRecipe(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBoolean(down);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		down = input.readBoolean();
	}
}
