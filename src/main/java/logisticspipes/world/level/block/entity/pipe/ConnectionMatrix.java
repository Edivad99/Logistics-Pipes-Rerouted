package logisticspipes.world.level.block.entity.pipe;

import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class ConnectionMatrix {

    private int mask = 0;
    private int isBCPipeMask = 0;
    @Getter
    private boolean dirty = false;

    public boolean isConnected(Direction direction) {
        // test if the direction.ordinal()'th bit of mask is set
        return (mask & (1 << direction.ordinal())) != 0;
    }

    public void setConnected(Direction direction, boolean value) {
        if (isConnected(direction) != value) {
            // invert the direction.ordinal()'th bit of mask
            mask ^= 1 << direction.ordinal();
            dirty = true;
        }
        if (!value) {
            setBCConnected(direction, false);
        }
    }

    public boolean isBCConnected(@Nullable Direction direction) {
        // test if the direction.ordinal()'th bit of mask is set
        return direction != null && (isBCPipeMask & (1 << direction.ordinal())) != 0;
    }

    public void setBCConnected(Direction direction, boolean value) {
        if (isBCConnected(direction) != value) {
            // invert the direction.ordinal()'th bit of mask
            isBCPipeMask ^= 1 << direction.ordinal();
            dirty = true;
        }
    }


    public void clean() {
        dirty = false;
    }

    /** What the client needs: two side masks, one bit per direction. */
    public record Wire(int mask, int bcMask) {

        public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, wire -> (byte) wire.mask,
                ByteBufCodecs.BYTE, wire -> (byte) wire.bcMask,
                (mask, bcMask) -> new Wire(mask, bcMask));
    }

    public Wire snapshot() {
        return new Wire(mask, isBCPipeMask);
    }

    /**
     * Takes what arrived, and marks itself dirty only for what actually changed -- the render
     * cache is rebuilt from that flag, so a state packet that says nothing new must cost nothing.
     */
    public void apply(Wire wire) {
        if (wire.mask() != mask) {
            mask = wire.mask();
            dirty = true;
        }
        if (wire.bcMask() != isBCPipeMask) {
            isBCPipeMask = wire.bcMask();
            dirty = true;
        }
    }
}
