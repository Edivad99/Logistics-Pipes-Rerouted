/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.world.item;

import javax.annotation.Nullable;

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

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.world.level.block.LPBlocks;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

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
                CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

                if (pipe == null) {
                    LogisticsPipes.LOG.warn("Pipe failed to create during placement at {},{},{}", pos.getX(),
                        pos.getY(), pos.getZ());
                    return InteractionResult.PASS;
                }

                if (LogisticsBlockGenericPipe.placePipe(pipe, level, pos, block, null)) {
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
            DoubleCoordinates placeAt = new DoubleCoordinates(pos);
            LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> globalPos = new LPPositionSet<>(
                DoubleCoordinatesType.class);
            globalPos.add(new DoubleCoordinatesType<>(placeAt, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
            LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = multiPipe.getSubBlocks();
            ITubeOrientation orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());
            if (orientation == null) {
                return InteractionResult.FAIL;
            }
            orientation.rotatePositions(positions);
            positions.stream().map(iPos -> iPos.add(placeAt)).forEach(globalPos::add);
            globalPos.addToAll(orientation.getOffset());
            placeAt.add(orientation.getOffset());

            for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> iPos : globalPos) {
                if (!player.mayUseItemAt(iPos.getBlockPos(), facing, itemstack) || !level.isEmptyBlock(
                    iPos.getBlockPos())) {
                    BlockEntity tile = level.getBlockEntity(iPos.getBlockPos());
                    boolean canPlace = false;
                    if (tile instanceof LogisticsTileGenericSubMultiBlock) {
                        if (CoreMultiBlockPipe.canShare(((LogisticsTileGenericSubMultiBlock) tile).getSubTypes(),
                            iPos.getType())) {
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
                CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

                if (pipe == null) {
                    LogisticsPipes.LOG.warn("Pipe failed to create during placement at {},{},{}", pos.getX(),
                        pos.getY(), pos.getZ());
                    return InteractionResult.SUCCESS;
                }

                if (LogisticsBlockGenericPipe.placePipe(pipe, level, placeAt.getBlockPos(), block, orientation)) {
                    BlockState state = level.getBlockState(placeAt.getBlockPos());
                    if (state.getBlock() == block) {
                        //setTileEntityNBT(world, player, pos, stack);
                        block.setPlacedBy(level, pos, state, player, itemstack);

                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placeAt.getBlockPos(),
                                itemstack);
                        }

                        BlockState newBlockState = level.getBlockState(placeAt.getBlockPos());
                        SoundType soundtype = newBlockState.getBlock()
                            .getSoundType(newBlockState, level, placeAt.getBlockPos(), player);
                        level.playSound(player, placeAt.getBlockPos(), soundtype.getPlaceSound(), SoundSource.BLOCKS,
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
