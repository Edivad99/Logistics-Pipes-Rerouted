package logisticspipes.renderer.state;

import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;

public class ConnectionMatrix {

	private int mask = 0;
	private int isBCPipeMask = 0;
	private int isTDPipeMask = 0;
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
			setTDConnected(direction, false);
		}
	}

	public boolean isBCConnected(Direction direction) {
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

	public boolean isTDConnected(Direction direction) {
		// test if the direction.ordinal()'th bit of mask is set
		return direction != null && (isTDPipeMask & (1 << direction.ordinal())) != 0;
	}

	public void setTDConnected(Direction direction, boolean value) {
		if (isTDConnected(direction) != value) {
			// invert the direction.ordinal()'th bit of mask
			isTDPipeMask ^= 1 << direction.ordinal();
			dirty = true;
		}
	}

	/**
	 * Return a mask representing the connectivity for all sides.
	 *
	 * @return mask in Direction order, least significant bit = first entry
	 */
	public int getMask() {
		return mask;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clean() {
		dirty = false;
	}

	/** What the client needs: three side masks, one bit per direction. */
	public record Wire(int mask, int bcMask, int tdMask) {

		public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BYTE, wire -> (byte) wire.mask,
				ByteBufCodecs.BYTE, wire -> (byte) wire.bcMask,
				ByteBufCodecs.BYTE, wire -> (byte) wire.tdMask,
				(mask, bcMask, tdMask) -> new Wire(mask, bcMask, tdMask));
	}

	public Wire snapshot() {
		return new Wire(mask, isBCPipeMask, isTDPipeMask);
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
		if (wire.tdMask() != isTDPipeMask) {
			isTDPipeMask = wire.tdMask();
			dirty = true;
		}
	}

}
