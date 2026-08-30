package logisticspipes.network.guis.pipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.orderer.NormalGuiOrderer;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class NormalOrdererGui extends CoordinatesGuiProvider {

	private Identifier dim = Identifier.withDefaultNamespace("overworld");

	public NormalOrdererGui(int id) {
		super(id);
	}

	public NormalOrdererGui setDim(Identifier dim) {
		this.dim = dim;
		return this;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeIdentifier(dim);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		Identifier rl = input.readIdentifier();
		if (rl != null) dim = rl;
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
