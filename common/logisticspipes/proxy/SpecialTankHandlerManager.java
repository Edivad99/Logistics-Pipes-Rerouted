package logisticspipes.proxy;

import net.neoforged.fml.ModList;

import appeng.api.ids.AEConstants;

import logisticspipes.LPConstants;
import logisticspipes.integrations.ae2.AENetworkTankHandler;
import logisticspipes.integrations.refinedstorage.RSNetworkTankHandler;

public class SpecialTankHandlerManager {

    public static void load() {
        if (ModList.get().isLoaded(AEConstants.MOD_ID)) {
            SimpleServiceLocator.specialTankHandler.registerProvider(new AENetworkTankHandler());
        }
        if (ModList.get().isLoaded(LPConstants.RS_MOD_ID)) {
            SimpleServiceLocator.specialTankHandler.registerProvider(new RSNetworkTankHandler());
        }
    }
}
