package logisticspipes.request;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.order.OrderTree;
import logisticspipes.request.promise.IExtraPromise;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.resources.FluidResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.IHavePriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class RequestTree extends RequestTreeNode {

    public enum ActiveRequestType {
        Provide,
        Craft,
        AcceptPartial,
        SimulateOnly,
        LogMissing,
        LogUsed
    }

    public static final EnumSet<ActiveRequestType> defaultRequestFlags = EnumSet.of(ActiveRequestType.Provide,
        ActiveRequestType.Craft);

    /**
     * What the promise totals are kept by: one provider, one kind of item.
     */
    private record PromiseKey(IProvide provider, ItemIdentifier item) {}

    /**
     * Created on first use, never by an initializer: the {@link RequestTreeNode} constructor runs the whole
     * request, promises included, before this class's fields are initialized.
     */
    private @Nullable Map<PromiseKey, Integer> promisetotals;

    public RequestTree(IResource requestType, @Nullable RequestTree parent, EnumSet<ActiveRequestType> requestFlags,
        @Nullable IAdditionalTargetInformation info) {
        super(requestType, parent, requestFlags, info);
    }

    private Map<PromiseKey, Integer> promiseTotals() {
        Map<PromiseKey, Integer> totals = promisetotals;
        if (totals == null) {
            totals = new HashMap<>();
            promisetotals = totals;
        }
        return totals;
    }

    public int getAllPromissesFor(IProvide provider, ItemIdentifier item) {
        return promiseTotals().getOrDefault(new PromiseKey(provider, item), 0);
    }

    public LinkedList<IExtraPromise> getExtrasFor(IResource item) {
        HashMap<IProvide, List<IExtraPromise>> extraMap = new HashMap<>();
        checkForExtras(item, extraMap);
        removeUsedExtras(item, extraMap);
        LinkedList<IExtraPromise> extras = new LinkedList<>();
        extraMap.values().forEach(extras::addAll);
        return extras;
    }

    protected OrderTree fullFillAll() {
        return fullFill();
    }

    public void sendMissingMessage(RequestLog log) {
        Map<IResource, Integer> missing = new HashMap<>();
        buildMissingMap(missing);
        log.handleMissingItems(RequestTreeNode.shrinkToList(missing));
    }

    public void sendUsedMessage(RequestLog log) {
        Map<IResource, Integer> used = new HashMap<>();
        Map<IResource, Integer> missing = new HashMap<>();
        buildUsedMap(used, missing);
        log.handleSucessfullRequestOfList(RequestTreeNode.shrinkToList(used), new OrderTree());
        log.handleMissingItems(RequestTreeNode.shrinkToList(missing));
    }

    protected void promiseAdded(IPromise promise) {
        promiseTotals().merge(new PromiseKey(promise.getProvider(), promise.getItemType()), promise.getAmount(),
            Integer::sum);
    }

    protected void promiseRemoved(IPromise promise) {
        PromiseKey key = new PromiseKey(promise.getProvider(), promise.getItemType());
        Map<PromiseKey, Integer> totals = promiseTotals();
        int r = totals.getOrDefault(key, 0) - promise.getAmount();
        if (r == 0) {
            totals.remove(key);
        } else {
            totals.put(key, r);
        }
    }

    public static class WorkWeightedSorter implements Comparator<ExitRoute> {

        public final double distanceWeight;

        public WorkWeightedSorter(double distanceWeight) {
            this.distanceWeight = distanceWeight;
        }

        @Override
        public int compare(ExitRoute o1, ExitRoute o2) {
            int c;
            final CoreRoutedPipe firstExitPipe = o1.destination.getPipe();
            final CoreRoutedPipe secondExitPipe = o2.destination.getPipe();
            if (firstExitPipe instanceof IHavePriority) {
                if (secondExitPipe instanceof IHavePriority) {
                    c = Integer.compare(((IHavePriority) secondExitPipe).getPriority(),
                        ((IHavePriority) firstExitPipe).getPriority());
                    if (c != 0) {
                        return c;
                    }
                } else {
                    return -1;
                }
            } else {
                if (secondExitPipe instanceof IHavePriority) {
                    return 1;
                }
            }

            if (distanceWeight < 1 / 64.0) {
                c = Double.compare(firstExitPipe.getLoadFactor(), secondExitPipe.getLoadFactor());
            } else {
                c = Double.compare(
                    (firstExitPipe.getLoadFactor() * 64) + (o1.distanceToDestination * distanceWeight),
                    (secondExitPipe.getLoadFactor() * 64) + (o2.distanceToDestination * distanceWeight));
            }
            return c;
        }

    }

    public static boolean request(List<ItemIdentifierStack> items, IRequestItems requester, @Nullable RequestLog log,
        EnumSet<ActiveRequestType> requestFlags,
        @Nullable IAdditionalTargetInformation info) {
        Map<IResource, Integer> messages = new HashMap<>();
        RequestTree tree = new RequestTree(
            new ItemResource(new ItemIdentifierStack(ItemIdentifier.get(Item.BY_BLOCK.get(Blocks.STONE)), 0),
                requester), null, requestFlags, info);
        boolean isDone = true;
        for (ItemIdentifierStack stack : items) {
            ItemResource req = new ItemResource(stack, requester);
            messages.merge(req, stack.getStackSize(), Integer::sum);
            RequestTree node = new RequestTree(req, tree, requestFlags, info);
            isDone = isDone && node.isDone();
        }
        if (isDone) {
            OrderTree list = tree.fullFillAll();
            if (log != null) {
                log.handleSucessfullRequestOfList(RequestTreeNode.shrinkToList(messages), list);
            }
            return true;
        } else {
            if (log != null) {
                tree.logFailedRequestTree(log);
            }
            return false;
        }
    }

    public static int request(ItemIdentifierStack item, IRequestItems requester, @Nullable RequestLog log,
        boolean acceptPartial, boolean simulateOnly,
        boolean logMissing, boolean logUsed, EnumSet<ActiveRequestType> requestFlags,
        @Nullable IAdditionalTargetInformation info) {
        ItemResource req = new ItemResource(item, requester);
        RequestTree tree = new RequestTree(req, null, requestFlags, info);
        if (!simulateOnly && (tree.isDone() || ((tree.getPromiseAmount() > 0) && acceptPartial))) {
            OrderTree list = tree.fullFillAll();
            if (log != null) {
                log.handleSucessfullRequestOf(req.copyForDisplayWith(item.getStackSize()), list);
            }
            return tree.getPromiseAmount();
        } else {
            if (log != null) {
                if (!tree.isDone()) {
                    tree.recurseFailedRequestTree();
                }
                if (logMissing) {
                    tree.sendMissingMessage(log);
                }
                if (logUsed) {
                    tree.sendUsedMessage(log);
                }

            }
            return tree.getPromiseAmount();
        }
    }

    public static boolean request(ItemIdentifierStack item, IRequestItems requester, @Nullable RequestLog log,
        @Nullable IAdditionalTargetInformation info) {
        return
            RequestTree.request(item, requester, log, false, false, true, false, RequestTree.defaultRequestFlags, info)
                == item.getStackSize();
    }

    public static int requestPartial(ItemIdentifierStack item, IRequestItems requester,
        @Nullable IAdditionalTargetInformation info) {
        return RequestTree.request(item, requester, null, true, false, true, false, RequestTree.defaultRequestFlags,
            info);
    }

    public static int simulate(ItemIdentifierStack item, IRequestItems requester, RequestLog log) {
        return RequestTree.request(item, requester, log, true, true, false, true, RequestTree.defaultRequestFlags,
            null);
    }

    public static int requestFluidPartial(FluidIdentifier liquid, int amount, IRequestFluid pipe,
        @Nullable RequestLog log) {
        return RequestTree.requestFluid(liquid, amount, pipe, log, true);
    }

    public static boolean requestFluid(FluidIdentifier liquid, int amount, IRequestFluid pipe,
        @Nullable RequestLog log) {
        return RequestTree.requestFluid(liquid, amount, pipe, log, false) == amount;
    }

    private static int requestFluid(FluidIdentifier liquid, int amount, IRequestFluid pipe, @Nullable RequestLog log,
        boolean acceptPartial) {
        FluidResource req = new FluidResource(liquid, amount, pipe);
        RequestTree request = new RequestTree(req, null, RequestTree.defaultRequestFlags, null);
        if (request.isDone() || acceptPartial) {
            request.fullFill();
            if (log != null) {
                log.handleSucessfullRequestOf(req.copyForDisplayWith(req.getRequestedAmount()), null);
            }
            return request.getPromiseAmount();
        } else {
            if (log != null) {
                request.sendMissingMessage(log);
            }
            return request.getPromiseAmount();
        }
    }
}
