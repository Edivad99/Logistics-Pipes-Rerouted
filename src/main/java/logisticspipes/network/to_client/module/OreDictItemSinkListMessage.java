package logisticspipes.network.to_client.module;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.ModuleTarget;

/**
 * The ore dictionary list a module settled on, for the players watching its HUD.
 *
 * <p>Sent on every change and again to each player as they start watching, so a dropped one is
 * made good by the next subscribe.
 */
public record OreDictItemSinkListMessage(ModuleTarget target, List<String> oreNames)
        implements CustomPacketPayload {

    public static final Type<OreDictItemSinkListMessage> TYPE =
            new Type<>(LPConstants.rl("ore_dict_item_sink_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreDictItemSinkListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, OreDictItemSinkListMessage::target,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    OreDictItemSinkListMessage::oreNames,
                    OreDictItemSinkListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OreDictItemSinkListMessage message, IPayloadContext context) {
        final ModuleOreDictItemSink module =
                message.target.resolve(context.player(), ModuleOreDictItemSink.class);
        if (module != null) {
            module.setOreList(message.oreNames);
        }
    }
}
