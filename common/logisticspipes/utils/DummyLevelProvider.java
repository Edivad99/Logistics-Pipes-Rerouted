package logisticspipes.utils;

import net.minecraft.world.level.Level;

import logisticspipes.interfaces.IWorldProvider;

public class DummyLevelProvider implements IWorldProvider {

	private final Level world;

	public DummyLevelProvider(Level world) {
		this.world = world;
	}

	@Override
	public Level getWorld() {
		return world;
	}
}
