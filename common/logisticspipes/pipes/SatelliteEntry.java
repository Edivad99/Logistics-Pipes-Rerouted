package logisticspipes.pipes;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A satellite as offered to the player in the selection popup.
 */
public record SatelliteEntry(String name, UUID routerId) {

    public static final StreamCodec<RegistryFriendlyByteBuf, SatelliteEntry> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SatelliteEntry::name,
                    UUIDUtil.STREAM_CODEC, SatelliteEntry::routerId,
                    SatelliteEntry::new);
}
