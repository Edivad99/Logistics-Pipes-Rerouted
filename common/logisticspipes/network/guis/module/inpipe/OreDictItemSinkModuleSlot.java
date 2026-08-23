package logisticspipes.network.guis.module.inpipe;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import logisticspipes.gui.modules.GuiOreDictItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.NBTModuleCoordinatesGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.item.ItemIdentifierInventory;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class OreDictItemSinkModuleSlot extends NBTModuleCoordinatesGuiProvider {

	public OreDictItemSinkModuleSlot(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		ModuleOreDictItemSink module = this.getLogisticsModule(player.level(), ModuleOreDictItemSink.class);
		if (module == null) {
			return null;
		}
		module.deserialize(TagValueInput.create(ProblemReporter.DISCARDING,
			player.level().registryAccess(), getNbt()));
		return new GuiOreDictItemSink(player.getInventory(), module);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		ModuleOreDictItemSink module = this.getLogisticsModule(player.level(), ModuleOreDictItemSink.class);
		if (module == null) {
			return null;
		}
		DummyContainer dummy = new DummyContainer(player.getInventory(), new ItemIdentifierInventory(1, "TMP", 1));
		dummy.addDummySlot(0, 0, 0);
		dummy.addNormalSlotsForPlayerInventory(0, 0);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new OreDictItemSinkModuleSlot(getId());
	}
}
