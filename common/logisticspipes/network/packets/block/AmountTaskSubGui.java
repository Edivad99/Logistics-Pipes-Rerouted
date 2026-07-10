package logisticspipes.network.packets.block;

import logisticspipes.gui.GuiStatistics;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class AmountTaskSubGui extends InventoryModuleCoordinatesPacket {

	public AmountTaskSubGui(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().screen instanceof GuiStatistics) {
			((GuiStatistics) Minecraft.getInstance().screen).handlePacket1(getIdentList());
		}
	}

	@Override
	public ModernPacket template() {
		return new AmountTaskSubGui(getId());
	}
}
