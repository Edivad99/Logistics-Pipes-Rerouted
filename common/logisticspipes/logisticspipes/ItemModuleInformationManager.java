package logisticspipes.logisticspipes;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.proxy.MainProxy;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;

public class ItemModuleInformationManager {

	public static void saveInformation(@Nonnull ItemStack stack, LogisticsModule module, HolderLookup.Provider provider) {
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
			if (!stack.hasTag()) {
				stack.setTag(new CompoundTag());
			}
			CompoundTag tag = Objects.requireNonNull(stack.getTag());
			tag.put("informationList", list);
			tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
			return;
		}
		if (!stack.hasTag()) {
			stack.setTag(new CompoundTag());
		}
		CompoundTag tag = Objects.requireNonNull(stack.getTag());
		tag.put("moduleInformation", nbt);
		if (module instanceof IClientInformationProvider) {
			List<String> information = ((IClientInformationProvider) module).getClientInformation();
			if (information.size() > 0) {
				ListTag list = new ListTag();
				for (String info : information) {
					list.add(StringTag.valueOf(info));
				}
				tag.put("informationList", list);
			}
		}
		tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
	}

	public static void readInformation(@Nonnull ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		if (stack.hasTag()) {
			CompoundTag nbt = Objects.requireNonNull(stack.getTag());
			if (nbt.contains("moduleInformation")) {
				CompoundTag moduleInformation = nbt.getCompound("moduleInformation");
				module.readFromNBT(moduleInformation, module.getWorld().registryAccess());
			}
		}
	}
}
