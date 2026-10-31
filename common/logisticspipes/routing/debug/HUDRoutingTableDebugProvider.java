package logisticspipes.routing.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;

public class HUDRoutingTableDebugProvider implements IHeadUpDisplayRendererProvider {

    private final IHeadUpDisplayRenderer hud;
    private final BlockPos pos;

    HUDRoutingTableDebugProvider(IHeadUpDisplayRenderer hud, BlockPos pos) {
        this.hud = hud;
        this.pos = pos;
    }

    @Override
    public IHeadUpDisplayRenderer getRenderer() {
        return hud;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public @Nullable Level getLevelForHUD() {
        return null;
    }

    @Override
    public void startWatching() {
    }

    @Override
    public void stopWatching() {
    }
}
