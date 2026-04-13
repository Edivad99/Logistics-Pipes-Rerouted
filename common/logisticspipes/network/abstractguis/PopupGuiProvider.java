package logisticspipes.network.abstractguis;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class PopupGuiProvider extends GuiProvider {

	public PopupGuiProvider(int id) {
		super(id);
	}

	@Override
	public final AbstractContainerMenu getContainer(Player player) {
		return null;
	}
}
