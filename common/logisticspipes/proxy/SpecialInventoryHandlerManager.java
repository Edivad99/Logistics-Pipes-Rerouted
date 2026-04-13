package logisticspipes.proxy;

import net.minecraftforge.fml.ModList;

import static logisticspipes.LPConstants.appliedenergisticsModID;

import logisticspipes.proxy.specialinventoryhandler.AEInterfaceInventoryHandler;
import network.rs485.logisticspipes.proxy.StorageDrawersProxy;

public class SpecialInventoryHandlerManager {

	public static void load() {

		if (ModList.get().isLoaded(appliedenergisticsModID)) {
			SimpleServiceLocator.inventoryUtilFactory.registerHandler(new AEInterfaceInventoryHandler());
		}

		// TODO(1.20.1): BuildCraft not ported — inventory handler registration disabled
		// SimpleServiceLocator.buildCraftProxy.registerInventoryHandler();

		StorageDrawersProxy.INSTANCE.registerInventoryHandler();

		// Charset has no 1.20.1 port — removed
	}

}
