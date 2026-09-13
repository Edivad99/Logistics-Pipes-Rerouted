package logisticspipes.proxy;

import net.neoforged.fml.ModList;

import appeng.api.ids.AEConstants;

import logisticspipes.LPConstants;
import logisticspipes.api.event.RegisterTankHandlersEvent;
import logisticspipes.integrations.ae2.AENetworkTankHandler;
import logisticspipes.integrations.refinedstorage.RSNetworkTankHandler;

/** The tank handlers Logistics Pipes ships itself, for the storage mods it knows about. */
public class SpecialTankHandlerManager {

    public static void register(RegisterTankHandlersEvent event) {
        if (ModList.get().isLoaded(AEConstants.MOD_ID)) {
            event.register(new AENetworkTankHandler());
        }
        if (ModList.get().isLoaded(LPConstants.RS_MOD_ID)) {
            event.register(new RSNetworkTankHandler());
        }
    }
}
