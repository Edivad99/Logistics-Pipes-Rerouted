package logisticspipes.network.packets.block;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

@StaticResolve
public class CompilerStatusPacket extends CoordinatesPacket {

	public CompilerStatusPacket(int id) {
		super(id);
	}

	@Getter
	@Setter
	private Identifier category;

	@Getter
	@Setter
	private double progress;

	@Getter
	@Setter
	private boolean wasAbleToConsumePower;

	@Getter
	@Setter
    private ItemStack disk;

	@Getter
	@Setter
    private ItemStack programmer;

	@Override
	public void processPacket(Player player) {
		LogisticsProgramCompilerBlockEntity tile = this.getTileAs(player.level(), LogisticsProgramCompilerBlockEntity.class);
		tile.setStateOnClient(this);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeIdentifier(category);
		output.writeDouble(progress);
		output.writeBoolean(wasAbleToConsumePower);
		output.writeItemStack(disk);
		output.writeItemStack(programmer);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		category = input.readIdentifier();
		progress = input.readDouble();
		wasAbleToConsumePower = input.readBoolean();
		disk = input.readItemStack();
		programmer = input.readItemStack();
	}

	@Override
	public ModernPacket template() {
		return new CompilerStatusPacket(getId());
	}
}
