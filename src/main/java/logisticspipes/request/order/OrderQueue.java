package logisticspipes.request.order;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OrderQueue<E extends Order, I> implements Iterable<E> {

    private final IIdentityProvider<E, I> identifyer;
    private final Map<I, Integer> extraSize = new HashMap<>();
    private final LinkedList<E> list = new LinkedList<>();
    private final List<E> unmodifiable = Collections.unmodifiableList(list);
    private int globalExtraCount = 0;

    public OrderQueue(IIdentityProvider<E, I> identifyer) {
        this.identifyer = identifyer;
    }

    public E getFirst() {
        return list.getFirst();
    }

    public void addLast(E order) {
        list.addLast(order);
        I ident = identifyer.getIdentity(order);
        if (identifyer.isExtra(order)) {
            extraSize.merge(ident, 1, Integer::sum);
            globalExtraCount++;
        } else if (extraSize.getOrDefault(ident, 0) > 0) {
            List<E> toMove = new LinkedList<>();
            for (E lElem : list) {
                if (identifyer.isExtra(lElem) && ident.equals(identifyer.getIdentity(lElem))) {
                    toMove.add(lElem);
                }
            }
            for (E move : toMove) {
                list.remove(move);
                list.addLast(move);
            }
        }
    }

    public void removeAll(List<E> orders) {
        list.removeAll(orders);
        for (E order : orders) {
            elemRemove(order);
        }
    }

    private void elemRemove(E elem) {
        if (identifyer.isExtra(elem)) {
            I ident = identifyer.getIdentity(elem);
            int prev = extraSize.getOrDefault(ident, 0);
            if (prev > 0) {
                extraSize.put(ident, prev - 1);
            }
            globalExtraCount--;
        }
    }

    public int size() {
        return list.size();
    }

    @Override
    public Iterator<E> iterator() {
        return unmodifiable.iterator();
    }

    public boolean hasExtras() {
        return globalExtraCount != 0;
    }

    public E removeFirst() {
        E elem = list.removeFirst();
        elemRemove(elem);
        return elem;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public interface IIdentityProvider<A, B> {

        B getIdentity(A o);

        boolean isExtra(A o);
    }
}
