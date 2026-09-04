package logisticspipes.network.to_client.security;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.client.gui.screen.SecurityStationScreen;
import logisticspipes.network.TargetLookup;

/**
 * The state of the security station's two checkboxes.
 *
 * <p>Both travel together because they are shown together and change for the same reasons: the GUI
 * opening, or either one being ticked. They used to be a packet each, sent one after the other.
 */
public record SecurityStationFlagsMessage(BlockPos pos, boolean allowCC, boolean autoDestroy)
        implements CustomPacketPayload {

    public static final Type<SecurityStationFlagsMessage> TYPE =
            new Type<>(LPConstants.rl("security_station_flags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityStationFlagsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SecurityStationFlagsMessage::pos,
                    ByteBufCodecs.BOOL, SecurityStationFlagsMessage::allowCC,
                    ByteBufCodecs.BOOL, SecurityStationFlagsMessage::autoDestroy,
                    SecurityStationFlagsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityStationFlagsMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (be == null) {
            return;
        }
        be.setClientCC(message.allowCC);
        be.setClientDestroy(message.autoDestroy);
        if (Minecraft.getInstance().screen instanceof SecurityStationScreen gui) {
            gui.refreshCheckBoxes();
        }
    }
}
