package logisticspipes.network.packets.orderer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.RequestPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SubmitFluidRequestPacket extends RequestPacket {

    public SubmitFluidRequestPacket(int id) {
        super(id);
    }

    @Override
    public ModernPacket template() {
        return new SubmitFluidRequestPacket(getId());
    }

    @Override
    public void processPacket(Player player) {
        assert player instanceof ServerPlayer;
        final LogisticsTileGenericPipe pipe = MainProxy.getProxy(false)
            .getPipeInDimensionAt(getDimension(), getPosX(), getPosY(), getPosZ(), player);
        if (pipe != null && pipe.pipe instanceof CoreRoutedPipe coreRoutedPipe
            && pipe.pipe instanceof IRequestFluid requestFluid) {
            RequestHandler.requestFluid(player, getStack(), coreRoutedPipe, requestFluid);
        }
    }
}
