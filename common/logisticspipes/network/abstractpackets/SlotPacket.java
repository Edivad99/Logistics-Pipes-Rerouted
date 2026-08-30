package logisticspipes.network.abstractpackets;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.gui.DummyContainer;

public abstract class SlotPacket extends ModernPacket {

	@Setter(AccessLevel.PRIVATE)
	@Getter(AccessLevel.PROTECTED)
	private int integer;

	public SlotPacket(int id) {
		super(id);
	}

	public <T extends Slot> T getSlot(Player player, Class<T> clazz) {
		if (player.containerMenu instanceof DummyContainer) {
			if (getInteger() >= player.containerMenu.slots.size()) {
				targetNotFound("The requested Slot was out of range");
			} else {
				Slot slot = player.containerMenu.getSlot(getInteger());
				if (slot == null) {
					targetNotFound("The requested Slot was null");
				} else if (!clazz.isAssignableFrom(slot.getClass())) {
					targetNotFound("Couldn't find " + clazz.getName() + ", found slot with " + slot.getClass());
				} else {
					return (T) slot;
				}
			}
		}
		return null;
	}

	public SlotPacket setSlot(Slot slot) {
		setInteger(slot.index);
		return this;
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setInteger(input.readInt());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(getInteger());
	}

	protected void targetNotFound(String message) {
		throw new TargetNotFoundException(message, this);
	}
}
