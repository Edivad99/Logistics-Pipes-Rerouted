package logisticspipes.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Opening a block's own screen on a player's behalf.
 */
public final class BlockMenus {

    private BlockMenus() {
    }

    /**
     * Opens the block's screen for the player, the way right-clicking it would.
     *
     * <p>Asking the block how it opens rather than replaying a right click is what makes this safe
     * to do on the player's behalf: no item is used, so nothing gets placed or wrenched, and a
     * block that refuses to open right now -- a chest with something sitting on top -- says so by
     * having no provider. Replaying a click stopped opening containers at all in 1.21.
     *
     * <p>The block entity is asked before the block: our own blocks carry their own
     * {@link MenuProvider}, which a vanilla block state lookup would not find.
     *
     * @return whether a screen was opened
     */
    public static boolean openFor(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider menuProvider) {
            return player.openMenu(menuProvider).isPresent();
        }
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        final MenuProvider provider = state.getMenuProvider(level, pos);
        return provider != null && player.openMenu(provider).isPresent();
    }
}
