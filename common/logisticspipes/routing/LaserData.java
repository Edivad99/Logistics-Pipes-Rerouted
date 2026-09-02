package logisticspipes.routing;

import java.util.EnumSet;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * One segment of the routing beam drawn between two pipes.
 *
 * <p>Mutable on purpose: the builder walks the network adding segments and then merges the
 * collinear ones in place.
 */
public class LaserData {

    public static final StreamCodec<RegistryFriendlyByteBuf, LaserData> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, LaserData::getPos,
                    Direction.STREAM_CODEC, LaserData::getDir,
                    ByteBufCodecs.collection(
                            size -> EnumSet.noneOf(PipeRoutingConnectionType.class),
                            NeoForgeStreamCodecs.enumCodec(PipeRoutingConnectionType.class)),
                    LaserData::getConnectionType,
                    ByteBufCodecs.BOOL, LaserData::isFinalPipe,
                    ByteBufCodecs.BOOL, LaserData::isStartPipe,
                    ByteBufCodecs.VAR_INT, LaserData::getLength,
                    LaserData::new);

    private final BlockPos pos;
    private final Direction dir;
    private final EnumSet<PipeRoutingConnectionType> connectionType;

    private boolean finalPipe = true;
    private boolean startPipe = false;
    private int length = 1;

    public LaserData(BlockPos pos, Direction dir, EnumSet<PipeRoutingConnectionType> connectionType) {
        this.pos = pos;
        this.dir = dir;
        this.connectionType = connectionType;
    }

    private LaserData(BlockPos pos, Direction dir, EnumSet<PipeRoutingConnectionType> connectionType,
            boolean finalPipe, boolean startPipe, int length) {
        this(pos, dir, connectionType);
        this.finalPipe = finalPipe;
        this.startPipe = startPipe;
        this.length = length;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getDir() {
        return dir;
    }

    public EnumSet<PipeRoutingConnectionType> getConnectionType() {
        return connectionType;
    }

    public boolean isFinalPipe() {
        return finalPipe;
    }

    public LaserData setFinalPipe(boolean finalPipe) {
        this.finalPipe = finalPipe;
        return this;
    }

    public boolean isStartPipe() {
        return startPipe;
    }

    public LaserData setStartPipe(boolean startPipe) {
        this.startPipe = startPipe;
        return this;
    }

    public int getLength() {
        return length;
    }

    public LaserData setLength(int length) {
        this.length = length;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LaserData other)) {
            return false;
        }
        return pos.equals(other.pos)
                && dir == other.dir
                && connectionType.equals(other.connectionType)
                && finalPipe == other.finalPipe
                && startPipe == other.startPipe
                && length == other.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, dir, connectionType, finalPipe, startPipe, length);
    }

    @Override
    public String toString() {
        return "LaserData(pos=" + pos + ", dir=" + dir + ", connectionType=" + connectionType
                + ", finalPipe=" + finalPipe + ", startPipe=" + startPipe + ", length=" + length + ")";
    }
}
