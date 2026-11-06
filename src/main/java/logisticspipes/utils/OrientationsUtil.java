package logisticspipes.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

public class OrientationsUtil {

    @Nullable
    public static Direction getOrientation(BlockEntity from, BlockEntity to) {
        final BlockPos pipe = from.getBlockPos();
        final BlockPos other = to.getBlockPos();
        if (pipe.getZ() == other.getZ()) {
            if (pipe.getY() == other.getY()) {
                if (pipe.getX() < other.getX()) {
                    return Direction.EAST;
                } else if (pipe.getX() > other.getX()) {
                    return Direction.WEST;
                }
            }
        }
        if (pipe.getX() == other.getX()) {
            if (pipe.getZ() == other.getZ()) {
                if (pipe.getY() < other.getY()) {
                    return Direction.UP;
                } else if (pipe.getY() > other.getY()) {
                    return Direction.DOWN;
                }
            }
        }
        if (pipe.getX() == other.getX()) {
            if (pipe.getY() == other.getY()) {
                if (pipe.getZ() < other.getZ()) {
                    return Direction.SOUTH;
                } else if (pipe.getZ() > other.getZ()) {
                    return Direction.NORTH;
                }
            }
        }
        return null;
    }

}
