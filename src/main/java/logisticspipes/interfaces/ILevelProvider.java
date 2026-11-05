package logisticspipes.interfaces;

import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Something that may be in a level: a module held in hand, for instance, is not.
 */
public interface ILevelProvider {

    @Nullable Level getLevel();
}
