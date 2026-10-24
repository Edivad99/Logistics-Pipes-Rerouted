package logisticspipes.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.netty.buffer.ByteBuf;

/**
 * What one player is allowed to do at a security station.
 *
 * <p>Immutable, so the shared {@link #NONE} and {@link #ALL} can be handed out freely; a change is
 * a new value from {@link #with}.
 */
public record SecurityPermissions(Set<SecurityPermission> granted) {

    public static final SecurityPermissions NONE = new SecurityPermissions(Set.of());
    public static final SecurityPermissions ALL =
        new SecurityPermissions(EnumSet.allOf(SecurityPermission.class));

    /** As a bit per permission; the order only has to agree between client and server. */
    public static final StreamCodec<ByteBuf, SecurityPermissions> STREAM_CODEC =
        ByteBufCodecs.VAR_INT.map(SecurityPermissions::fromBits, SecurityPermissions::toBits);

    public SecurityPermissions {
        final EnumSet<SecurityPermission> copy = EnumSet.noneOf(SecurityPermission.class);
        copy.addAll(granted);
        granted = Collections.unmodifiableSet(copy);
    }

    public boolean allows(SecurityPermission permission) {
        return granted.contains(permission);
    }

    public SecurityPermissions with(SecurityPermission permission, boolean allowed) {
        final EnumSet<SecurityPermission> changed = EnumSet.noneOf(SecurityPermission.class);
        changed.addAll(granted);
        if (allowed) {
            changed.add(permission);
        } else {
            changed.remove(permission);
        }
        return new SecurityPermissions(changed);
    }

    public static SecurityPermissions load(ValueInput input) {
        final EnumSet<SecurityPermission> granted = EnumSet.noneOf(SecurityPermission.class);
        for (SecurityPermission permission : SecurityPermission.values()) {
            if (input.getBooleanOr(permission.key, false)) {
                granted.add(permission);
            }
        }
        return new SecurityPermissions(granted);
    }

    public void save(ValueOutput output) {
        for (SecurityPermission permission : SecurityPermission.values()) {
            output.putBoolean(permission.key, allows(permission));
        }
    }

    private static SecurityPermissions fromBits(int bits) {
        final EnumSet<SecurityPermission> granted = EnumSet.noneOf(SecurityPermission.class);
        for (SecurityPermission permission : SecurityPermission.values()) {
            if ((bits & 1 << permission.ordinal()) != 0) {
                granted.add(permission);
            }
        }
        return new SecurityPermissions(granted);
    }

    private int toBits() {
        int bits = 0;
        for (SecurityPermission permission : granted) {
            bits |= 1 << permission.ordinal();
        }
        return bits;
    }
}
