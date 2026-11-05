package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Something that has a HUD panel floating at its position.
 */
public interface IHeadUpDisplayRendererProvider {

    IHeadUpDisplayRenderer getRenderer();

    BlockPos getPos();

    @Nullable Level getLevelForHUD();

    void startWatching();

    void stopWatching();
}
