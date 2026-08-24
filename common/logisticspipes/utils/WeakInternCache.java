package logisticspipes.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * An intern cache that holds its keys strongly and its values weakly, dropping entries once the
 * value has been collected.
 * <p>
 * Used for the identity caches whose size is driven by what players happen to be carrying rather
 * than by a registry: every durability value of every tool, every enchanted book, every fluid a mod
 * decides to distinguish by data components. A strong map there is a slow leak.
 *
 * @param <K> the cache key. Must be immutable with value-based equals/hashCode.
 * @param <V> the interned value.
 */
public final class WeakInternCache<K, V> {

	private static final class Ref<K, V> extends WeakReference<V> {

		private final K key;

		Ref(K key, V value, ReferenceQueue<V> queue) {
			super(value, queue);
			this.key = key;
		}
	}

	private final HashMap<K, Ref<K, V>> map = new HashMap<>(1024, 0.5f);
	private final ReferenceQueue<V> refQueue = new ReferenceQueue<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock rlock = lock.readLock();
	private final Lock wlock = lock.writeLock();

	private WeakInternCache() {}

	/**
	 * The cleanup thread is started here rather than from the constructor: starting a thread that
	 * touches {@code this} before construction has completed publishes a not-yet-constructed object
	 * to the thread it just spawned.
	 *
	 * @param threadName name for the daemon thread that drains collected references.
	 */
	public static <K, V> WeakInternCache<K, V> create(String threadName) {
		WeakInternCache<K, V> cache = new WeakInternCache<>();
		Thread cleanup = new Thread(cache::drainForever, threadName);
		cleanup.setDaemon(true);
		cleanup.start();
		return cache;
	}

	/**
	 * @return the interned value for {@code key}, or null if there is none (or it has been
	 * collected). Useful as a fast path when the caller can avoid building a value on a hit.
	 */
	@Nullable
	public V getIfPresent(K key) {
		rlock.lock();
		try {
			Ref<K, V> ref = map.get(key);
			return ref != null ? ref.get() : null;
		} finally {
			rlock.unlock();
		}
	}

	/**
	 * Returns the interned value for {@code key}, creating it with {@code factory} if absent. The
	 * factory runs under the write lock, so it must not call back into this cache.
	 */
	public V getOrCreate(K key, Function<K, V> factory) {
		V hit = getIfPresent(key);
		if (hit != null) {
			return hit;
		}
		wlock.lock();
		try {
			Ref<K, V> ref = map.get(key);
			if (ref != null) {
				V existing = ref.get();
				if (existing != null) {
					return existing;
				}
			}
			V created = factory.apply(key);
			map.put(key, new Ref<>(key, created, refQueue));
			return created;
		} finally {
			wlock.unlock();
		}
	}

	/**
	 * Applies {@code action} to every value still alive in the cache, under the read lock. The
	 * action must not call back into this cache.
	 */
	public void forEachValue(Consumer<V> action) {
		rlock.lock();
		try {
			for (Ref<K, V> ref : map.values()) {
				V value = ref.get();
				if (value != null) {
					action.accept(value);
				}
			}
		} finally {
			rlock.unlock();
		}
	}

	private void drainForever() {
		// Runs until interrupted rather than forever: swallowing InterruptedException and continuing
		// would leave the thread unstoppable and drop the interrupt flag on the floor.
		while (!Thread.currentThread().isInterrupted()) {
			Ref<K, V> ref;
			try {
				@SuppressWarnings("unchecked")
				Ref<K, V> taken = (Ref<K, V>) refQueue.remove();
				ref = taken;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			// The unlock has to be in a finally: this is the write lock every insertion needs, so an
			// exception escaping the loop would hold it forever and wedge the whole cache rather
			// than just killing this thread.
			wlock.lock();
			try {
				do {
					// The entry may have been replaced by a live one in the meantime.
					if (map.get(ref.key) == ref) {
						map.remove(ref.key);
					}
					@SuppressWarnings("unchecked")
					Ref<K, V> next = (Ref<K, V>) refQueue.poll();
					ref = next;
				} while (ref != null);
			} finally {
				wlock.unlock();
			}
		}
	}
}
