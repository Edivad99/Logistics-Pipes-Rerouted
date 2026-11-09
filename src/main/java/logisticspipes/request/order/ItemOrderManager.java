package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.inventory.IExternalCrafter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.resources.DictResource;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemOrderManager extends OrderManager<ItemOrder, DictResource.Identifier> {

    /**
     * Where the orders are saved in the pipe's data.
     */
    public static final String SAVE_KEY = "itemOrders";

    private final SavedOrders<SavedItemOrder> savedOrders = new SavedOrders<>();

    private static class IC implements OrderQueue.IIdentityProvider<ItemOrder, DictResource.Identifier> {

        @Override
        public DictResource.Identifier getIdentity(ItemOrder o) {
            return o.getResource().getIdentifier();
        }

        @Override
        public boolean isExtra(ItemOrder o) {
            return o instanceof ExtraItemOrder;
        }
    }

    private static class ExtraItemOrder extends ItemOrder {

        public ExtraItemOrder(DictResource item, @Nullable IRequestItems destination, ResourceType type,
            @Nullable IAdditionalTargetInformation info) {
            super(item, destination, type, info);
        }
    }

    public ItemOrderManager(ILPPositionProvider pos) {
        super(new OrderQueue<>(new IC()), pos);
    }

    public ItemOrderManager(IChangeListener listener, ILPPositionProvider pos) {
        super(listener, pos, new OrderQueue<>(new IC()));
    }

    @Override
    public void sendFailed() {
        orders.getFirst().sendFailed();
        super.sendFailed();
    }

    public ItemOrder addOrder(ItemIdentifierStack stack, IRequestItems requester, ResourceType type,
        @Nullable IAdditionalTargetInformation info) {
        ItemOrder order = new ItemOrder(new DictResource(stack, null), requester, type, info);
        orders.addLast(order);
        listen();
        return order;
    }

    public ItemOrder addOrder(DictResource stack, IRequestItems requester, ResourceType type,
        @Nullable IAdditionalTargetInformation info) {
        ItemOrder order = new ItemOrder(stack, requester, type, info);
        orders.addLast(order);
        listen();
        return order;
    }

    public ExternalCraftingOrder addExternalCraftingOrder(ItemIdentifierStack stack, IRequestItems requester,
        @Nullable IAdditionalTargetInformation info, int ownerSlot, IExternalCrafter.Job job) {
        ExternalCraftingOrder order =
            new ExternalCraftingOrder(new DictResource(stack, null), requester, info, ownerSlot, job, false);
        orders.addLast(order);
        listen();
        return order;
    }

    public void addExtra(DictResource stack) {
        orders.addLast(new ExtraItemOrder(stack, null, ResourceType.EXTRA, null));
        listen();
    }

    public void removeExtras(DictResource resource) {
        int itemsToRemove = resource.getRequestedAmount();
        DictResource.Identifier ident = resource.getIdentifier();
        Iterator<ItemOrder> iter = orders.iterator();
        List<ItemOrder> toRemove = new LinkedList<>();
        while (iter.hasNext()) {
            ItemOrder order = iter.next();
            if (order.getType() != ResourceType.EXTRA) {
                continue;
            }
            if (order.getResource().getIdentifier().equals(ident)) {
                if (itemsToRemove >= order.getAmount()) {
                    itemsToRemove -= order.getAmount();
                    toRemove.add(order);
                    if (itemsToRemove == 0) {
                        orders.removeAll(toRemove);
                        return;
                    }
                } else {
                    order.getResource().getItemStack().setStackSize(order.getAmount() - itemsToRemove);
                    break;
                }
            }
        }
        orders.removeAll(toRemove);
    }

    public int totalItemsCountInOrders(ItemIdentifier item) {
        int itemCount = 0;
        for (ItemOrder request : orders) {
            if (!request.getResource().getItem().equals(item)) {
                continue;
            }
            itemCount += request.getResource().stack.getStackSize();
        }
        return itemCount;
    }

    /* Kept across a reload */

    public void serialize(ValueOutput output) {
        List<SavedItemOrder> toSave = new ArrayList<>(savedOrders.pending());
        for (ItemOrder order : orders) {
            SavedItemOrder saved = SavedItemOrder.of(order);
            if (saved != null) {
                toSave.add(saved);
            }
        }
        if (!toSave.isEmpty()) {
            output.store(SAVE_KEY, SavedItemOrder.CODEC.listOf(), toSave);
        }
    }

    public void deserialize(ValueInput input) {
        savedOrders.load(input.read(SAVE_KEY, SavedItemOrder.CODEC.listOf()).orElse(List.of()));
    }

    /**
     * Turns the orders read back from the saved world into live ones, as the pipes they go to load.
     * Called on the server every tick.
     */
    public void restoreSavedOrders() {
        savedOrders.restore(this::restore, pos);
    }

    private boolean restore(SavedItemOrder saved) {
        IRequestItems destination = null;
        if (saved.type() != ResourceType.EXTRA) {
            CoreRoutedPipe pipe = saved.target().findPipe();
            if (pipe == null) {
                return false;
            }
            destination = pipe;
        }
        DictResource resource = new DictResource(new ItemIdentifierStack(saved.item(), saved.amount()), destination)
            .loadFromBitSet(BitSet.valueOf(new long[] { saved.fuzzyFlags() }));
        IAdditionalTargetInformation info = saved.target().information();
        ItemOrder order;
        if (destination == null) {
            order = new ExtraItemOrder(resource, null, ResourceType.EXTRA, null);
        } else if (saved.externalOwner().isPresent()) {
            order = new ExternalCraftingOrder(resource, destination, info, saved.externalOwner().get(), null,
                saved.accepted());
        } else {
            order = new ItemOrder(resource, destination, saved.type(), info);
        }
        orders.addLast(order);
        listen();
        return true;
    }

    /**
     * An order as saved with the world.
     *
     * @param externalOwner for an order handed to an autocrafting system, the slot of the provider
     *                      module it belongs to
     * @param accepted      whether that system had taken it on
     */
    private record SavedItemOrder(ItemIdentifier item, int amount, int fuzzyFlags, ResourceType type,
        SavedOrders.Target target, Optional<Integer> externalOwner, boolean accepted) {

        static final Codec<SavedItemOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.xmap(ItemIdentifier::get, item -> item.makeNormalStack(1))
                .fieldOf("item").forGetter(SavedItemOrder::item),
            Codec.INT.fieldOf("amount").forGetter(SavedItemOrder::amount),
            Codec.INT.optionalFieldOf("fuzzyFlags", 0).forGetter(SavedItemOrder::fuzzyFlags),
            Codec.STRING.xmap(ResourceType::valueOf, ResourceType::name)
                .fieldOf("type").forGetter(SavedItemOrder::type),
            SavedOrders.Target.CODEC.fieldOf("target").forGetter(SavedItemOrder::target),
            Codec.INT.optionalFieldOf("externalOwner").forGetter(SavedItemOrder::externalOwner),
            Codec.BOOL.optionalFieldOf("accepted", false).forGetter(SavedItemOrder::accepted)
        ).apply(instance, SavedItemOrder::new));

        /**
         * The order as it can be saved, or null when it is done, going nowhere, or turned down.
         */
        static @Nullable SavedItemOrder of(ItemOrder order) {
            if (order.isFinished() || order.getAmount() <= 0) {
                return null;
            }
            SavedOrders.Target target = SavedOrders.Target.of(order.getRouter(), order.getInformation());
            if (target == null || (order.getType() != ResourceType.EXTRA && target.router().isEmpty())) {
                return null;
            }
            Optional<Integer> externalOwner = Optional.empty();
            boolean accepted = false;
            if (order instanceof ExternalCraftingOrder external) {
                if (external.hasFailed()) {
                    return null;
                }
                externalOwner = Optional.of(external.getOwnerSlot());
                accepted = external.isAccepted();
            }
            long[] flags = order.getResource().getBitSet().toLongArray();
            return new SavedItemOrder(order.getResource().getItem(), order.getAmount(),
                flags.length == 0 ? 0 : (int) flags[0], order.getType(), target, externalOwner, accepted);
        }
    }
}
