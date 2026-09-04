package logisticspipes.interfaces;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A block entity that opens its own screen.
 *
 * <p>Every one of ours needs the client to find it again before the menu can be built, so the
 * position is written here once instead of at each call site. A block entity with more to say
 * overrides this and writes its extra data after calling {@code super}.
 */
public interface IBlockEntityMenuProvider extends MenuProvider {

    @Override
    default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(((BlockEntity) this).getBlockPos());
    }
}
