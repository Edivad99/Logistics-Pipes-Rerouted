package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OrdererRefreshRequestPacket extends IntegerCoordinatesPacket {

    public OrdererRefreshRequestPacket(int id) {
        super(id);
    }

    @Override
    public ModernPacket template() {
        return new OrdererRefreshRequestPacket(getId());
    }

    @Override
    public void processPacket(Player player) {
        final LogisticsTileGenericPipe pipe = MainProxy.getProxy(false).getPipeInDimensionAt(getDimension(), getPosX(), getPosY(), getPosZ(), player);
        if (pipe != null && pipe.pipe instanceof CoreRoutedPipe coreRoutedPipe) {
            RequestHandler.DisplayOptions option = switch (getInteger() % 10) {
                case 1 -> RequestHandler.DisplayOptions.SupplyOnly;
                case 2 -> RequestHandler.DisplayOptions.CraftOnly;
                default -> RequestHandler.DisplayOptions.Both;
            };
            RequestHandler.refresh(player, coreRoutedPipe, option);
        }
    }
}
