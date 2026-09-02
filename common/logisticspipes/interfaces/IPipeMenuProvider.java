package logisticspipes.interfaces;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;

import logisticspipes.pipes.basic.CoreUnroutedPipe;

/**
 * A pipe that opens its own screen.
 *
 * <p>The pipe rather than its block entity: one block entity hosts any kind of pipe, so the
 * screen belongs to what is inside it. The position is written for the client to find the pipe
 * again; a pipe with more to say overrides this and writes it after calling {@code super}.
 *
 * <p>The title goes unused -- our screens draw their own -- but a menu provider has to offer one.
 */
public interface IPipeMenuProvider extends MenuProvider {

    @Override
    default Component getDisplayName() {
        return Component.empty();
    }

    @Override
    default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(((CoreUnroutedPipe) this).getPos());
    }
}
