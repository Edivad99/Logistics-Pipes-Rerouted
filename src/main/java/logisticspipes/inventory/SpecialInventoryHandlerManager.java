package logisticspipes.inventory;

import net.neoforged.fml.ModList;

import appeng.api.ids.AEConstants;

import logisticspipes.LPConstants;
import logisticspipes.integrations.ae2.AEInterfaceInventoryHandler;
import logisticspipes.integrations.refinedstorage.RSInterfaceInventoryHandler;

public final class SpecialInventoryHandlerManager {

    private SpecialInventoryHandlerManager() {
    }

    public static void load() {
        if (ModList.get().isLoaded(AEConstants.MOD_ID)) {
            InventoryUtilFactory.get().registerHandler(new AEInterfaceInventoryHandler());
        }
        if (ModList.get().isLoaded(LPConstants.RS_MOD_ID)) {
            InventoryUtilFactory.get().registerHandler(new RSInterfaceInventoryHandler());
        }
    }
}
