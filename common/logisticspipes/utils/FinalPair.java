package logisticspipes.utils;

/**
 * A final pair that caches hashcode and implements equals, mainly for use as
 * hashmap key
 */
public class FinalPair<T1, T2> {

	private final T1 value1;
	private final T2 value2;
	private final int hashcode;

	public FinalPair(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
		hashcode = this.value1.hashCode() ^ this.value2.hashCode();
	}

	public T1 getValue1() {
		return value1;
	}

	public T2 getValue2() {
		return value2;
	}

	@Override
	public String toString() {
		return String.format("<%s,%s>", value1, value2);
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FinalPair)) {
			return false;
		}
		FinalPair<?, ?> p = (FinalPair<?, ?>) o;
		return value1.equals(p.value1) && value2.equals(p.value2);
	}
}
