package logisticspipes.network.guis.item;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.hud.GuiHUDSettings;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class HUDSettingsGui extends GuiProvider {

	private int slot;

	public HUDSettingsGui(int id) {
		super(id);
	}

	public HUDSettingsGui setSlot(int slot) {
		this.slot = slot;
		return this;
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(slot);
	}

	@Override
	public void readData(LPDataInput input) {
		slot = input.readInt();
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiHUDSettings(player, slot);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addRestrictedHotbarForPlayerInventory(10, 160);
		dummy.addRestrictedArmorForPlayerInventory(10, 60);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new HUDSettingsGui(getId());
	}
}
