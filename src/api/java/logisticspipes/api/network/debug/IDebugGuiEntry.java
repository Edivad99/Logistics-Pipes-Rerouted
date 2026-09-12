package logisticspipes.api.network.debug;

import java.util.concurrent.Future;

public abstract class IDebugGuiEntry {

	public static IDebugGuiEntry create()
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			return (IDebugGuiEntry) Class.forName("network.rs485.debug.gui.DebugGuiEntry").newInstance();
		} catch (ReflectiveOperationException ignored) {}
		return (IDebugGuiEntry) Class.forName("network.rs485.debuggui.DebugGuiEntry").newInstance();
	}

	public abstract IDataConnection startServerDebugging(Object object, IDataConnection outgoingData,
			IObjectIdentification objectIdent);

	public abstract Future<IDataConnection> startClientDebugging(String name, IDataConnection outgoingData);

	public abstract void exec();

}
