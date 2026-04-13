package logisticspipes.gui.modules;

import net.minecraft.world.inventory.AbstractContainerMenu;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.GuiOpenChassis;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public abstract class ModuleBaseGui extends LogisticsBaseGuiScreen {

	@Getter
	protected LogisticsModule module;

	public ModuleBaseGui(AbstractContainerMenu par1Container, LogisticsModule module) {
		super(par1Container);
		this.module = module;
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (module == null) {
			return super.charTyped(typedChar, keyCode);
		}
		if (keyCode == 1 || typedChar == 'e') {
			if (module.getSlot() == ModulePositionType.SLOT) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiOpenChassis.class).setBlockPos(module.getBlockPos()));
			}
			return super.charTyped(typedChar, keyCode);
		}
		return super.charTyped(typedChar, keyCode);
	}
}
