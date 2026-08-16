package logisticspipes.utils.gui;

import java.util.Objects;

import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.utils.DummyLevelProvider;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DummyModuleContainer extends DummyContainer {

	private final LogisticsModule module;
	private final int slot;

	public DummyModuleContainer(Player player, int slot) {
		super(player.getInventory(), null);
		this.slot = slot;
		ItemStack moduleStack = player.getInventory().items.get(slot);
		if (moduleStack.isEmpty()) throw new IllegalStateException("Module stack is empty");
		module = ((ItemModule) moduleStack.getItem()).getModuleForItem(moduleStack, null, new DummyLevelProvider(player.level()), null);
		Objects.requireNonNull(module, "module was null for item " + moduleStack.toString());
		module.registerPosition(ModulePositionType.IN_HAND, slot);
		ItemModuleInformationManager.readInformation(moduleStack, module);
	}

	public LogisticsModule getModule() {
		return module;
	}

	public void setInventory(Container inv) {
		dummyInventory = inv;
	}

	@Override
    protected Slot addSlot(Slot slotIn) {
		if (slotIn.getSlotIndex() == slot && slotIn.container == playerInventory) {
			return super.addSlot(new UnmodifiableSlot(slotIn));
		}
		return super.addSlot(slotIn);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		ItemModuleInformationManager.saveInformation(player.getInventory().items.get(slot), module, player.registryAccess());
		player.getInventory().setChanged();
	}
}
