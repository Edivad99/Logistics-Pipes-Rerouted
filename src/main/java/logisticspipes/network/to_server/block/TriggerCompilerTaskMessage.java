package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity.CompilerTask;

/**
 * The player set a program compiler to work.
 *
 * <p>The kind of task is named rather than spelled: it used to travel as one of the strings
 * "category", "program" or "flash", which the compiler matched in two separate switches, the second
 * of which threw {@code UnsupportedOperationException} on anything else.
 */
public record TriggerCompilerTaskMessage(BlockPos pos, Identifier category, CompilerTask task)
        implements CustomPacketPayload {

    public static final Type<TriggerCompilerTaskMessage> TYPE =
            new Type<>(LPConstants.rl("trigger_compiler_task"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerCompilerTaskMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TriggerCompilerTaskMessage::pos,
                    Identifier.STREAM_CODEC, TriggerCompilerTaskMessage::category,
                    NeoForgeStreamCodecs.enumCodec(CompilerTask.class), TriggerCompilerTaskMessage::task,
                    TriggerCompilerTaskMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerCompilerTaskMessage message, IPayloadContext context) {
        final LogisticsProgramCompilerBlockEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsProgramCompilerBlockEntity.class);
        if (be != null) {
            be.triggerNewTask(message.category, message.task);
        }
    }
}
