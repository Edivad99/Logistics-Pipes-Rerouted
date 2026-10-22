package logisticspipes.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.inventory.transactor.ITransactor;
import logisticspipes.inventory.transactor.TransactorSimple;

/**
 * Finds the way into a block's inventory: a registered {@link SpecialInventoryHandler} if one claims
 * the block, otherwise its item handler capability.
 */
public class InventoryUtilFactory {

    private final List<SpecialInventoryHandler.Factory> handlerFactories = new ArrayList<>();

    @Nullable
    private static ResourceHandler<ItemResource> getItemHandler(BlockEntity blockEntity,
        @Nullable Direction direction) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }
        // Pass the known block entity: the short getCapability() overload looks the state and the
        // block entity up on the level, and that goes through getChunk() — a *blocking chunk load*.
        // Called from the pipe adjacency scan, which also runs while a chunk is being unloaded, it
        // would start a chunk generation task the server can no longer finish and hang the save.
        BlockPos blockPos = blockEntity.getBlockPos();
        BlockState blockState = blockEntity.getBlockState();
        return level.getCapability(Capabilities.Item.BLOCK, blockPos, blockState, blockEntity, direction);
    }

    public void registerHandler(SpecialInventoryHandler.Factory handlerFactory) {
        handlerFactories.add(handlerFactory);
        LogisticsPipes.LOG.info("Loaded SpecialInventoryHandler.Factory: {}",
            handlerFactory.getClass().getCanonicalName());
    }

    @Nullable
    public IInventoryUtil getInventoryUtil(@Nullable BlockEntity blockEntity, @Nullable Direction direction) {
        return getHidingInventoryUtil(blockEntity, direction, ProviderMode.DEFAULT);
    }

    @Nullable
    public IInventoryUtil getHidingInventoryUtil(@Nullable BlockEntity blockEntity, @Nullable Direction direction,
        ProviderMode mode) {
        if (blockEntity == null) {
            return null;
        }
        SpecialInventoryHandler special = getSpecialHandlerFor(blockEntity, direction, mode);
        if (special != null) {
            return special;
        }
        ResourceHandler<ItemResource> handler = getItemHandler(blockEntity, direction);
        return handler == null ? null : new InventoryUtil(handler, mode);
    }

    @Nullable
    public ITransactor getTransactor(BlockEntity blockEntity, @Nullable Direction direction) {
        SpecialInventoryHandler special = getSpecialHandlerFor(blockEntity, direction, ProviderMode.DEFAULT);
        if (special != null) {
            return special;
        }
        ResourceHandler<ItemResource> handler = getItemHandler(blockEntity, direction);
        return handler == null ? null : new TransactorSimple(handler);
    }

    @Nullable
    private SpecialInventoryHandler getSpecialHandlerFor(BlockEntity blockEntity, @Nullable Direction direction,
        ProviderMode mode) {
        return handlerFactories.stream()
            .filter(factory -> factory.isType(blockEntity, direction))
            .map(factory -> factory.getUtilForBlockEntity(blockEntity, direction, mode))
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
    }
}
