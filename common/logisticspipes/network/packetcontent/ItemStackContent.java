package logisticspipes.network.packetcontent;

import net.minecraft.world.item.ItemStack;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public class ItemStackContent implements IPacketContent<ItemStack> {

	private ItemStack stack = ItemStack.EMPTY;

	@Override
    public ItemStack getValue() {
		return stack;
	}

	@Override
	public void setValue(ItemStack value) {
		stack = value;
	}

	@Override
	public void readData(LPDataInput input) {
		stack = input.readItemStack();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemStack(stack);
	}
}
