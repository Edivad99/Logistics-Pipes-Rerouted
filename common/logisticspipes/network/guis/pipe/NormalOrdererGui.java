package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.orderer.NormalGuiOrderer;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class NormalOrdererGui extends CoordinatesGuiProvider {

	private int dim;

	public NormalOrdererGui(int id) {
		super(id);
	}

	public NormalOrdererGui setDim(int dim) {
		this.dim = dim;
		return this;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(dim);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		dim = input.readInt();
	}

	@Override
	public Object getClientGui(Player player) {
		return new NormalGuiOrderer(getPosX(), getPosY(), getPosZ(), dim, player);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		return new DummyContainer(player.getInventory(), null);
	}

	@Override
	public GuiProvider template() {
		return new NormalOrdererGui(getId());
	}
}
