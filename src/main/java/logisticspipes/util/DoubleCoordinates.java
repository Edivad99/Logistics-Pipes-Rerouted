package logisticspipes.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.utils.IPositionRotateble;

/**
 * A position held as three doubles.
 *
 * <p>Everything that builds one gives it whole numbers: routers hand over their block coordinates,
 * {@link CoordinateUtils} steps by a {@link net.minecraft.core.Direction}, and every reader either
 * truncates back to ints or asks for {@link #getBlockPos()}. The one method that produced a
 * fractional value, {@link #center()}, is reachable only from a branch that no longer has any
 * handlers registered.
 *
 * <p>So this is a {@link net.minecraft.core.BlockPos} in a heavier shape: 24 bytes instead of a
 * packed long, no equality with vanilla positions, and its own serialization.
 *
 * @deprecated use {@link net.minecraft.core.BlockPos}. Replacing this class means changing the
 *         signatures it appears in -- {@code IRouter.getLPPosition}, {@code
 *         IOrderInfoProvider.getTargetPosition} and the {@code ICoordinates} hierarchy -- so it is
 *         a job of its own rather than something to do in passing.
 */
@Deprecated(forRemoval = true)
@Data
public class DoubleCoordinates implements IPositionRotateble, ICoordinates, LPSerializable {

    private double xCoord;
    private double yCoord;
    private double zCoord;

    public static final StreamCodec<ByteBuf, DoubleCoordinates> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, DoubleCoordinates::getXDouble,
            ByteBufCodecs.DOUBLE, DoubleCoordinates::getYDouble,
            ByteBufCodecs.DOUBLE, DoubleCoordinates::getZDouble,
            DoubleCoordinates::new);

    public DoubleCoordinates() {
        setXCoord(0.0);
        setYCoord(0.0);
        setZCoord(0.0);
    }

    public DoubleCoordinates(double xCoord, double yCoord, double zCoord) {
        setXCoord(xCoord);
        setYCoord(yCoord);
        setZCoord(zCoord);
    }

    public DoubleCoordinates(LPDataInput input) {
        read(input);
    }

    public DoubleCoordinates(ICoordinates coords) {
        this(coords.getXDouble(), coords.getYDouble(), coords.getZDouble());
    }

    public DoubleCoordinates(BlockEntity tile) {
        this(tile.getBlockPos());
    }

    public DoubleCoordinates(CoreUnroutedPipe pipe) {
        this(pipe.getX(), pipe.getY(), pipe.getZ());
    }

    public DoubleCoordinates(IPipeInformationProvider pipe) {
        this(pipe.getX(), pipe.getY(), pipe.getZ());
    }

    public DoubleCoordinates(Entity entity) {
        this(entity.getX(), entity.getY(), entity.getZ());
    }

    public DoubleCoordinates(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    public static DoubleCoordinates deserialize(String prefix, ValueInput input) {
        double sentinel = Double.NaN;
        double x = input.getDoubleOr(prefix + "xPos", sentinel);
        double y = input.getDoubleOr(prefix + "yPos", sentinel);
        double z = input.getDoubleOr(prefix + "zPos", sentinel);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return null;
        }
        return new DoubleCoordinates(x, y, z);
    }

    @Override
    public double getXDouble() {
        return getXCoord();
    }

    @Override
    public double getYDouble() {
        return getYCoord();
    }

    @Override
    public double getZDouble() {
        return getZCoord();
    }

    @Override
    public int getXInt() {
        return (int) getXCoord();
    }

    @Override
    public int getYInt() {
        return (int) getYCoord();
    }

    @Override
    public int getZInt() {
        return (int) getZCoord();
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int) getXCoord(), (int) getYCoord(), (int) getZCoord());
    }

    public BlockEntity getTileEntity(BlockGetter world) {
        return world.getBlockEntity(getBlockPos());
    }

    @Override
    public String toString() {
        return "(" + getXCoord() + ", " + getYCoord() + ", " + getZCoord() + ")";
    }

    /** The block these coordinates fall in, for display. Truncates, where {@link #toString} does not. */
    public String toIntBasedString() {
        return "(" + getXInt() + ", " + getYInt() + ", " + getZInt() + ")";
    }

    public BlockState getBlockState(BlockGetter world) {
        return world.getBlockState(getBlockPos());
    }

    public boolean blockExists(Level level) {
        return !level.isEmptyBlock(getBlockPos());
    }

    public double distanceTo(DoubleCoordinates targetPos) {
        return Math.sqrt(
            Math.pow(targetPos.getXCoord() - getXCoord(), 2) + Math.pow(targetPos.getYCoord() - getYCoord(), 2) + Math
                .pow(targetPos.getZCoord() - getZCoord(), 2));
    }

    /** The middle of the block these coordinates fall in. Leaves this instance alone. */
    public DoubleCoordinates center() {
        return new DoubleCoordinates(getXInt() + 0.5, getYInt() + 0.5, getZInt() + 0.5);
    }

    public void serialize(String prefix, ValueOutput output) {
        output.putDouble(prefix + "xPos", xCoord);
        output.putDouble(prefix + "yPos", yCoord);
        output.putDouble(prefix + "zPos", zCoord);
    }

    public DoubleCoordinates add(DoubleCoordinates toAdd) {
        setXCoord(getXCoord() + toAdd.getXCoord());
        setYCoord(getYCoord() + toAdd.getYCoord());
        setZCoord(getZCoord() + toAdd.getZCoord());
        return this;
    }

    public void setBlockToAir(Level level) {
        level.removeBlock(getBlockPos(), false);
    }

    @Override
    public void rotateLeft() {
        double tmp = getZCoord();
        setZCoord(-getXCoord());
        setXCoord(tmp);
    }

    @Override
    public void rotateRight() {
        double tmp = getXCoord();
        setXCoord(-getZCoord());
        setZCoord(tmp);
    }

    @Override
    public void mirrorX() {
        setXCoord(-getXCoord());
    }

    @Override
    public void mirrorZ() {
        setZCoord(-getZCoord());
    }

    public double getLength() {
        return Math.sqrt(getXDouble() * getXDouble() + getYDouble() * getYDouble() + getZDouble() * getZDouble());
    }

    @Override
    public void read(LPDataInput input) {
        xCoord = input.readDouble();
        yCoord = input.readDouble();
        zCoord = input.readDouble();
    }

    @Override
    public void write(LPDataOutput output) {
        output.writeDouble(xCoord);
        output.writeDouble(yCoord);
        output.writeDouble(zCoord);
    }
}
