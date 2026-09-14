package logisticspipes.utils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.jspecify.annotations.Nullable;

/**
 * Cache is cleared every 200 ticks when used on Routed Pipes.
 */
public class CacheHolder {

	public enum CacheTypes {
		/**
		 * <p>
		 * Cleared when pipes [every supported type] are added to the network.
		 * </p>
		 */
		Routing,
		/**
		 * <p>
		 * Cleared when an item is inserted or extracted from an adjacent
		 * inventory<br>
		 * The Extraction trigger needs to be implemented separately for every
		 * use case.
		 * </p>
		 */
		Inventory
	}

	private final Table<CacheTypes, Object, Object> cache = HashBasedTable.create();

	public @Nullable Object getCacheFor(CacheTypes type, Object key) {
		return cache.get(type, key);
	}

	public void setCache(CacheTypes type, Object key, Object value) {
		cache.put(type, key, value);
	}

	public void trigger(@Nullable CacheTypes type) {
		if (type != null) {
			cache.row(type).clear();
		} else {
			cache.clear();
		}
	}
}
