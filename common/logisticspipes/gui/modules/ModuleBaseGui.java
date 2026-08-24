package logisticspipes.gui.modules;

import net.minecraft.client.input.CharacterEvent;
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
	public boolean charTyped(CharacterEvent event) {
		char typedChar = (char) event.codepoint();
		int keyCode = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (module == null) {
			return super.charTyped(event);
		}
		if (keyCode == 1 || typedChar == 'e') {
			if (module.getSlot() == ModulePositionType.SLOT) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiOpenChassis.class).setBlockPos(module.getBlockPos()));
			}
			return super.charTyped(event);
		}
		return super.charTyped(event);
	}
}
