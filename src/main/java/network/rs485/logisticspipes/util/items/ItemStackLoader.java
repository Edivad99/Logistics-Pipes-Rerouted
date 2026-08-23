package network.rs485.logisticspipes.util.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemStackLoader {

	/** The key a bare stack is stored under when it has no name of its own in the parent. */
	private static final String DEFAULT_KEY = "item";

	/**
	 * Reads a stack, yielding {@link ItemStack#EMPTY} when the key is absent or unreadable.
	 *
	 * <p>1.21.6 removed {@code ItemStack#parse(HolderLookup.Provider, Tag)} along with the rest of
	 * the direct-NBT surface; stacks go through {@link ItemStack#CODEC} on a {@link ValueInput},
	 * which supplies the registry ops itself.</p>
	 */
	public static ItemStack loadItemStack(ValueInput input, String key) {
		return input.read(key, ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	public static ItemStack loadItemStack(ValueInput input) {
		return loadItemStack(input, DEFAULT_KEY);
	}

	/** Writes a stack, skipping empty ones so absent and empty stay the same thing on read. */
	public static void saveItemStack(ValueOutput output, String key, ItemStack stack) {
		if (!stack.isEmpty()) {
			output.store(key, ItemStack.CODEC, stack);
		}
	}

	public static void saveItemStack(ValueOutput output, ItemStack stack) {
		saveItemStack(output, DEFAULT_KEY, stack);
	}
}
