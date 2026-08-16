package logisticspipes.utils;

import logisticspipes.interfaces.IWorldProvider;
import net.minecraft.world.level.Level;

public class DummyLevelProvider implements IWorldProvider {

	private final Level level;

	public DummyLevelProvider(Level level) {
		this.level = level;
	}

	@Override
	public Level getWorld() {
		return level;
	}
}
