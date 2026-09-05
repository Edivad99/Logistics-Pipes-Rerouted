package logisticspipes.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


public class ItemPipeManager extends LogisticsItem {

    private static final Direction[] FACING_CYCLE = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
        Direction.UP, Direction.DOWN };
    private static final Direction[] HORIZONTAL_CYCLE = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
        Direction.WEST };

    public ItemPipeManager(Properties properties) {
        super(properties);
    }

    private static Direction rotateDirection(Direction d, boolean reverse) {
        int idx = 0;
        for (int i = 0; i < FACING_CYCLE.length; i++) {
            if (FACING_CYCLE[i] == d) {
                idx = i;
                break;
            }
        }
        int next = reverse ? (idx + FACING_CYCLE.length - 1) % FACING_CYCLE.length : (idx + 1) % FACING_CYCLE.length;
        return FACING_CYCLE[next];
    }

    private static Direction rotateHorizontal(Direction d, boolean reverse) {
        int idx = 0;
        for (int i = 0; i < HORIZONTAL_CYCLE.length; i++) {
            if (HORIZONTAL_CYCLE[i] == d) {
                idx = i;
                break;
            }
        }
        int next = reverse ?
            (idx + HORIZONTAL_CYCLE.length - 1) % HORIZONTAL_CYCLE.length :
            (idx + 1) % HORIZONTAL_CYCLE.length;
        return HORIZONTAL_CYCLE[next];
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    /**
     * Right-clicking a non-pipe block with the Pipe Manager rotates it clockwise,
     * cycling through FACING / HORIZONTAL_FACING / AXIS properties as available.
     * LP pipe tiles are handled by blockActivated() upstream; this handles everything else.
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);

        // Try FACING first, then HORIZONTAL_FACING, then AXIS
        BlockState rotated = null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction current = state.getValue(BlockStateProperties.FACING);
            Direction next = rotateDirection(current, player.isCrouching());
            rotated = state.setValue(BlockStateProperties.FACING, next);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction next = rotateHorizontal(current, player.isCrouching());
            rotated = state.setValue(BlockStateProperties.HORIZONTAL_FACING, next);
        } else {
            // Fall back to Minecraft's built-in rotate() for axis-based blocks etc.
            BlockState attempt = state.rotate(level, pos, Rotation.CLOCKWISE_90);
            if (!attempt.equals(state)) {
                rotated = attempt;
            }
        }

        if (rotated != null && !rotated.equals(state)) {
            level.setBlock(pos, rotated, 3);
            level.levelEvent(player, 1000, pos, 0);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
