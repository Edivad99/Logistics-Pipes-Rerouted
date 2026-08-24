package logisticspipes.utils;

import net.minecraft.world.level.Level;

import logisticspipes.interfaces.IWorldProvider;

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
