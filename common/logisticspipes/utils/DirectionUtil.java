package logisticspipes.utils;

import net.minecraft.core.Direction;

public class DirectionUtil {

	public static Direction getOrientation(int input) {
		if (input < 0 || Direction.values().length <= input) {
			return null;
		}
		return Direction.from3DDataValue(input);
	}
}
