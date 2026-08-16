package logisticspipes.utils;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum MinecraftColor {
	BLACK(0xff000000),
	RED(0xffff0000),
	GREEN(0xff00ff00),
	BROWN(0xff895836),
	BLUE(0xff0000ff),
	PURPLE(0xffB064D8),
	CYAN(0xff3C8EB0),
	LIGHT_GRAY(0xffBABAC1),
	GRAY(0xff848484),
	PINK(0xffF7B4D6),
	LIME(0xff83D41C),
	YELLOW(0xffE7E72A),
	LIGHT_BLUE(0xff82ACE7),
	MAGENTA(0xffDB7AD5),
	ORANGE(0xffE69E34),
	WHITE(0xffffffff),
	BLANK(0x00000000);

	private final int colorCode;

	MinecraftColor(int colorCode) {
		this.colorCode = colorCode;
	}

	// In 1.20.1 dyes are separate items; map via DyeItem / DyeColor
	private static final java.util.Map<Item, MinecraftColor> DYE_TO_COLOR;
	static {
		DYE_TO_COLOR = new java.util.HashMap<>();
		for (MinecraftColor color : values()) {
			if (color != BLANK) {
				Item dyeItem = DyeItem.byColor(color.toDyeColor());
				DYE_TO_COLOR.put(dyeItem, color);
			}
		}
	}

	private DyeColor toDyeColor() {
		return switch (this) {
			case BLACK -> DyeColor.BLACK;
			case RED -> DyeColor.RED;
			case GREEN -> DyeColor.GREEN;
			case BROWN -> DyeColor.BROWN;
			case BLUE -> DyeColor.BLUE;
			case PURPLE -> DyeColor.PURPLE;
			case CYAN -> DyeColor.CYAN;
			case LIGHT_GRAY -> DyeColor.LIGHT_GRAY;
			case GRAY -> DyeColor.GRAY;
			case PINK -> DyeColor.PINK;
			case LIME -> DyeColor.LIME;
			case YELLOW -> DyeColor.YELLOW;
			case LIGHT_BLUE -> DyeColor.LIGHT_BLUE;
			case MAGENTA -> DyeColor.MAGENTA;
			case ORANGE -> DyeColor.ORANGE;
			case WHITE -> DyeColor.WHITE;
			default -> throw new IllegalStateException("No DyeColor for " + this);
		};
	}

	public static MinecraftColor getColor(ItemStack item) {
		if (!item.isEmpty()) {
			MinecraftColor color = DYE_TO_COLOR.get(item.getItem());
			if (color != null) return color;
		}
		return BLANK;
	}

	public int getColorCode() {
		return colorCode;
	}

	public ItemStack getItemStack() {
		if (this == BLANK) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(DyeItem.byColor(toDyeColor()));
	}

	public MinecraftColor getNext() {
		if (this == BLANK) {
			return BLACK;
		}
		return MinecraftColor.values()[ordinal() + 1];
	}

	public MinecraftColor getPrev() {
		if (this == BLACK) {
			return BLANK;
		}
		return MinecraftColor.values()[ordinal() - 1];
	}
}
