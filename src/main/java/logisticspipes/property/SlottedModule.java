package logisticspipes.property;

import org.jspecify.annotations.Nullable;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;

public record SlottedModule(int slot, @Nullable LogisticsModule module) {

    public static final String SLOT_INDEX_KEY = "slotted_module.slot";
    public static final String MODULE_NAME_KEY = "slotted_module.name";

    public boolean isEmpty() {
        return module == null;
    }

    public void registerPosition() {
        if (module != null) {
            module.registerPosition(ModulePositionType.SLOT, slot);
        }
    }
}
