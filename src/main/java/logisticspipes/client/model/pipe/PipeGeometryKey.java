package logisticspipes.client.model.pipe;

import java.util.Arrays;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.renderer.state.PipeRenderState;

/**
 * Everything about a pipe that changes its geometry, captured as one immutable value.
 *
 * <p>This is what makes the baked pipeline possible. The old renderer read the mutable
 * {@code PipeRenderState}, the pipe and the surrounding world directly while emitting
 * vertices, every frame, on the render thread. Chunk meshes are built off-thread and cached,
 * so the inputs have to be snapshotted on the main thread first and then be usable as a
 * cache key — hence the value semantics and the hand-written {@link #equals}.</p>
 *
 * <p>Two pipes with equal keys produce byte-identical geometry, which is what lets
 * {@code PipeQuadBaker} share one baked quad list across every pipe in that configuration —
 * typically a handful of distinct keys for a whole base.</p>
 */
public final class PipeGeometryKey {

    private final int connectedMask;
    private final int bcConnectedMask;
    private final int tdConnectedMask;
    private final int specialEndMask;
    private final int solidSideMask;
    private final int routedInDirMask;
    private final int subPowerInDirMask;
    private final boolean routed;
    private final boolean hasPower;
    private final boolean hasPowerUpgrade;
    private final boolean fluid;
    private final int textureIndex;
    @Nullable
    private final Direction pointedOrientation;
    /**
     * Per-face extent of the neighbouring block's collision box, driving the side scaling
     * that stretches a pipe end to meet a partial-height neighbour.
     */
    private final double[] neighbourBounds;

    private final int hash;

    private PipeGeometryKey(int connectedMask, int bcConnectedMask, int tdConnectedMask, int specialEndMask,
        int solidSideMask, int routedInDirMask, int subPowerInDirMask, boolean routed, boolean hasPower,
        boolean hasPowerUpgrade, boolean fluid, int textureIndex, @Nullable Direction pointedOrientation,
        double[] neighbourBounds) {
        this.connectedMask = connectedMask;
        this.bcConnectedMask = bcConnectedMask;
        this.tdConnectedMask = tdConnectedMask;
        this.specialEndMask = specialEndMask;
        this.solidSideMask = solidSideMask;
        this.routedInDirMask = routedInDirMask;
        this.subPowerInDirMask = subPowerInDirMask;
        this.routed = routed;
        this.hasPower = hasPower;
        this.hasPowerUpgrade = hasPowerUpgrade;
        this.fluid = fluid;
        this.textureIndex = textureIndex;
        this.pointedOrientation = pointedOrientation;
        this.neighbourBounds = neighbourBounds;
        this.hash = computeHash();
    }

    /**
     * Snapshots a placed pipe. Must be called on the main thread — it reads neighbouring
     * block states.
     */
    public static PipeGeometryKey of(LogisticsTileGenericPipe tile, CoreUnroutedPipe pipe, PipeRenderState state) {
        BlockGetter level = tile.getWorld();
        BlockPos pos = tile.getBlockPos();

        int specialEnd = 0;
        int solid = 0;
        double[] bounds = new double[6];
        for (Direction dir : Direction.values()) {
            if (pipe.hasSpecialPipeEndAt(dir)) {
                specialEnd |= 1 << dir.ordinal();
            }
            bounds[dir.ordinal()] = neighbourBound(level, pos, dir);
            if (level != null && isSolidAgainst(level, pos, dir) && !state.pipeConnectionMatrix.isConnected(dir)) {
                solid |= 1 << dir.ordinal();
            }
        }
        return build(state, specialEnd, solid, bounds);
    }

    /**
     * Snapshots a pipe with no world around it — the item form, and the placement preview.
     * Neighbour-dependent adjustments fall back to a full-cube neighbour, matching the
     * {@code pipeTile == null} branch of the original renderer.
     */
    public static PipeGeometryKey ofItem(CoreUnroutedPipe pipe, PipeRenderState state) {
        int specialEnd = 0;
        double[] bounds = new double[6];
        for (Direction dir : Direction.values()) {
            if (pipe.hasSpecialPipeEndAt(dir)) {
                specialEnd |= 1 << dir.ordinal();
            }
            bounds[dir.ordinal()] = unitCubeBound(dir);
        }
        // No world, so no mounts can be placed: solid side mask stays empty.
        return build(state, specialEnd, 0, bounds);
    }

