package logisticspipes.network.abstractguis;

import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.utils.gui.DummyContainer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class UpgradeCoordinatesGuiProvider extends CoordinatesPopupGuiProvider {

	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PRIVATE)
	private int positionInt;

	public UpgradeCoordinatesGuiProvider(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(positionInt);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		positionInt = input.readInt();
	}

	public UpgradeCoordinatesGuiProvider setSlot(Slot slot) {
		this.setPositionInt(slot.index);
		return this;
	}

	public <T extends Slot> T getSlot(Player player, Class<T> clazz) {
		if (player.containerMenu instanceof DummyContainer) {
			if (positionInt >= player.containerMenu.slots.size()) {
				throw new TargetNotFoundException("The requested Slot was out of range", this);
			} else {
				Slot slot = player.containerMenu.getSlot(positionInt);
				if (slot == null) {
					throw new TargetNotFoundException("The requested Slot was null", this);
				} else if (!clazz.isAssignableFrom(slot.getClass())) {
					throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", found slot with " + slot.getClass(), this);
				} else {
					return (T) slot;
				}
			}
		}
		return null;
	}

}
