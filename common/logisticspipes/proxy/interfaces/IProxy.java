package logisticspipes.proxy.interfaces;

public interface IProxy {

    void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag);

    void sendBroadCast(String message);

	void tickServer();

	void tickClient();

    boolean checkSinglePlayerOwner(String commandSenderName);

	void openFluidSelectGui(int slotId);
}
