package logisticspipes.network.abstractpackets;

import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import java.util.Objects;

public abstract class ItemPacket extends CoordinatesPacket {

	@Getter
	@Setter
	@Nonnull
	private ItemStack stack = ItemStack.EMPTY;

	public ItemPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		if (getStack().isEmpty()) {
			output.writeInt(0);
		} else {
			output.writeInt(BuiltInRegistries.ITEM.getId(getStack().getItem()));
			output.writeInt(getStack().getCount());
			output.writeInt(getStack().getDamageValue());
			CompoundTag tag = null;
			if (getStack().has(DataComponents.CUSTOM_DATA)) {
				tag = Objects.requireNonNull(getStack().get(DataComponents.CUSTOM_DATA)).copyTag();
			}
			output.writeCompoundTag(tag);
		}
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);

		final int itemID = input.readInt();
		if (itemID == 0) {
			setStack(ItemStack.EMPTY);
		} else {
			int stackSize = input.readInt();
			int damage = input.readInt();
			ItemStack newStack = new ItemStack(BuiltInRegistries.ITEM.byId(itemID), stackSize);
			newStack.setDamageValue(damage);
			setStack(newStack);
			var tag = input.readCompoundTag();
			if (tag != null) {
				getStack().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
		}
	}
}
