package logisticspipes.api.property.layer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import logisticspipes.api.property.InventoryProperty;

public class PropertyOverlayInventoryAdapter<T extends Container, P extends InventoryProperty<T>>
    implements Container {

    private final PropertyOverlay<T, P> propertyOverlay;

    public PropertyOverlayInventoryAdapter(PropertyOverlay<T, P> propertyOverlay) {
        this.propertyOverlay = propertyOverlay;
    }

    @Override
    public int getContainerSize() {
        return propertyOverlay.read(Container::getContainerSize);
    }

    @Override
    public boolean isEmpty() {
        return propertyOverlay.read(Container::isEmpty);
    }

    /** @deprecated do not modify returned ItemStack, handle as immutable */
    @Deprecated
    @Override
    public ItemStack getItem(int index) {
        return propertyOverlay.read(prop -> prop.getItem(index));
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return propertyOverlay.write(prop -> prop.removeItem(index, count));
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return propertyOverlay.write(prop -> prop.removeItemNoUpdate(index));
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        propertyOverlay.writeVoid(prop -> prop.setItem(index, stack));
    }

    @Override
    public int getMaxStackSize() {
        return propertyOverlay.read(Container::getMaxStackSize);
    }

    @Override
    public void setChanged() {
        propertyOverlay.writeVoid(Container::setChanged);
    }

    @Override
    public boolean stillValid(Player player) {
        return propertyOverlay.read(prop -> prop.stillValid(player));
    }

    /** @deprecated no-op on adapter */
    @Deprecated
    @Override
    public void startOpen(ContainerUser containerUser) {
    }

    /** @deprecated no-op on adapter */
    @Deprecated
    @Override
    public void stopOpen(ContainerUser containerUser) {
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return propertyOverlay.read(prop -> prop.canPlaceItem(index, stack));
    }

    @Override
    public void clearContent() {
        propertyOverlay.writeVoid(Container::clearContent);
    }
}
