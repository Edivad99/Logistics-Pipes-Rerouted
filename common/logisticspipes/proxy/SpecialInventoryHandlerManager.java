package logisticspipes.proxy;

import net.neoforged.fml.ModList;

import appeng.api.ids.AEConstants;

import logisticspipes.LPConstants;
import logisticspipes.integrations.ae2.AEInterfaceInventoryHandler;
import logisticspipes.integrations.refinedstorage.RSInterfaceInventoryHandler;

public class SpecialInventoryHandlerManager {

    public static void load() {
        if (ModList.get().isLoaded(AEConstants.MOD_ID)) {
            SimpleServiceLocator.inventoryUtilFactory.registerHandler(new AEInterfaceInventoryHandler());
        }
        if (ModList.get().isLoaded(LPConstants.RS_MOD_ID)) {
            SimpleServiceLocator.inventoryUtilFactory.registerHandler(new RSInterfaceInventoryHandler());
        }
    }
}
