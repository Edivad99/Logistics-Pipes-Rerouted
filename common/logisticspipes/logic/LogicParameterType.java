package logisticspipes.logic;

import lombok.Getter;
import net.minecraft.core.Direction;

public enum LogicParameterType {
	Number(long.class),
	Float(double.class),
	Boolean(boolean.class),
	Direction(Direction.class);

	@Getter
	private final Class<?> javaClass;

	LogicParameterType(Class<?> clazz) {
		javaClass = clazz;
	}
}
