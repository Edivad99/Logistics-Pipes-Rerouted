package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.network.TargetLookup;

/**
 * A list that tells the clients watching its chunk whenever it changes.
 *
 * <p>Every mutating method marks the list dirty; the owner decides when to actually send, by
 * calling {@link #sendUpdateToWatchers()} on its own tick.
 */
public class SyncList<E> implements List<E> {

	private final List<E> list;

	/** Where the list lives, and how to say what is in it. Null until {@link #syncTo} is called. */
	private @Nullable Sync<E> sync;

	private boolean dirty = false;

	private record Sync<E>(Level level, BlockPos pos, Function<List<E>, CustomPacketPayload> message) {
	}

	public SyncList() {
		this(new ArrayList<>());
	}

	public SyncList(List<E> list) {
		this.list = list;
	}

	/**
	 * Starts sending updates, and sends one now.
	 *
	 * @param message builds the message that describes the list's contents
	 */
	public void syncTo(Level level, BlockPos pos, Function<List<E>, CustomPacketPayload> message) {
		sync = new Sync<>(level, pos, message);
		send(sync);
	}

	public void sendUpdateToWatchers() {
		final Sync<E> target = sync;
		if (dirty && target != null) {
			dirty = false;
			send(target);
		}
	}

	private void send(Sync<E> target) {
		TargetLookup.sendToChunkWatchers(target.level(), target.pos(), target.message().apply(list));
	}

	/** Marks the list changed without going through one of its own methods. */
	public void setChanged() {
		dirty = true;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object paramObject) {
		return list.contains(paramObject);
	}

	@Override
	public Iterator<E> iterator() {
		return new SyncIter(list.iterator());
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] paramArrayOfT) {
		return list.toArray(paramArrayOfT);
	}

	@Override
	public boolean add(E paramE) {
		boolean flag = list.add(paramE);
		setChanged();
		return flag;
	}

	@Override
	public boolean remove(Object paramObject) {
		boolean flag = list.remove(paramObject);
		setChanged();
		return flag;
	}

	@Override
	public boolean containsAll(Collection<?> paramCollection) {
		return list.containsAll(paramCollection);
	}

	@Override
	public boolean addAll(Collection<? extends E> paramCollection) {
		boolean flag = list.addAll(paramCollection);
		setChanged();
		return flag;
	}

	@Override
	public boolean addAll(int paramInt, Collection<? extends E> paramCollection) {
		boolean flag = list.addAll(paramInt, paramCollection);
		setChanged();
		return flag;
	}

	@Override
	public boolean removeAll(Collection<?> paramCollection) {
		boolean flag = list.removeAll(paramCollection);
		setChanged();
		return flag;
	}

	@Override
	public boolean retainAll(Collection<?> paramCollection) {
		boolean flag = list.retainAll(paramCollection);
		setChanged();
		return flag;
	}

	@Override
	public void clear() {
		list.clear();
		setChanged();
	}

	@Override
	public E get(int paramInt) {
		return list.get(paramInt);
	}

	@Override
	public E set(int paramInt, E paramE) {
		E object = list.set(paramInt, paramE);
		setChanged();
		return object;
	}

	@Override
	public void add(int paramInt, E paramE) {
		list.add(paramInt, paramE);
		setChanged();
	}

	@Override
	public E remove(int paramInt) {
		E object = list.remove(paramInt);
		setChanged();
		return object;
	}

	@Override
	public int indexOf(Object paramObject) {
		return list.indexOf(paramObject);
	}

	@Override
	public int lastIndexOf(Object paramObject) {
		int index = list.lastIndexOf(paramObject);
		setChanged();
		return index;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new SyncListIter(list.listIterator());
	}

	@Override
	public ListIterator<E> listIterator(int paramInt) {
		return new SyncListIter(list.listIterator(paramInt));
	}

	@Override
	public List<E> subList(int paramInt1, int paramInt2) {
		throw new UnsupportedOperationException();
	}

	private class SyncIter implements Iterator<E> {

		private final Iterator<E> iter;

		protected SyncIter(Iterator<E> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public E next() {
			return iter.next();
		}

		@Override
		public void remove() {
			iter.remove();
			setChanged();
		}
	}

	private class SyncListIter extends SyncIter implements ListIterator<E> {

		private final ListIterator<E> iter;

		protected SyncListIter(ListIterator<E> iter) {
			super(iter);
			this.iter = iter;
		}

		@Override
		public void add(E paramE) {
			iter.add(paramE);
			setChanged();
		}

		@Override
		public boolean hasPrevious() {
			return iter.hasPrevious();
		}

		@Override
		public int nextIndex() {
			return iter.nextIndex();
		}

		@Override
		public E previous() {
			return iter.previous();
		}

		@Override
		public int previousIndex() {
			return iter.previousIndex();
		}

		@Override
		public void set(E paramE) {
			iter.set(paramE);
			setChanged();
		}
	}
}
