package logisticspipes.network.to_client.module;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.network.ModuleTarget;

/**
 * The list a string-based item sink settled on, for the players watching its HUD.
 *
 * <p>Sent on every change and again to each player as they start watching, so a dropped one is
 * made good by the next subscribe.
 */
public record StringBasedItemSinkListMessage(ModuleTarget target, List<String> names)
        implements CustomPacketPayload {

    public static final Type<StringBasedItemSinkListMessage> TYPE =
            new Type<>(LPConstants.rl("string_based_item_sink_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StringBasedItemSinkListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, StringBasedItemSinkListMessage::target,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    StringBasedItemSinkListMessage::names,
                    StringBasedItemSinkListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StringBasedItemSinkListMessage message, IPayloadContext context) {
        final IStringBasedModule module =
                message.target.resolve(context.player(), IStringBasedModule.class);
        if (module != null) {
            module.stringListProperty().replaceContent(message.names);
        }
    }
}
