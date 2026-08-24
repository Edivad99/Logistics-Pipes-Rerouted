package logisticspipes.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

@Getter
@Accessors(chain = true)
public class PlayerIdentifier {

    public static final Codec<PlayerIdentifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("username").forGetter(identifier -> Optional.ofNullable(identifier.getUsername())),
        UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(identifier -> Optional.ofNullable(identifier.getId()))
    ).apply(instance, (username, id) -> PlayerIdentifier.get(username.orElse(null), id.orElse(null))));

    public static final StreamCodec<ByteBuf, PlayerIdentifier> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            identifier -> Optional.ofNullable(identifier.getUsername()),
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            identifier -> Optional.ofNullable(identifier.getId()),
            (username, id) -> PlayerIdentifier.get(username.orElse(null), id.orElse(null)));

    private static final Map<UUID, PlayerIdentifier> idBased = new HashMap<>();
    private static final Map<String, PlayerIdentifier> nameBased = new HashMap<>();
    private @Nullable String username;
    private @Nullable UUID id;

    private PlayerIdentifier(@Nullable String username, @Nullable UUID id) {
        this.username = username;
        this.id = id;
    }

    public static PlayerIdentifier get(Player player) {
        return PlayerIdentifier.get(player.getGameProfile().name(), player.getGameProfile().id());
    }

    public static PlayerIdentifier get(@Nullable String username, @Nullable UUID id) {
        if (PlayerIdentifier.idBased.containsKey(id)) {
            return PlayerIdentifier.idBased.get(id).setUsername(username);
        }
        if (PlayerIdentifier.nameBased.containsKey(username)) {
            return PlayerIdentifier.nameBased.get(username);
        }
        if (id != null) {
            PlayerIdentifier ident = new PlayerIdentifier(username, id);
            PlayerIdentifier.idBased.put(id, ident);
            return ident;
        }
        if (username == null) {
            throw new IllegalStateException("Username and id cannot be both null");
        }
        PlayerIdentifier ident = new PlayerIdentifier(username, null);
        PlayerIdentifier.nameBased.put(username, ident);
        return ident;
    }

    /**
     * Reads the identifier stored under {@code key}.
     *
     * <p>The two fields used to be flattened into the parent tag as {@code <prefix>_id} and
     * {@code <prefix>_name}. A {@link ValueInput} addresses children by key rather than by string
     * concatenation, so they are nested under one child now.</p>
     */
    public static PlayerIdentifier deserialize(ValueInput input, String key) {
        ValueInput child = input.childOrEmpty(key);
        UUID id = child.getString("id").map(raw -> {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }).orElse(null);
        String username = child.getStringOr("name", "");
        return PlayerIdentifier.get(username, id);
    }

    public static PlayerIdentifier convertFromUsername(String name) {
        return PlayerIdentifier.get(name, null);
    }

    public void serialize(ValueOutput output, String key) {
        ValueOutput child = output.child(key);
        if (id != null) {
            child.putString("id", id.toString());
        }
        if (username != null) {
            child.putString("name", username);
        }
    }

    public PlayerIdentifier setUsername(@Nullable String string) {
        if (username == null || username.isEmpty()) {
            username = string;
        }
        return this;
    }

    public PlayerIdentifier setID(UUID uuid) {
        if (id == null) {
            id = uuid;
            PlayerIdentifier.idBased.put(id, this);
        }
        return this;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerIdentifier) {
            return id.equals(((PlayerIdentifier) obj).id);
        } else {
            return false;
        }
    }
}
