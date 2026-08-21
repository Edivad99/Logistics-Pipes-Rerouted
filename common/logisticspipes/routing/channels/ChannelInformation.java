package logisticspipes.routing.channels;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NonNull;

import logisticspipes.utils.PlayerIdentifier;

@Data
public class ChannelInformation {

    public static final Codec<ChannelInformation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("name").forGetter(channel -> Optional.ofNullable(channel.getName())),
        UUIDUtil.STRING_CODEC.fieldOf("channelIdentifier").forGetter(ChannelInformation::getChannelIdentifier),
        PlayerIdentifier.CODEC.fieldOf("owner").forGetter(ChannelInformation::getOwner),
        AccessRights.CODEC.fieldOf("rights").forGetter(ChannelInformation::getRights),
        UUIDUtil.STRING_CODEC.optionalFieldOf("responsibleSecurityID")
            .forGetter(channel -> Optional.ofNullable(channel.getResponsibleSecurityID()))
    ).apply(instance, (name, identifier, owner, rights, securityId) ->
        new ChannelInformation(name.orElse(null), identifier, owner, rights, securityId.orElse(null))));

    public static final StreamCodec<ByteBuf, ChannelInformation> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), channel -> Optional.ofNullable(channel.getName()),
            UUIDUtil.STREAM_CODEC, ChannelInformation::getChannelIdentifier,
            PlayerIdentifier.STREAM_CODEC, ChannelInformation::getOwner,
            ByteBufCodecs.idMapper(id -> AccessRights.values()[id], AccessRights::ordinal),
            ChannelInformation::getRights,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            channel -> Optional.ofNullable(channel.getResponsibleSecurityID()),
            (name, identifier, owner, rights, securityId) ->
                new ChannelInformation(name.orElse(null), identifier, owner, rights, securityId.orElse(null)));
    private final @NonNull UUID channelIdentifier;
    /**
     * Null for a channel sent as a bare reference, see {@code ChannelManager.removeChannel}.
     */
    private @Nullable String name;
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

    public enum AccessRights implements StringRepresentable {
        PRIVATE,
        SECURED,
        PUBLIC;

        public static final Codec<AccessRights> CODEC = StringRepresentable.fromEnum(AccessRights::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
