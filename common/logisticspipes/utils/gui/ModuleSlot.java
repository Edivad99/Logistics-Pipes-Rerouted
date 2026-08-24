package logisticspipes.utils.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.world.item.ItemModule;

public class ModuleSlot extends RestrictedSlot {

	@Getter
	private final PipeLogisticsChassis pipe;
	@Getter
	private final int moduleIndex;

	public ModuleSlot(Container iinventory, int i, int j, int k, PipeLogisticsChassis pipe) {
		super(iinventory, i, j, k, ItemModule.class);
		this.pipe = pipe;
		moduleIndex = i;
	}

	@Override
	public void onTake(Player player, ItemStack itemStack) {
		ItemModuleInformationManager.saveInformation(itemStack, pipe.getSubModule(moduleIndex), player.registryAccess());
		super.onTake(player, itemStack);
	}
}
