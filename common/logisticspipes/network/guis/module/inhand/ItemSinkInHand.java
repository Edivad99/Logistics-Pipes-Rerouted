package logisticspipes.network.guis.module.inhand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import logisticspipes.items.ItemModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.DummyModuleContainer;
import network.rs485.logisticspipes.gui.module.ItemSinkGui;

@StaticResolve
public class ItemSinkInHand extends ModuleInHandGuiProvider {

	public ItemSinkInHand(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsModule module = ItemModule.getLogisticsModule(player, getInvSlot());
		if (!(module instanceof ModuleItemSink)) {
			return null;
		}
		ItemStack usedItemStack = (player.getMainHandItem().getItem() instanceof ItemModule) ?
				player.getMainHandItem() : (player.getOffhandItem().getItem() instanceof ItemModule) ?
				player.getOffhandItem() : ItemStack.EMPTY;
		return ItemSinkGui.create(player.getInventory(), (ModuleItemSink) module, usedItemStack, false, true);
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyModuleContainer dummy = new DummyModuleContainer(player, getInvSlot());
		if (!(dummy.getModule() instanceof ModuleItemSink)) {
			return null;
		}
		dummy.setInventory(((ModuleItemSink) dummy.getModule()).getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new ItemSinkInHand(getId());
	}
}
