package logisticspipes.utils;

import logisticspipes.interfaces.IWorldProvider;
import net.minecraft.world.level.Level;

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
