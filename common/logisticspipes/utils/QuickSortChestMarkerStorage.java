package logisticspipes.utils;

import java.util.Collection;
import java.util.HashMap;

import lombok.Getter;

public class QuickSortChestMarkerStorage {

	@Getter
	private static final QuickSortChestMarkerStorage instance = new QuickSortChestMarkerStorage();

	private QuickSortChestMarkerStorage() {}

	private record SlotKey(int x, int y, int z, int slot) {}

	private final HashMap<SlotKey, Integer> marker = new HashMap<>();

	@Getter
	private boolean isActivated = false;

	public void setSlots(int x, int y, int z, int slot, int pos) {
		if (isActivated) {
			marker.put(new SlotKey(x, y, z, slot), pos);
		}
	}

	public void enable() {
		marker.clear();
		isActivated = true;
	}

	public void disable() {
		marker.clear();
		isActivated = false;
	}

	public Collection<Integer> getMarker() {
		return marker.values();
	}
}
