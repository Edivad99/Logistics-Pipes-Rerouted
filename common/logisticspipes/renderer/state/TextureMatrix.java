package logisticspipes.renderer.state;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;

import lombok.Getter;

import logisticspipes.LPConfigs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;

public class TextureMatrix {

	//Old Pipe Renderer
	private final int[] iconIndexes = new int[7];

	//New Pipe Renderer
	@Getter
	private int textureIndex;
	@Getter
	private boolean isRouted;
	private boolean[] isRoutedInDir = new boolean[6];
	private boolean[] isSubPowerInDir = new boolean[6];
	@Getter
	private boolean hasPowerUpgrade;
	@Getter
	private boolean hasPower;
	@Getter
	private boolean isFluid;
	@Getter
	private Direction pointedOrientation;

	private boolean dirty = true;

	public int getTextureIndex(Direction direction) {
		return iconIndexes[direction.ordinal()];
	}

	public void setIconIndex(Direction direction, int value) {
		if (iconIndexes[direction.ordinal()] != value) {
			iconIndexes[direction.ordinal()] = value;
			dirty = true;
		}
	}

	public void refreshStates(CoreUnroutedPipe pipe) {
		if (textureIndex != pipe.getTextureIndex()) {
			dirty = true;
		}
		textureIndex = pipe.getTextureIndex();
		if (isRouted != pipe.isRoutedPipe()) {
			dirty = true;
		}
		isRouted = pipe.isRoutedPipe();
		if (isRouted) {
			CoreRoutedPipe cPipe = (CoreRoutedPipe) pipe;
			for (int i = 0; i < 6; i++) {
				if (isRoutedInDir[i] != cPipe.getRouter().isRoutedExit(Direction.from3DDataValue(i))) {
					dirty = true;
				}
				isRoutedInDir[i] = cPipe.getRouter().isRoutedExit(Direction.from3DDataValue(i));
			}
			for (int i = 0; i < 6; i++) {
				if (isSubPowerInDir[i] != cPipe.getRouter().isSubPoweredExit(Direction.from3DDataValue(i))) {
					dirty = true;
				}
				isSubPowerInDir[i] = cPipe.getRouter().isSubPoweredExit(Direction.from3DDataValue(i));
			}
			if (hasPowerUpgrade != (cPipe.getUpgradeManager().hasRFPowerSupplierUpgrade() || cPipe.getUpgradeManager().getIC2PowerLevel() > 0)) {
				dirty = true;
			}
			hasPowerUpgrade = cPipe.getUpgradeManager().hasRFPowerSupplierUpgrade() || cPipe.getUpgradeManager().getIC2PowerLevel() > 0;
			if (hasPower != (cPipe.textureBufferPowered || LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean())) {
				dirty = true;
			}
			hasPower = cPipe.textureBufferPowered || LPConfigs.COMMON.LOGISTICS_POWER_USAGE_DISABLED.getAsBoolean();
			if (isFluid != cPipe.isFluidPipe()) {
				dirty = true;
			}
			isFluid = cPipe.isFluidPipe();
			if (pointedOrientation != cPipe.getPointedOrientation()) {
				dirty = true;
			}
			pointedOrientation = cPipe.getPointedOrientation();
		} else {
			isRoutedInDir = new boolean[6];
		}
	}

	/** Item-renderer entry: only sets textureIndex (no router/world access). */
	public void refreshStatesForItem(CoreUnroutedPipe pipe) {
		textureIndex = pipe.getTextureIndex();
		isRouted = pipe.isRoutedPipe();
		isRoutedInDir = new boolean[6];
		isSubPowerInDir = new boolean[6];
		hasPowerUpgrade = false;
		hasPower = true;
		dirty = true;
	}

	public boolean isRoutedInDir(Direction dir) {
		if (dir == null) {
			return false;
		}
		return isRoutedInDir[dir.ordinal()];
	}

	public boolean isSubPowerInDir(Direction dir) {
		if (dir == null) {
			return false;
		}
		return isSubPowerInDir[dir.ordinal()];
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clean() {
		dirty = false;
	}

	/**
	 * What the client needs to pick textures. The two per-side flag arrays travel as bit masks,
	 * the same shape the connection masks already use.
	 */
	public record Wire(byte[] iconIndexes, int textureIndex, boolean routed, int routedMask,
			int subPowerMask, boolean powerUpgrade, boolean power, boolean fluid,
			Optional<Direction> pointedOrientation) {

		public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BYTE_ARRAY, Wire::iconIndexes,
				ByteBufCodecs.VAR_INT, Wire::textureIndex,
				ByteBufCodecs.BOOL, Wire::routed,
				ByteBufCodecs.BYTE, wire -> (byte) wire.routedMask,
				ByteBufCodecs.BYTE, wire -> (byte) wire.subPowerMask,
				ByteBufCodecs.BOOL, Wire::powerUpgrade,
				ByteBufCodecs.BOOL, Wire::power,
				ByteBufCodecs.BOOL, Wire::fluid,
				ByteBufCodecs.optional(Direction.STREAM_CODEC.cast()), Wire::pointedOrientation,
				(icons, index, routed, routedMask, subPowerMask, upgrade, power, fluid, pointed) ->
						new Wire(icons, index, routed, routedMask, subPowerMask, upgrade, power, fluid, pointed));
	}

	private static int maskOf(boolean[] flags) {
		int mask = 0;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i]) {
				mask |= 1 << i;
			}
		}
		return mask;
	}

	private static boolean[] flagsOf(int mask, int length) {
		boolean[] flags = new boolean[length];
		for (int i = 0; i < length; i++) {
			flags[i] = (mask & (1 << i)) != 0;
		}
		return flags;
	}

	public Wire snapshot() {
		byte[] icons = new byte[iconIndexes.length];
		for (int i = 0; i < iconIndexes.length; i++) {
			icons[i] = (byte) iconIndexes[i];
		}
		return new Wire(icons, textureIndex, isRouted, maskOf(isRoutedInDir), maskOf(isSubPowerInDir),
				hasPowerUpgrade, hasPower, isFluid, Optional.ofNullable(pointedOrientation));
	}

	/**
	 * Only the icon indexes and the texture index dirty the matrix, which is what the old reader
	 * did: the rest is read by the renderer every frame and does not invalidate anything.
	 */
	public void apply(Wire wire) {
		for (int i = 0; i < iconIndexes.length && i < wire.iconIndexes().length; i++) {
			if (iconIndexes[i] != wire.iconIndexes()[i]) {
				iconIndexes[i] = wire.iconIndexes()[i];
				dirty = true;
			}
		}
		if (wire.textureIndex() != textureIndex) {
			textureIndex = wire.textureIndex();
			dirty = true;
		}
		isRouted = wire.routed();
		isRoutedInDir = flagsOf(wire.routedMask(), isRoutedInDir.length);
		isSubPowerInDir = flagsOf(wire.subPowerMask(), isSubPowerInDir.length);
		hasPowerUpgrade = wire.powerUpgrade();
		hasPower = wire.power();
		isFluid = wire.fluid();
		pointedOrientation = wire.pointedOrientation().orElse(null);
	}
}
