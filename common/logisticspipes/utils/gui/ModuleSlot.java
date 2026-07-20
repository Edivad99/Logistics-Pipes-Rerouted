package logisticspipes.utils.gui;

import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.pipes.PipeLogisticsChassis;
import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModuleSlot extends RestrictedSlot {

	@Getter
	private final PipeLogisticsChassis _pipe;
	@Getter
	private final int _moduleIndex;

	public ModuleSlot(Container iinventory, int i, int j, int k, PipeLogisticsChassis pipe) {
		super(iinventory, i, j, k, ItemModule.class);
		_pipe = pipe;
		_moduleIndex = i;
	}

	@Override
	public void onTake(Player player, ItemStack itemStack) {
		ItemModuleInformationManager.saveInformation(itemStack, _pipe.getSubModule(_moduleIndex), player.registryAccess());
		super.onTake(player, itemStack);
	}
}
