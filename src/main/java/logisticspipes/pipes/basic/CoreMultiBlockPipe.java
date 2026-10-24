package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.PositionRotation;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsGenericSubMultiBlockBlockEntity;

public abstract class CoreMultiBlockPipe extends CoreUnroutedPipe {

    /** Two shared sub-block types that may sit in the same cell, in either order. */
    private record ShareCombination(SubBlockTypeForShare first, SubBlockTypeForShare second) {}

    private static final List<ShareCombination> allowedCombinations = List.of(
        new ShareCombination(SubBlockTypeForShare.S_CURVE_A, SubBlockTypeForShare.S_CURVE_B),
        new ShareCombination(SubBlockTypeForShare.S_CURVE_A, SubBlockTypeForShare.S_CURVE_A),
        new ShareCombination(SubBlockTypeForShare.CURVE_OUT_A, SubBlockTypeForShare.CURVE_INNER_A),
        new ShareCombination(SubBlockTypeForShare.CURVE_OUT_B, SubBlockTypeForShare.CURVE_INNER_B),
        new ShareCombination(SubBlockTypeForShare.CURVE_OUT_A, SubBlockTypeForShare.S_CURVE_A),
        new ShareCombination(SubBlockTypeForShare.CURVE_OUT_B, SubBlockTypeForShare.S_CURVE_A),
        new ShareCombination(SubBlockTypeForShare.GAIN_A, SubBlockTypeForShare.GAIN_B),
        new ShareCombination(SubBlockTypeForShare.GAIN_A, SubBlockTypeForShare.GAIN_A)
    );

    /**
     * Block-local shapes of the cells this pipe covers. Like the collision boxes they are built
     * from, they never change once the pipe is placed. Collision runs on worker threads too.
     */
    private final Map<BlockPos, VoxelShape> cellShapes = new ConcurrentHashMap<>();
    /**
     * The main block's cell shape joined with the pipe center, kept for the same reason.
     */
    @Getter
    @Setter
    private volatile @Nullable VoxelShape mainShape;

    public CoreMultiBlockPipe(PipeMultiBlockTransportLogistics transport, Item item) {
        super(transport, item);
    }

    public VoxelShape getCellShape(BlockPos cell, BiFunction<CoreMultiBlockPipe, BlockPos, VoxelShape> builder) {
        VoxelShape shape = cellShapes.get(cell);
        if (shape == null) {
            shape = builder.apply(this, cell);
            // Empty can also mean the orientation has not arrived yet, so it is asked again.
            if (!shape.isEmpty()) {
                // Collision passes a mutable position.
                cellShapes.put(cell.immutable(), shape);
            }
        }
        return shape;
    }

    public static boolean canShare(List<SubBlockTypeForShare> list, @Nullable SubBlockTypeForShare toAdd) {
        if (toAdd == SubBlockTypeForShare.NON_SHARE) {
            return false;
        }
        if (toAdd == null) {
            return false;
        }
        if (list.size() > 1) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }
        SubBlockTypeForShare contained = list.getFirst();
        if (contained == SubBlockTypeForShare.NON_SHARE) {
            return false;
        }
        for (ShareCombination allowed : allowedCombinations) {
            if (allowed.first() == contained && allowed.second() == toAdd
                || allowed.second() == contained && allowed.first() == toAdd) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this tube fits at {@code placeAt} in {@code orientation}: every block it covers is
     * free, or held by a tube it may share that block with. Placement and the ghost preview both
     * ask this, so the preview never shows a tube that will not go down.
     *
     * @param mayUse whether the player may build at a position; the preview, which only draws,
     *               passes one that always agrees
     */
    public boolean fitsAt(Level level, BlockPos placeAt, ITubeOrientation orientation, Predicate<BlockPos> mayUse) {
        PositionRotation rotation = new PositionRotation();
        orientation.rotatePositions(rotation);
        List<SubBlock> blocks = new ArrayList<>();
        blocks.add(new SubBlock(BlockPos.ZERO, SubBlockTypeForShare.NON_SHARE));
        getSubBlocks().stream().map(sub -> sub.rotated(rotation)).forEach(blocks::add);

        // The share types only say which half of a block each tube runs through, and two
        // S-curves crossing in an X pair them up exactly like two running side by side. What
        // tells them apart is how much they overlap: tubes alongside each other share at most
        // two blocks, tubes crossing share their whole footprint.
        Map<BlockPos, Integer> sharedWith = new HashMap<>();
        for (SubBlock block : blocks) {
            BlockPos target = block.at(placeAt);
            if (mayUse.test(target) && level.isEmptyBlock(target)) {
                continue;
            }
            if (!(level.getBlockEntity(target) instanceof LogisticsGenericSubMultiBlockBlockEntity subTile)
                || !canShare(subTile.getSubTypes(), block.type())) {
                return false;
            }
            subTile.getConnectedMainPipes()
                .forEach(main -> sharedWith.merge(main.getBlockPos(), 1, Integer::sum));
        }
        return sharedWith.values().stream().allMatch(shared -> shared <= 2);
    }

    @Override
    public boolean isMultiBlock() {
        return true;
    }

    /**
     * North Orientated
     *
     * @return Relative Positions
     */
    public abstract List<SubBlock> getSubBlocks();

    public abstract List<SubBlock> getRotatedSubBlocks();

    public abstract void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB area);

    public abstract AABB getCompleteBox();

    @Nullable
    public abstract ITubeOrientation getTubeOrientation(Player player, int xPos, int zPos);

    public abstract float getPipeLength();

    public double getDistanceWeight() {
        return 1.0 / 8.0;
    }

    public float getYawDiff(LPTravelingItem item) {
        return (float) (getItemRenderYaw(getPipeLength(), item) - getItemRenderYaw(0.0F, item));
    }

    public abstract @Nullable Direction getExitForInput(Direction comingFrom);

    @Nullable
    public abstract BlockEntity getConnectedEndTile(Direction output);

    @Override
    public abstract boolean actAsNormalPipe();

    @Override
    public boolean canPipeConnect(BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof LogisticsGenericSubMultiBlockBlockEntity) {
            return true;
        }
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity genericPipeBlockEntity) {
            if (genericPipeBlockEntity.pipe.isMultiBlock()) {
                return true;
            }
        }
        return super.canPipeConnect(blockEntity, side);
    }

    public enum SubBlockTypeForShare {
        NON_SHARE,
        S_CURVE_A,
        S_CURVE_B,
        CURVE_OUT_A,
        CURVE_INNER_A,
        CURVE_OUT_B,
        CURVE_INNER_B,
        GAIN_A,
        GAIN_B
    }

    /**
     * One block of a multiblock pipe: where it sits relative to the main block, and what it is
     * willing to share that spot with.
     */
    public record SubBlock(BlockPos offset, SubBlockTypeForShare type) {

        public SubBlock rotated(PositionRotation rotation) {
            return new SubBlock(rotation.apply(offset), type);
        }

        /**
         * Where this block goes when the pipe is placed at {@code origin}.
         */
        public BlockPos at(BlockPos origin) {
            return origin.offset(offset);
        }
    }

}
