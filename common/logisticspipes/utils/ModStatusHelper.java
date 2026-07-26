package logisticspipes.utils;


import net.neoforged.fml.ModList;

public class ModStatusHelper {

	public static boolean isModLoaded(String modId) {
		if (modId.contains("@")) {
			String version = modId.substring(modId.indexOf('@') + 1);
			modId = modId.substring(0, modId.indexOf('@'));
			if (ModList.get().isLoaded(modId)) {
				final String modIdFinal = modId;
				return ModList.get().getModContainerById(modIdFinal)
						.map(mod -> mod.getModInfo().getVersion().toString().startsWith(version))
						.orElse(false);
			}
			return false;
		} else {
			return ModList.get().isLoaded(modId);
		}
	}

	public static boolean areModsLoaded(String modIds) {
		if (modIds.contains("+")) {
			for (String modId : modIds.split("\\+")) {
				if (!isModLoaded(modId)) {
					return false;
				}
			}
			return true;
		} else {
			return isModLoaded(modIds);
		}
	}
}