    private static PipeGeometryKey build(PipeRenderState state, int specialEnd, int solid, double[] bounds) {
        int connected = 0;
        int bc = 0;
        int td = 0;
        int routedInDir = 0;
        int subPower = 0;
        for (Direction dir : Direction.values()) {
            int bit = 1 << dir.ordinal();
            if (state.pipeConnectionMatrix.isConnected(dir)) {
                connected |= bit;
            }
            if (state.pipeConnectionMatrix.isBCConnected(dir)) {
                bc |= bit;
            }
            if (state.pipeConnectionMatrix.isTDConnected(dir)) {
                td |= bit;
            }
            if (state.textureMatrix.isRoutedInDir(dir)) {
                routedInDir |= bit;
            }
            if (state.textureMatrix.isSubPowerInDir(dir)) {
                subPower |= bit;
            }
        }
        return new PipeGeometryKey(connected, bc, td, specialEnd, solid, routedInDir, subPower,
            state.textureMatrix.isRouted(), state.textureMatrix.isHasPower(),
            state.textureMatrix.isHasPowerUpgrade(), state.textureMatrix.isFluid(),
            state.textureMatrix.getTextureIndex(), state.textureMatrix.getPointedOrientation(), bounds);
    }

    /**
     * The neighbour's collision extent along {@code dir}, in the axis order the side scaling
     * expects. A connected side or a missing world reports the full cube.
     */
    private static double neighbourBound(@Nullable BlockGetter level, BlockPos pos, Direction dir) {
        if (level == null) {
            return unitCubeBound(dir);
        }
        BlockPos neighbour = pos.relative(dir);
        BlockState blockState = level.getBlockState(neighbour);
        VoxelShape shape = blockState.getCollisionShape(level, neighbour);
        AABB box = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        // The original packed the neighbour AABB as {minY, minZ, minX, maxY, maxZ, maxX} and
        // then indexed it with dir.ordinal()/2 + (dir.ordinal()%2 == 0 ? 3 : 0). That reduces
        // to: the min along the axis for positive faces, the max for negative ones.
        return switch (dir) {
            case DOWN -> box.maxY;
            case UP -> box.minY;
            case NORTH -> box.maxZ;
            case SOUTH -> box.minZ;
            case WEST -> box.maxX;
            case EAST -> box.minX;
        };
    }

    private static double unitCubeBound(Direction dir) {
        return dir.ordinal() % 2 == 0 ? 1.0 : 0.0;
    }

    private static boolean isSolidAgainst(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        BlockState blockState = level.getBlockState(neighbour);
        return !blockState.isAir() && blockState.isFaceSturdy(level, neighbour, dir.getOpposite());
    }

    // ─── Queries used by the baker ──────────────────────────────────────────

    public boolean isConnected(Direction dir) {
        return (connectedMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isBCConnected(Direction dir) {
        return (bcConnectedMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isTDConnected(Direction dir) {
        return (tdConnectedMask & (1 << dir.ordinal())) != 0;
    }

    public boolean hasSpecialPipeEndAt(Direction dir) {
        return (specialEndMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isSolidSide(Direction dir) {
        return (solidSideMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isRoutedInDir(Direction dir) {
        return (routedInDirMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isSubPowerInDir(Direction dir) {
        return (subPowerInDirMask & (1 << dir.ordinal())) != 0;
    }

    public boolean isRouted() {
        return routed;
    }

    public boolean hasPower() {
        return hasPower;
    }

    public boolean hasPowerUpgrade() {
        return hasPowerUpgrade;
    }

    public boolean isFluid() {
        return fluid;
    }

    public int textureIndex() {
        return textureIndex;
    }

    @Nullable
    public Direction pointedOrientation() {
        return pointedOrientation;
    }

    public double neighbourBound(Direction dir) {
        return neighbourBounds[dir.ordinal()];
    }

    /**
     * Number of faces that are connected or carry a special pipe end.
     */
    public int connectionCount() {
        return Integer.bitCount(connectedMask | specialEndMask);
    }

    // ─── Value semantics ────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PipeGeometryKey other)) {
            return false;
        }
        return connectedMask == other.connectedMask
            && bcConnectedMask == other.bcConnectedMask
            && tdConnectedMask == other.tdConnectedMask
            && specialEndMask == other.specialEndMask
            && solidSideMask == other.solidSideMask
            && routedInDirMask == other.routedInDirMask
            && subPowerInDirMask == other.subPowerInDirMask
            && routed == other.routed
            && hasPower == other.hasPower
            && hasPowerUpgrade == other.hasPowerUpgrade
            && fluid == other.fluid
            && textureIndex == other.textureIndex
            && pointedOrientation == other.pointedOrientation
            && Arrays.equals(neighbourBounds, other.neighbourBounds);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private int computeHash() {
        int result = connectedMask;
        result = 31 * result + bcConnectedMask;
        result = 31 * result + tdConnectedMask;
        result = 31 * result + specialEndMask;
        result = 31 * result + solidSideMask;
        result = 31 * result + routedInDirMask;
        result = 31 * result + subPowerInDirMask;
        result = 31 * result + Boolean.hashCode(routed);
        result = 31 * result + Boolean.hashCode(hasPower);
        result = 31 * result + Boolean.hashCode(hasPowerUpgrade);
        result = 31 * result + Boolean.hashCode(fluid);
        result = 31 * result + textureIndex;
        result = 31 * result + (pointedOrientation == null ? 0 : pointedOrientation.ordinal() + 1);
        return 31 * result + Arrays.hashCode(neighbourBounds);
    }
}
