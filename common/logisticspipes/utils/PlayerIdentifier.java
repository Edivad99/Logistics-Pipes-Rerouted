package logisticspipes.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class PlayerIdentifier {

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
        return PlayerIdentifier.get(player.getGameProfile().getName(), player.getGameProfile().getId());
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

    public static PlayerIdentifier readFromNBT(CompoundTag nbt, String prefix) {
        UUID id = null;
        if (nbt.contains(prefix + "_id")) {
            String tmp = nbt.getString(prefix + "_id");
            try {
                id = UUID.fromString(tmp);
            } catch (Exception ignored) {
            }
        }
        String username = nbt.getString(prefix + "_name");
        return PlayerIdentifier.get(username, id);
    }

    public static PlayerIdentifier convertFromUsername(String name) {
        return PlayerIdentifier.get(name, null);
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        if (id != null) {
            nbt.putString(prefix + "_id", id.toString());
        }
        nbt.putString(prefix + "_name", username);
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
