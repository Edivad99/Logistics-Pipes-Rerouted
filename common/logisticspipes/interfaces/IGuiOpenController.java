package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;


public interface IGuiOpenController {

	void guiOpenedByPlayer(Player player);

	void guiClosedByPlayer(Player player);
}
