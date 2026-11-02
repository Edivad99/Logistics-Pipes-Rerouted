package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class OrderTree extends ArrayList<IOrderInfoProvider> {

    /**
     * An order and everything it had to be broken down into, all the way down.
     *
     * <p>Recursive because the structure is: a request for a crafted item becomes requests for its
     * ingredients, which may themselves be crafted.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, OrderTree> STREAM_CODEC =
        StreamCodec.recursive(self -> StreamCodec.composite(
            IOrderInfoProvider.STREAM_CODEC.apply(ByteBufCodecs.list()), List::copyOf,
            self.apply(ByteBufCodecs.list()), OrderTree::getSubOrders,
            OrderTree::new));

    @Getter
    private final List<OrderTree> subOrders;

    /**
     * Null until first asked for; see {@link #getList()}.
     */
    private @Nullable List<IOrderInfoProvider> cachedList;

    /**
     * Null until first asked for; see {@link #getProgresses()}.
     */
    private @Nullable List<Float> cachedProgress;

    public OrderTree() {
        subOrders = new ArrayList<>();
    }

    public OrderTree(List<IOrderInfoProvider> orders, List<OrderTree> subOrders) {
        addAll(orders);
        this.subOrders = new ArrayList<>(subOrders);
    }

    /**
     * Every order in the tree, this list's own first and then each sub-list's, flattened.
     *
     * <p>Built once and kept: the monitor asks for it every frame. Nothing adds to a list after it
     * has been shown, so there is no cache to invalidate.
     */
    public List<IOrderInfoProvider> getList() {
        List<IOrderInfoProvider> flattened = cachedList;
        if (flattened == null) {
            flattened = new ArrayList<>(this);
            for (OrderTree sub : subOrders) {
                flattened.addAll(sub.getList());
            }
            cachedList = flattened;
        }
        return flattened;
    }

    public int getTreeRootSize() {
        int subSize = 0;
        for (OrderTree sub : subOrders) {
            subSize += sub.getTreeRootSize();
        }
        return Math.max(size(), subSize);
    }

    public int getSubTreeRootSize() {
        int subSize = 0;
        for (OrderTree sub : subOrders) {
            subSize += sub.getTreeRootSize();
        }
        return subSize;
    }

    public void setWatched() {
        this.forEach(IOrderInfoProvider::setWatched);
        subOrders.forEach(OrderTree::setWatched);
    }

    /**
     * The distinct progress points of this list's own orders, in the order first seen.
     */
    public List<Float> getProgresses() {
        List<Float> distinct = cachedProgress;
        if (distinct == null) {
            distinct = new ArrayList<>();
            for (IOrderInfoProvider order : this) {
                for (Float point : order.getProgresses()) {
                    if (!distinct.contains(point)) {
                        distinct.add(point);
                    }
                }
            }
            cachedProgress = distinct;
        }
        return distinct;
    }
}
