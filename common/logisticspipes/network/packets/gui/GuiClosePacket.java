package logisticspipes.network.packets.gui;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@StaticResolve
public class GuiClosePacket extends CoordinatesPacket {

	public GuiClosePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		// always mark the GUI origin's chunk dirty - something may have changed in the GUI
		getTileAs(player.level(), BlockEntity.class).setChanged();
	}

	@Override
	public ModernPacket template() {
		return new GuiClosePacket(getId());
	}
}
