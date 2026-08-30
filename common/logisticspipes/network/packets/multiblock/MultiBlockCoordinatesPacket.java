package logisticspipes.network.packets.multiblock;

import java.util.List;
import java.util.Set;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class MultiBlockCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private Set<DoubleCoordinates> targetPos;

	@Getter
	@Setter
	private List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes;

	public MultiBlockCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCollection(targetPos);
		output.writeCollection(subTypes, LPDataOutput::writeEnum);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		targetPos = input.readSet(DoubleCoordinates::new);
		subTypes = input.readArrayList(data1 -> data1.readEnum(CoreMultiBlockPipe.SubBlockTypeForShare.class));
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericSubMultiBlock block = this.getTileAs(player.level(), LogisticsTileGenericSubMultiBlock.class);
		block.setPosition(targetPos, subTypes);
	}

	@Override
	public ModernPacket template() {
		return new MultiBlockCoordinatesPacket(getId());
	}
}
