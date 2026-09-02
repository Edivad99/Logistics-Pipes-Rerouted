package logisticspipes.network.to_server.module;

import java.lang.ref.WeakReference;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsEventListener;
import logisticspipes.network.to_client.module.QuickSortMarkerMessage;

import network.rs485.logisticspipes.module.AsyncQuicksortModule;

/**
 * The player opened or closed a container screen, so the quicksort modules aimed at it know
 * whether anyone is looking.
 *
 * <p>Opening and closing are one message with a flag: they are sent from the two arms of the same
 * {@code if}, and the modules answer both by adding or removing the same player.
 */
public record QuickSortChestWatchMessage(boolean watching) implements CustomPacketPayload {

    public static final Type<QuickSortChestWatchMessage> TYPE =
            new Type<>(LPConstants.rl("quick_sort_chest_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickSortChestWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, QuickSortChestWatchMessage::watching,
                    QuickSortChestWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuickSortChestWatchMessage message, IPayloadContext context) {
        final Player player = context.player();
        final List<WeakReference<AsyncQuicksortModule>> sorters =
                LogisticsEventListener.chestQuickSortConnection.get(player);
        if (sorters == null || sorters.isEmpty()) {
            return;
        }
        if (message.watching && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new QuickSortMarkerMessage());
        }
        for (WeakReference<AsyncQuicksortModule> sorter : sorters) {
            final AsyncQuicksortModule module = sorter.get();
            if (module == null) {
                continue;
            }
            if (message.watching) {
                module.addWatchingPlayer(player);
            } else {
                module.removeWatchingPlayer(player);
            }
        }
        if (!message.watching) {
            LogisticsEventListener.chestQuickSortConnection.remove(player);
        }
    }
}
