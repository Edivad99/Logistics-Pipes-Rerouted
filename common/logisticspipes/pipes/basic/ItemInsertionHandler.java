package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.interfaces.ILPItemAcceptor;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemInsertionHandler implements IItemHandler {

	public static final List<ILPItemAcceptor> ACCEPTORS = new ArrayList<>();

	private final LogisticsTileGenericPipe pipe;
	private final Direction dir;

	public ItemInsertionHandler(LogisticsTileGenericPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) { // getItem → getStackInSlot in IItemHandler 1.20.1
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (!simulate) {
			return handleItemInsetion(pipe, dir, stack);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	public static ItemStack handleItemInsetion(LogisticsTileGenericPipe pipe, Direction from, ItemStack stack) {
		for (ILPItemAcceptor acceptor : ACCEPTORS) {
			if (acceptor.accept(pipe, from, stack)) {
				return ItemStack.EMPTY;
			}
		}
		return pipe.insertItem(from, stack);
	}
}
