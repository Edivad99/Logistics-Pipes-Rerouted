package logisticspipes.api.network.debug;

import org.jspecify.annotations.Nullable;

public interface IObjectIdentification {

	boolean toStringObject(Object o);

	/**
	 * @param o
	 * @return null, if object isn't handled, otherwise the String value
	 */
	@Nullable
	String handleObject(Object o);

}
