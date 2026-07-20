package logisticspipes.proxy.specialinventoryhandler;
// NOTE: Applied Energistics 2 API not ported — stub

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class AEInterfaceInventoryHandler extends SpecialInventoryHandler implements SpecialInventoryHandler.Factory {
    @Override public boolean init() { return false; }
    @Override public boolean isType(BlockEntity tile, @Nullable Direction dir) { return false; }
    @Override @Nullable public SpecialInventoryHandler getUtilForTile(BlockEntity tile, @Nullable Direction dir, ProviderMode mode) { return null; }
    @Override public int itemCount(ItemIdentifier item) { return 0; }
    @Override
    public ItemStack getSingleItem(ItemIdentifier item) { return ItemStack.EMPTY; }
    @Override public boolean containsUndamagedItem(ItemIdentifier item) { return false; }
    @Override public int roomForItem(ItemStack stack) { return 0; }
    @Override public int getContainerSize() { return 0; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override
    public ItemStack add(ItemStack stack, Direction from, boolean doAdd) { return stack; }
    @Override
    public Set<ItemIdentifier> getItems() { return new java.util.HashSet<>(); }
    @Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() { return new HashMap<>(); }
}
