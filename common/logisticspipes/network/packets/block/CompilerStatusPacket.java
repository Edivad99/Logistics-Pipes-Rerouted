package logisticspipes.network.packets.block;

import javax.annotation.Nonnull;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CompilerStatusPacket extends CoordinatesPacket {

	public CompilerStatusPacket(int id) {
		super(id);
	}

	@Getter
	@Setter
	private ResourceLocation category;

	@Getter
	@Setter
	private double progress;

	@Getter
	@Setter
	private boolean wasAbleToConsumePower;

	@Getter
	@Setter
	@Nonnull
	private ItemStack disk;

	@Getter
	@Setter
	@Nonnull
	private ItemStack programmer;

	@Override
	public void processPacket(Player player) {
		LogisticsProgramCompilerBlockEntity tile = this.getTileAs(player.level(), LogisticsProgramCompilerBlockEntity.class);
		tile.setStateOnClient(this);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeResourceLocation(category);
		output.writeDouble(progress);
		output.writeBoolean(wasAbleToConsumePower);
		output.writeItemStack(disk);
		output.writeItemStack(programmer);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		category = input.readResourceLocation();
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
