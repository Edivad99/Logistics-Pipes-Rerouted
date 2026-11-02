package logisticspipes.property;

import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.utils.item.SimpleStackInventory;

public class SimpleInventoryProperty implements Property<SimpleStackInventory>, Container {

    private final SimpleStackInventory inv;

    private final String tagKey;

    private final CopyOnWriteArraySet<ObserverCallback<SimpleStackInventory>> propertyObservers =
        new CopyOnWriteArraySet<>();

    public SimpleInventoryProperty(SimpleStackInventory inv, String tagKey) {
        this.inv = inv;
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<SimpleStackInventory>> getPropertyObservers() {
        return propertyObservers;
    }

    @Override
    public SimpleStackInventory copyValue() {
        return new SimpleStackInventory(inv);
    }

    @Override
    public SimpleInventoryProperty copyProperty() {
        return new SimpleInventoryProperty(copyValue(), tagKey);
    }

    @Override
    public void deserialize(ValueInput input) {
        inv.deserialize(input, tagKey);
    }

    @Override
    public void serialize(ValueOutput output) {
        inv.serialize(output, tagKey);
    }

    public void clearInventorySlotContents(int i) {
        inv.clearInventorySlotContents(i);
        iChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        inv.dropContents(level, pos);
        iChanged();
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = inv.removeItem(index, count);
        iChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = inv.removeItemNoUpdate(index);
        iChanged();
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inv.setItem(index, stack);
        iChanged();
    }

    @Override
    public void setChanged() {
        inv.setChanged();
        iChanged();
    }

    public void clear() {
        clearContent();
    }

    @Override
    public void clearContent() {
        inv.clearContent();
        iChanged();
    }

    /**
     * @deprecated do not change the returned ItemStack, and do not call setChanged afterwards
     */
    @Deprecated
    @Override
    public ItemStack getItem(int index) {
        return inv.getItem(index);
    }

    public boolean isSlotEmpty(int index) {
        return inv.getItem(index).isEmpty();
    }

    // ── plain delegation ─────────────────────────────────────────────────────
    // Only Container's abstract members: its default members stay inherited, so they keep running
    // against this property and therefore through the notifying overrides above.

    @Override
    public int getContainerSize() {
        return inv.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.stillValid(player);
    }
}
