package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PipePropertiesMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

import network.rs485.logisticspipes.property.PropertyHolder;

/**
 * The properties the player changed in a pipe's GUI.
 *
 * <p>The pipe's counterpart to {@link SetModulePropertiesMessage}: same open-ended bag of NBT, same
 * full-state answer.
 */
public record SetPipePropertiesMessage(BlockPos pos, CompoundTag properties) implements CustomPacketPayload {

    public static final Type<SetPipePropertiesMessage> TYPE =
            new Type<>(LPConstants.rl("set_pipe_properties"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPipePropertiesMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetPipePropertiesMessage::pos,
                    ByteBufCodecs.COMPOUND_TAG, SetPipePropertiesMessage::properties,
                    SetPipePropertiesMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** What the holder has, for the GUI to send on close: normally only the properties it changed. */
    public static SetPipePropertiesMessage of(BlockPos pos, PropertyHolder holder,
            HolderLookup.Provider registries) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        PropertyHolder.serialize(output, holder);
        return new SetPipePropertiesMessage(pos, output.buildResult());
    }

    public static void handle(SetPipePropertiesMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof PropertyHolder holder)
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final RegistryAccess registries = player.level().registryAccess();
        be.pipe.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, message.properties));
        PacketDistributor.sendToPlayer(player, PipePropertiesMessage.of(message.pos, holder, registries));
    }
}
