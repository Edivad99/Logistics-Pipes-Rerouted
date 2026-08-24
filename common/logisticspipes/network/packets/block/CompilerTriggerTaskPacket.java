package logisticspipes.network.packets.block;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CompilerTriggerTaskPacket extends CoordinatesPacket {

	public CompilerTriggerTaskPacket(int id) {
		super(id);
	}

	@Getter
	@Setter
	private Identifier category;

	@Getter
	@Setter
	private String type;

	@Override
	public void processPacket(Player player) {
		LogisticsProgramCompilerBlockEntity tile = this.getTileAs(player.level(), LogisticsProgramCompilerBlockEntity.class);
		tile.triggerNewTask(getCategory(), getType());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeIdentifier(category);
		output.writeUTF(type);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		category = input.readIdentifier();
		type = input.readUTF();
	}

	@Override
	public ModernPacket template() {
		return new CompilerTriggerTaskPacket(getId());
	}
}
