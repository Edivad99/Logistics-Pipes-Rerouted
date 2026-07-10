package logisticspipes.recipes;

import javax.annotation.Nonnull;
import lombok.Data;
import net.minecraft.world.item.ItemStack;

@Data
public class CraftingParts {

	/**
	 * Iron Chip
	 * FPGA
	 */
	@Nonnull
	private final ItemStack chipFpga;
	/**
	 * Gold Chip
	 * Basic Microcontroller
	 */
	@Nonnull
	private final ItemStack chipBasic;
	/**
	 * Diamond Chip
	 * Advanced Microcontroller
	 */
	@Nonnull
	private final ItemStack chipAdvanced;
}
