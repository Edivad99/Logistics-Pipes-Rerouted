package logisticspipes.routing.channels;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NonNull;

import logisticspipes.utils.PlayerIdentifier;

@Data
public class ChannelInformation {

    public static final StreamCodec<ByteBuf, ChannelInformation> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), channel -> Optional.ofNullable(channel.getName()),
            UUIDUtil.STREAM_CODEC, ChannelInformation::getChannelIdentifier,
            PlayerIdentifier.STREAM_CODEC, ChannelInformation::getOwner,
            ByteBufCodecs.idMapper(id -> AccessRights.values()[id], AccessRights::ordinal), ChannelInformation::getRights,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), channel -> Optional.ofNullable(channel.getResponsibleSecurityID()),
            (name, identifier, owner, rights, securityId) ->
                new ChannelInformation(name.orElse(null), identifier, owner, rights, securityId.orElse(null)));

    /** Null for a channel sent as a bare reference, see {@code ChannelManager.removeChannel}. */
    private @Nullable String name;
    private final @NonNull UUID channelIdentifier;
    private @NonNull PlayerIdentifier owner;
    private @NonNull AccessRights rights;
    private @Nullable UUID responsibleSecurityID;

    public ChannelInformation(@Nullable String name, UUID identifier, PlayerIdentifier owner,
        ChannelInformation.AccessRights rights, @Nullable UUID securityId) {
        // Checked here as well as on the generated setters: a channel that loses its identifier or
        // its owner fails far from here otherwise, and one of the two ways in is the network.
        this.name = name;
        this.channelIdentifier = Objects.requireNonNull(identifier, "channelIdentifier");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.rights = Objects.requireNonNull(rights, "rights");
        this.responsibleSecurityID = securityId;
    }

    public ChannelInformation(CompoundTag tag) {
        this(
            tag.contains("name") ? tag.getString("name") : null,
            UUID.fromString(tag.getString("channelIdentifier")),
            PlayerIdentifier.readFromNBT(tag, "owner"),
            AccessRights.values()[tag.getInt("rights")],
            readOptionalUUID(tag, "responsibleSecurityID"));
    }

    @Nullable
    private static UUID readOptionalUUID(CompoundTag tag, String key) {
        return tag.contains(key) ? UUID.fromString(tag.getString(key)) : null;
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        // An absent key rather than an empty string, so that a null name survives the round trip
        // instead of coming back as "" -- and so that putString is never handed a null.
        if (name != null) {
            tag.putString("name", name);
        }
        tag.putString("channelIdentifier", channelIdentifier.toString());
        owner.writeToNBT(tag, "owner");
        tag.putInt("rights", rights.ordinal());
        if (responsibleSecurityID != null) {
            tag.putString("responsibleSecurityID", responsibleSecurityID.toString());
        }
        return tag;
    }

    public enum AccessRights {
        PRIVATE,
        SECURED,
        PUBLIC
    }
}
