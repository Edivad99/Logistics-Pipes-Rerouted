package logisticspipes.network.packets.block;

import logisticspipes.logic.interfaces.ILogicControllerTile;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.logic.LogicControllerGuiProvider;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@StaticResolve
public class LogicControllerPacket extends CoordinatesPacket {

	public LogicControllerPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		ILogicControllerTile tile = this.getTileAs(player.level(), ILogicControllerTile.class);
		if (tile == null) {
			return;
		}
		NewGuiHandler.getGui(LogicControllerGuiProvider.class).setTilePos((BlockEntity) tile).open(player);
	}

	@Override
	public ModernPacket template() {
		return new LogicControllerPacket(getId());
	}
}
