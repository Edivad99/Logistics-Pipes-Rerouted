package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

@StaticResolve
public class CraftingSetType extends CoordinatesPacket {

	@Getter
	@Setter
	private ItemIdentifier targetType;

	public CraftingSetType(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity table = this.getTileAs(player.level(), BlockEntity.class);
		if (table instanceof LogisticsCraftingTableBlockEntity) {
			((LogisticsCraftingTableBlockEntity) table).targetType = targetType;
			((LogisticsCraftingTableBlockEntity) table).cacheRecipe();
		} else if (table instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) table).pipe instanceof PipeBlockRequestTable) {
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) table).pipe).targetType = targetType;
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) table).pipe).cacheRecipe();
		}
	}

	@Override
	public ModernPacket template() {
		return new CraftingSetType(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeItemIdentifier(targetType);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		targetType = input.readItemIdentifier();
	}
}
