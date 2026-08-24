package logisticspipes.utils;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
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

	private static final Map<DyeColor, MinecraftColor> BY_DYE;
	static {
		BY_DYE = new EnumMap<>(DyeColor.class);
		for (MinecraftColor color : values()) {
			if (color != BLANK) {
				BY_DYE.put(color.toDyeColor(), color);
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
			DyeColor dye = item.get(DataComponents.DYE);
			if (dye != null) {
				MinecraftColor color = BY_DYE.get(dye);
				if (color != null) return color;
			}
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
		return BuiltInRegistries.ITEM.get(toDyeColor().getTag())
			.flatMap(holders -> holders.stream().findFirst())
			.map(ItemStack::new)
			.orElse(ItemStack.EMPTY);
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
