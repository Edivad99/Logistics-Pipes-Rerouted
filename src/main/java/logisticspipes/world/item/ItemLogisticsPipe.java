package logisticspipes.world.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.utils.PositionRotation;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsGenericPipeBlock;
import logisticspipes.world.level.block.entity.LogisticsGenericSubMultiBlockBlockEntity;

/**
 * A logistics pipe Item
 */
public class ItemLogisticsPipe extends LogisticsItem {

    @Setter
    @Getter
    @Nullable
    private CoreUnroutedPipe dummyPipe;

    public ItemLogisticsPipe(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction facing = context.getClickedFace();
        Block block = LPBlocks.PIPE.get();

        BlockState iblockstate = level.getBlockState(pos);

        if (!iblockstate.canBeReplaced()) {
            pos = pos.relative(facing);
        }

        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!dummyPipe.isMultiBlock()) {
            if (player.mayUseItemAt(pos, facing, itemstack) && level.isEmptyBlock(pos)) {
                CoreUnroutedPipe pipe = LogisticsGenericPipeBlock.createPipe(this);

                if (pipe == null) {
                    LogisticsPipes.LOG.warn("Pipe failed to create during placement at {},{},{}", pos.getX(),
                        pos.getY(), pos.getZ());
                    return InteractionResult.PASS;
                }

                if (LogisticsGenericPipeBlock.placePipe(pipe, level, pos, block, null)) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(block)) {
                        //setTileEntityNBT(world, player, pos, stack);
                        block.setPlacedBy(level, pos, state, player, itemstack);

                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, itemstack);
                        }

                        BlockState newBlockState = level.getBlockState(pos);
                        SoundType soundtype = newBlockState.getBlock().getSoundType(newBlockState, level, pos, player);
                        level.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
                            (soundtype.getVolume() + 1.0F) / 2.0F,
                            soundtype.getPitch() * 0.8F);

                        itemstack.shrink(1);
                    }
                }

                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            CoreMultiBlockPipe multiPipe = (CoreMultiBlockPipe) dummyPipe;
            boolean isFreeSpace = true;
            ITubeOrientation orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());
            if (orientation == null) {
                return InteractionResult.FAIL;
            }
            BlockPos placeAt = pos.offset(orientation.getOffset());
            final PositionRotation rotation = new PositionRotation();
            orientation.rotatePositions(rotation);
            List<CoreMultiBlockPipe.SubBlock> globalPos = new ArrayList<>();
            globalPos.add(new CoreMultiBlockPipe.SubBlock(BlockPos.ZERO,
                CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
            multiPipe.getSubBlocks().stream().map(sub -> sub.rotated(rotation)).forEach(globalPos::add);

            for (CoreMultiBlockPipe.SubBlock iPos : globalPos) {
                final BlockPos target = iPos.at(placeAt);
                if (!player.mayUseItemAt(target, facing, itemstack) || !level.isEmptyBlock(target)) {
                    BlockEntity tile = level.getBlockEntity(target);
                    boolean canPlace = false;
                    if (tile instanceof LogisticsGenericSubMultiBlockBlockEntity) {
                        if (CoreMultiBlockPipe.canShare(((LogisticsGenericSubMultiBlockBlockEntity) tile).getSubTypes(),
                            iPos.type())) {
                            canPlace = true;
                        }
                    }
                    if (!canPlace) {
                        isFreeSpace = false;
                        break;
                    }
                }
            }
            if (isFreeSpace) {
                CoreUnroutedPipe pipe = LogisticsGenericPipeBlock.createPipe(this);

                if (pipe == null) {
                    LogisticsPipes.LOG.warn("Pipe failed to create during placement at {},{},{}", pos.getX(),
                        pos.getY(), pos.getZ());
                    return InteractionResult.SUCCESS;
                }

                if (LogisticsGenericPipeBlock.placePipe(pipe, level, placeAt, block, orientation)) {
                    BlockState state = level.getBlockState(placeAt);
                    if (state.getBlock() == block) {
                        //setTileEntityNBT(world, player, pos, stack);
                        block.setPlacedBy(level, pos, state, player, itemstack);

                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placeAt,
                                itemstack);
                        }

                        BlockState newBlockState = level.getBlockState(placeAt);
                        SoundType soundtype = newBlockState.getBlock()
                            .getSoundType(newBlockState, level, placeAt, player);
                        level.playSound(player, placeAt, soundtype.getPlaceSound(), SoundSource.BLOCKS,
                            (soundtype.getVolume() + 1.0F) / 2.0F,
                            soundtype.getPitch() * 0.8F);

                        itemstack.shrink(1);
                    }
                }

                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }
    }
}
