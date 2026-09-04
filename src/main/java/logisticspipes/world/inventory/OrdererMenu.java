package logisticspipes.world.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import lombok.Getter;

import logisticspipes.network.RemotePipeTarget;
import logisticspipes.pipes.basic.CoreRoutedPipe;

/**
 * The request screen, which has no slots of its own.
 *
 * <p>It carries the pipe's dimension as well as its position: the remote orderer opens this on a
 * pipe in another dimension entirely, where looking the pipe up in the viewer's own level would
 * find nothing.
 */
public class OrdererMenu extends DummyMenu {

    @Getter
    private final RemotePipeTarget target;

    public OrdererMenu(MenuType<?> menuType, int containerId, Inventory inventory, RemotePipeTarget target) {
        super(menuType, containerId, inventory.player, null);
        this.target = target;
    }

    /** The pipe as the client will address it. */
    public static RemotePipeTarget targetOf(CoreRoutedPipe pipe) {
        return new RemotePipeTarget(pipe.getWorld().dimension().identifier(), pipe.getPos());
    }

    /**
     * Opens the plain request screen on any routed pipe.
     *
     * <p>Not a {@link net.minecraft.world.MenuProvider} on the pipe itself: three different things
     * open this screen -- the request pipe, a remote orderer in hand, and the remote orderer item
     * used on a pipe -- and only the last two decide the pipe from outside it.
     */
    public static void open(ServerPlayer player, CoreRoutedPipe pipe) {
        final RemotePipeTarget target = targetOf(pipe);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, viewer) ->
                        new OrdererMenu(LPMenuTypes.ORDERER.get(), containerId, inventory, target),
                Component.empty()),
            buffer -> RemotePipeTarget.STREAM_CODEC.encode(buffer, target));
    }
}
