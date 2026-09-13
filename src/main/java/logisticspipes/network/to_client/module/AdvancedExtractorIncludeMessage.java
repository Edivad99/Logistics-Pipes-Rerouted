package logisticspipes.network.to_client.module;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.AsyncAdvancedExtractor;
import logisticspipes.network.ModuleTarget;

/**
 * Whether an advanced extractor's filter includes or excludes, for the players watching its HUD.
 */
public record AdvancedExtractorIncludeMessage(ModuleTarget target, boolean included)
    implements CustomPacketPayload {

    public static final Type<AdvancedExtractorIncludeMessage> TYPE =
        new Type<>(LPConstants.rl("advanced_extractor_include"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedExtractorIncludeMessage> STREAM_CODEC =
        StreamCodec.composite(
            ModuleTarget.STREAM_CODEC, AdvancedExtractorIncludeMessage::target,
            ByteBufCodecs.BOOL, AdvancedExtractorIncludeMessage::included,
            AdvancedExtractorIncludeMessage::new);

    public static void handle(AdvancedExtractorIncludeMessage message, IPayloadContext context) {
        final AsyncAdvancedExtractor module =
            message.target.resolve(context.player(), AsyncAdvancedExtractor.class);
        if (module != null) {
            module.getItemsIncluded().setValue(message.included);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
