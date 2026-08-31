package logisticspipes.network.to_client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.ModuleTarget;

/**
 * Whether an item sink is the default route, for the players watching its HUD.
 */
public record ItemSinkDefaultRouteMessage(ModuleTarget target, boolean defaultRoute)
        implements CustomPacketPayload {

    public static final Type<ItemSinkDefaultRouteMessage> TYPE =
            new Type<>(LPConstants.rl("item_sink_default_route"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSinkDefaultRouteMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ItemSinkDefaultRouteMessage::target,
                    ByteBufCodecs.BOOL, ItemSinkDefaultRouteMessage::defaultRoute,
                    ItemSinkDefaultRouteMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemSinkDefaultRouteMessage message, IPayloadContext context) {
        final ModuleItemSink module = message.target.resolve(context.player(), ModuleItemSink.class);
        if (module != null) {
            module.setDefaultRoute(message.defaultRoute);
        }
    }
}
