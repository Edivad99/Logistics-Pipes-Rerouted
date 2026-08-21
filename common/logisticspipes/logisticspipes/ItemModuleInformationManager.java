package logisticspipes.logisticspipes;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.proxy.MainProxy;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ItemModuleInformationManager {

	public static void saveInformation(ItemStack stack, LogisticsModule module, HolderLookup.Provider provider) {
		if (module == null) {
			return;
		}
		CompoundTag nbt = new CompoundTag();
		module.writeToNBT(nbt, provider);
		if (nbt.equals(new CompoundTag())) {
			return;
		}
		if (MainProxy.isClient()) {
			ListTag list = new ListTag();
			String info1 = "Please reopen the window";
			String info2 = "to see the information.";
			list.add(StringTag.valueOf(info1));
			list.add(StringTag.valueOf(info2));
			stack.update(
					DataComponents.CUSTOM_DATA,
					CustomData.EMPTY,
					customData -> {
						CompoundTag tag = customData.copyTag();
						tag.put("informationList", list);
						tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
						return CustomData.of(tag);
					}
			);
			return;
		}
		stack.update(
				DataComponents.CUSTOM_DATA,
				CustomData.EMPTY,
				customData -> {
					CompoundTag tag = customData.copyTag();
					tag.put("moduleInformation", nbt);
					if (module instanceof IClientInformationProvider) {
						List<String> information = ((IClientInformationProvider) module).getClientInformation();
						if (!information.isEmpty()) {
							ListTag list = new ListTag();
							for (String info : information) {
								list.add(StringTag.valueOf(info));
							}
							tag.put("informationList", list);
						}
					}
					tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
					return CustomData.of(tag);
				}
		);
	}

	public static void readInformation(ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		if (stack.has(DataComponents.CUSTOM_DATA)) {
			CompoundTag nbt = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
			if (nbt.contains("moduleInformation")) {
				CompoundTag moduleInformation = nbt.getCompoundOrEmpty("moduleInformation");
				module.readFromNBT(moduleInformation, module.getWorld().registryAccess());
			}
		}
	}
}
