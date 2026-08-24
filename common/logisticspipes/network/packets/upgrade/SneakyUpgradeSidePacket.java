package logisticspipes.network.packets.upgrade;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.SlotPacket;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.UpgradeSlot;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SneakyUpgradeSidePacket extends SlotPacket {

	@Setter
	@Getter
	private Direction side;

	public SneakyUpgradeSidePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		UpgradeSlot slot = getSlot(player, UpgradeSlot.class);
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) return;
		stack.update(
				DataComponents.CUSTOM_DATA,
				CustomData.EMPTY,
				customData -> {
					CompoundTag tag = customData.copyTag();
					tag.putString(SneakyUpgradeConfig.SIDE_KEY, SneakyUpgradeConfig.Sides.getNameForDirection(side));
					return CustomData.of(tag);
				}
		);
		slot.set(stack);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(side);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		side = input.readFacing();
	}

	@Override
	public ModernPacket template() {
		return new SneakyUpgradeSidePacket(getId());
	}
}
