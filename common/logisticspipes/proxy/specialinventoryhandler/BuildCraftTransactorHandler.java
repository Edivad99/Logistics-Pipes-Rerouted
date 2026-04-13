package logisticspipes.proxy.specialinventoryhandler;
// TODO: BuildCraft not ported — stub

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class BuildCraftTransactorHandler extends SpecialInventoryHandler implements SpecialInventoryHandler.Factory {
    @Override public boolean init() { return false; }
    @Override public boolean isType(@Nonnull BlockEntity tile, @Nullable Direction dir) { return false; }
    @Override @Nullable public SpecialInventoryHandler getUtilForTile(@Nonnull BlockEntity tile, @Nullable Direction dir, @Nonnull ProviderMode mode) { return null; }
    @Override public int itemCount(@Nonnull ItemIdentifier item) { return 0; }
    @Override @Nonnull public ItemStack getSingleItem(ItemIdentifier item) { return ItemStack.EMPTY; }
    @Override public boolean containsUndamagedItem(@Nonnull ItemIdentifier item) { return false; }
    @Override public int roomForItem(@Nonnull ItemStack stack) { return 0; }
    @Override public int getContainerSize() { return 0; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override @Nonnull public ItemStack add(@Nonnull ItemStack stack, Direction from, boolean doAdd) { return stack; }
    @Override @Nonnull public Set<ItemIdentifier> getItems() { return new java.util.HashSet<>(); }
    @Override @Nonnull public Map<ItemIdentifier, Integer> getItemsAndCount() { return new HashMap<>(); }
}
