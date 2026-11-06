package logisticspipes.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The rotation an {@link logisticspipes.interfaces.ITubeOrientation} applies to a multiblock's
 * offsets, recorded instead of performed.
 *
 * <p>The orientations describe themselves by calling {@link #rotateLeft()} and friends, which used
 * to mutate whatever position or set was handed to them. Collecting the calls into a single
 * transform lets the positions stay immutable, and lets one orientation be applied to any number of
 * them without being asked twice.
 *
 * <p>Only the horizontal plane turns; the height is carried through untouched.
 */
public final class PositionRotation implements IPositionRotateble {

    /**
     * (x, z) becomes (xx * x + xz * z, zx * x + zz * z).
     */
    private int xx = 1;
    private int xz = 0;
    private int zx = 0;
    private int zz = 1;

    @Override
    public void rotateLeft() {
        compose(0, 1, -1, 0);
    }

    @Override
    public void rotateRight() {
        compose(0, -1, 1, 0);
    }

    @Override
    public void mirrorX() {
        compose(-1, 0, 0, 1);
    }

    @Override
    public void mirrorZ() {
        compose(1, 0, 0, -1);
    }

    /**
     * Applies one more step on top of what is already recorded.
     */
    private void compose(int newXx, int newXz, int newZx, int newZz) {
        int composedXx = newXx * xx + newXz * zx;
        int composedXz = newXx * xz + newXz * zz;
        int composedZx = newZx * xx + newZz * zx;
        int composedZz = newZx * xz + newZz * zz;
        xx = composedXx;
        xz = composedXz;
        zx = composedZx;
        zz = composedZz;
    }

    public BlockPos apply(BlockPos pos) {
        return new BlockPos(xx * pos.getX() + xz * pos.getZ(), pos.getY(), zx * pos.getX() + zz * pos.getZ());
    }

    public Vec3 apply(Vec3 pos) {
        return new Vec3(xx * pos.x + xz * pos.z, pos.y, zx * pos.x + zz * pos.z);
    }
}
