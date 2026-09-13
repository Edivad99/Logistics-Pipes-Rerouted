package logisticspipes.utils;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

public class DirectionUtil {

	@Nullable
	public static Direction getOrientation(int input) {
		if (input < 0 || Direction.values().length <= input) {
			return null;
		}
		return Direction.from3DDataValue(input);
	}
}
