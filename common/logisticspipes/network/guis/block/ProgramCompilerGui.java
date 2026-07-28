package logisticspipes.network.guis.block;

import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import logisticspipes.client.gui.screen.ProgramCompilerScreen;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Deprecated(forRemoval = true)
public class ProgramCompilerGui extends CoordinatesGuiProvider {

	public ProgramCompilerGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		//return new ProgramCompilerScreen(player, getTileAs(player.level(), LogisticsProgramCompilerBlockEntity.class));
        return null;
	}

	@Override
	public AbstractContainerMenu getContainer(Player player) {
		LogisticsProgramCompilerBlockEntity compilerBlock = getTileAs(player.level(), LogisticsProgramCompilerBlockEntity.class);
		/*
		DummyContainer dummy = new DummyContainer(player, null, compilerBlock);

		dummy.addRestrictedSlot(0, compilerBlock.getInventory(), 10, 10, LPItems.DISK.get());
		dummy.addRestrictedSlot(1, compilerBlock.getInventory(), 154, 10, LPItems.LOGISTICS_PROGRAMMER.get());

		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
		 */
        return null;
	}

	@Override
	public GuiProvider template() {
		return new ProgramCompilerGui(getId());
	}
}
