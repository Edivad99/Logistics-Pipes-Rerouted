package logisticspipes.inventory;

import java.util.Map;
import java.util.stream.IntStream;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.inventory.transactor.ITransactor;
import logisticspipes.utils.item.ItemIdentifier;

public abstract class SpecialInventoryHandler implements IInventoryUtil, ITransactor {

    @Override
    public int itemCount(ItemIdentifier itemIdent) {
        final Map<ItemIdentifier, Integer> map = getItemsAndCount();
        return map.getOrDefault(itemIdent, 0);
    }

    @Override
    public ItemStack getMultipleItems(ItemIdentifier itemIdent, int count) {
        if (itemCount(itemIdent) < count) {
            return ItemStack.EMPTY;
        }
        return IntStream.range(0, count).mapToObj((i) -> getSingleItem(itemIdent))
            .filter(itemStack -> !itemStack.isEmpty()).reduce((left, right) -> {
                left.grow(right.getCount());
                return left;
            }).orElse(ItemStack.EMPTY);
    }

    public interface Factory {

        boolean isType(BlockEntity blockEntity, @Nullable Direction direction);

        @Nullable
        SpecialInventoryHandler getUtilForBlockEntity(BlockEntity blockEntity, @Nullable Direction direction,
            ProviderMode mode);
    }
}
