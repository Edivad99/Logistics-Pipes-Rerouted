package logisticspipes.api.property;

import net.minecraft.world.Container;

public interface InventoryProperty<T extends Container> extends Property<T>, Container {

    @Override
    InventoryProperty<? extends T> copyProperty();
}
