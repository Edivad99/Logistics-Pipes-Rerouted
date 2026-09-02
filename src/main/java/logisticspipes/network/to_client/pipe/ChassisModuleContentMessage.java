package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The modules a chassis pipe holds, for the players watching its HUD.
 */
public record ChassisModuleContentMessage(BlockPos pos, List<ItemIdentifierStack> modules)
        implements CustomPacketPayload {

    public static final Type<ChassisModuleContentMessage> TYPE =
            new Type<>(LPConstants.rl("chassis_module_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChassisModuleContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChassisModuleContentMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ChassisModuleContentMessage::modules,
                    ChassisModuleContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChassisModuleContentMessage message, IPayloadContext context) {
        final PipeLogisticsChassis chassis =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, PipeLogisticsChassis.class);
        if (chassis != null) {
            chassis.handleModuleItemIdentifierList(message.modules);
        }
    }
}
