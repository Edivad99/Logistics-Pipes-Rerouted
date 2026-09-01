package logisticspipes.network.to_client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import network.rs485.logisticspipes.property.PropertyHolder;

/**
 * A pipe's properties, as the server has them.
 *
 * <p>Sent in answer to every {@link logisticspipes.network.to_server.SetPipePropertiesMessage}, so
 * the client never has to assume its own edit took.
 */
public record PipePropertiesMessage(BlockPos pos, CompoundTag properties) implements CustomPacketPayload {

    public static final Type<PipePropertiesMessage> TYPE = new Type<>(LPConstants.rl("pipe_properties"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipePropertiesMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipePropertiesMessage::pos,
                    ByteBufCodecs.COMPOUND_TAG, PipePropertiesMessage::properties,
                    PipePropertiesMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Everything the holder has, for a full resync. */
    public static PipePropertiesMessage of(BlockPos pos, PropertyHolder holder, HolderLookup.Provider registries) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        PropertyHolder.serialize(output, holder);
        return new PipePropertiesMessage(pos, output.buildResult());
    }

    public static void handle(PipePropertiesMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof PropertyHolder)) {
            return;
        }
        be.pipe.deserialize(TagValueInput.create(ProblemReporter.DISCARDING,
                context.player().level().registryAccess(), message.properties));
    }
}
