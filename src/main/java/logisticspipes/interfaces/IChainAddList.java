package logisticspipes.interfaces;

import java.util.List;

/**
 * A list that hands back what was added, so a widget can be collected while it is being registered.
 */
public interface IChainAddList<T> extends List<T> {

    /**
     * @return {@code element}
     */
    T addChain(T element);
}
