package logisticspipes.network.guis.pipe;

import logisticspipes.gui.GuiChassisPipe;
import logisticspipes.world.item.ItemUpgrade;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractguis.BooleanModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@StaticResolve
public class ChassisGuiProvider extends BooleanModuleCoordinatesGuiProvider {

	public ChassisGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeLogisticsChassis)) {
			return null;
		}
		return new GuiChassisPipe(player, (PipeLogisticsChassis) pipe.pipe, isFlag());
	}

	@Override
	public DummyContainer getContainer(Player player) {
		LogisticsTileGenericPipe pipe = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (!(pipe.pipe instanceof PipeLogisticsChassis)) {
			return null;
		}
		final PipeLogisticsChassis chassiPipe = (PipeLogisticsChassis) pipe.pipe;
		Container moduleInventory = chassiPipe.getModuleInventory(player.registryAccess());
		DummyContainer dummy = new DummyContainer(player.getInventory(), moduleInventory);
		if (chassiPipe.getChassisSize() < 5) {
			dummy.addNormalSlotsForPlayerInventory(18, 97);
		} else {
			dummy.addNormalSlotsForPlayerInventory(18, 174);
		}
		for (int i = 0; i < chassiPipe.getChassisSize(); i++) {
			dummy.addModuleSlot(i, moduleInventory, 19, 9 + 20 * i, chassiPipe);
		}

		if (chassiPipe.getUpgradeManager().hasUpgradeModuleUpgrade()) {
			for (int i = 0; i < chassiPipe.getChassisSize(); i++) {
				final int fI = i;
				ModuleUpgradeManager upgradeManager = chassiPipe.getModuleUpgradeManager(i);
				dummy.addUpgradeSlot(0, upgradeManager, 0, 145, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassiPipe, fI));
				dummy.addUpgradeSlot(1, upgradeManager, 1, 165, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassiPipe, fI));
			}
		}
		return dummy;
	}

	public static boolean checkStack(ItemStack stack, PipeLogisticsChassis chassiPipe, int moduleSlot) {
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemUpgrade)) {
			return false;
		}
		LogisticsModule module = chassiPipe.getModules().getModule(moduleSlot);
		if (module == null) {
			return false;
		}
		return ((ItemUpgrade) stack.getItem()).getUpgradeForItem(stack, null).isAllowedForModule(module);
	}

	@Override
	public GuiProvider template() {
		return new ChassisGuiProvider(getId());
	}
}
