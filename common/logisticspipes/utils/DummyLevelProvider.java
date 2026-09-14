package logisticspipes.utils;

import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ILevelProvider;

public record DummyLevelProvider(Level level) implements ILevelProvider {

    @Override
    public Level getLevel() {
        return level;
    }
}
