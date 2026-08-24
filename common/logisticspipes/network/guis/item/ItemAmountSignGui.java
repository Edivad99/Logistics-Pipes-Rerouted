package logisticspipes.network.guis.item;

import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.ItemAmountSignCreationGui;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ItemAmountSignGui extends CoordinatesGuiProvider {

	@Getter
	@Setter
	private Direction dir;

	public ItemAmountSignGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof CoreRoutedPipe routedPipe)) {
			return null;
		}
		// The sign itself is what the GUI is built around, and it reaches the client in its own
		// packet. Asking for a retry beats dereferencing null: TargetNotFoundException is a
		// DelayPacketException, so the handler re-queues this packet instead of crashing.
		if (!(routedPipe.getPipeSign(dir) instanceof ItemAmountPipeSign)) {
			throw new TargetNotFoundException("No item amount sign on side " + dir + " yet", this);
		}
		return new ItemAmountSignCreationGui(player, routedPipe, dir);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof CoreRoutedPipe)) {
			return null;
		}
		ItemAmountPipeSign sign = ((ItemAmountPipeSign) ((CoreRoutedPipe) pipe.pipe).getPipeSign(dir));
		Objects.requireNonNull(sign);
		DummyContainer dummy = new DummyContainer(player.getInventory(), sign.itemTypeInv);
		dummy.addDummySlot(0, 0, 0);
		dummy.addNormalSlotsForPlayerInventory(0, 0);
		return dummy;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(dir);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		dir = input.readFacing();
	}

	@Override
	public GuiProvider template() {
		return new ItemAmountSignGui(getId());
	}
}
