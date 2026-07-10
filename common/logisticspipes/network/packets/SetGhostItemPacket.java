package logisticspipes.network.packets;

import javax.annotation.Nonnull;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packetcontent.IntegerContent;
import logisticspipes.network.packetcontent.ItemStackContent;
import logisticspipes.network.packetcontent.PacketContentBuilder;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@StaticResolve
public class SetGhostItemPacket extends ModernPacket {

	private final IntegerContent integer;
	private final ItemStackContent stack;

	public SetGhostItemPacket(int id) {
		super(id);
		PacketContentBuilder builder = new PacketContentBuilder();
		integer = builder.addInteger();
		stack = builder.addItemStack();
		builder.build(this);
	}

	@Override
	public void processPacket(Player player) {
		AbstractContainerMenu container = player.containerMenu;

		if (container != null) {
			if (integer.getValue() >= 0 && integer.getValue() < container.slots.size()) {
				Slot slot = container.getSlot(integer.getValue());

				if (slot instanceof DummySlot || slot instanceof FluidSlot) {
					slot.set(stack.getValue());
				}
			}
		}
	}

	@Override
	public ModernPacket template() {
		return new SetGhostItemPacket(getId());
	}

	public SetGhostItemPacket putInt(int value) {
		integer.setValue(value);
		return this;
	}

	public SetGhostItemPacket setStack(@Nonnull ItemStack value) {
		stack.setValue(value);
		return this;
	}
}
