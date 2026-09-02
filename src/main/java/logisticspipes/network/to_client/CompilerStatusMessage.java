package logisticspipes.network.to_client;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

/**
 * What a program compiler is doing, for the players watching its screen.
 *
 * <p>The two stacks come along because the compiler's inventory is not a container the client has
 * open -- it reads the disk and the programmer straight off the block entity.
 *
 * @param currentTask what is being compiled, empty when the compiler is idle
 * @param hadPower    whether the last tick found the power it needed; the screen greys out when not
 */
public record CompilerStatusMessage(
        BlockPos pos,
        Optional<Identifier> currentTask,
        double progress,
        boolean hadPower,
        ItemStack disk,
        ItemStack programmer
) implements CustomPacketPayload {

    public static final Type<CompilerStatusMessage> TYPE = new Type<>(LPConstants.rl("compiler_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompilerStatusMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CompilerStatusMessage::pos,
                    ByteBufCodecs.optional(Identifier.STREAM_CODEC), CompilerStatusMessage::currentTask,
                    ByteBufCodecs.DOUBLE, CompilerStatusMessage::progress,
                    ByteBufCodecs.BOOL, CompilerStatusMessage::hadPower,
                    ItemStack.OPTIONAL_STREAM_CODEC, CompilerStatusMessage::disk,
                    ItemStack.OPTIONAL_STREAM_CODEC, CompilerStatusMessage::programmer,
                    CompilerStatusMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompilerStatusMessage message, IPayloadContext context) {
        final LogisticsProgramCompilerBlockEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsProgramCompilerBlockEntity.class);
        if (be != null) {
            be.setStateOnClient(message);
        }
    }
}
