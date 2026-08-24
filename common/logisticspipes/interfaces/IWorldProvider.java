package logisticspipes.interfaces;

import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

public interface IWorldProvider {

	@Nullable
    Level getWorld();
}
