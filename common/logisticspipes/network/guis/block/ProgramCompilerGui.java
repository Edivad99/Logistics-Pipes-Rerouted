package logisticspipes.network.guis.block;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.gui.GuiProgramCompiler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

@StaticResolve
public class ProgramCompilerGui extends CoordinatesGuiProvider {

	public ProgramCompilerGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiProgramCompiler(player, getTileAs(player.level(), LogisticsProgramCompilerTileEntity.class));
	}

	@Override
	public AbstractContainerMenu getContainer(Player player) {
		LogisticsProgramCompilerTileEntity compilerBlock = getTileAs(player.level(), LogisticsProgramCompilerTileEntity.class);
		DummyContainer dummy = new DummyContainer(player, null, compilerBlock);

		dummy.addRestrictedSlot(0, compilerBlock.getInventory(), 10, 10, LPItems.disk.get());
		dummy.addRestrictedSlot(1, compilerBlock.getInventory(), 154, 10, LPItems.logisticsProgrammer.get());

		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new ProgramCompilerGui(getId());
	}
}
