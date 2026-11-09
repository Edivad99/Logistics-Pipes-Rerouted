package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.promise.FluidPromise;
import logisticspipes.utils.FluidIdentifier;

public class FluidOrderManager extends OrderManager<FluidOrder, FluidIdentifier> {

    /**
     * Where the orders are saved in the pipe's data.
     */
    public static final String SAVE_KEY = "fluidOrders";

    private final SavedOrders<SavedFluidOrder> savedOrders = new SavedOrders<>();

    private static class IC implements OrderQueue.IIdentityProvider<FluidOrder, FluidIdentifier> {

        @Override
        public FluidIdentifier getIdentity(FluidOrder o) {
            return o.getFluid();
        }

        @Override
        public boolean isExtra(FluidOrder o) {
            return false;
        }
    }

    public FluidOrderManager(ILPPositionProvider pos) {
        super(new OrderQueue<>(new IC()), pos);
    }

    @Override
    public void sendFailed() {
        orders.getFirst().sendFailed();
        super.sendFailed();
    }

    public FluidOrder addOrder(FluidPromise promise, IRequestFluid destination, ResourceType type,
        @Nullable IAdditionalTargetInformation info) {
        if (promise.amount < 0) {
            throw new IllegalArgumentException("The amount can't be less than zero");
        }
        FluidOrder order = new FluidOrder(promise.liquid, promise.amount, destination, type, info);
        orders.addLast(order);
        listen();
        return order;
    }

    public int totalFluidsCountInOrders(FluidIdentifier fluid) {
        int itemCount = 0;
        for (FluidOrder request : orders) {
            if (!request.getFluid().equals(fluid)) {
                continue;
            }
            itemCount += request.getAmount();
        }
        return itemCount;
    }

    /* Kept across a reload */

    public void serialize(ValueOutput output) {
        List<SavedFluidOrder> toSave = new ArrayList<>(savedOrders.pending());
        for (FluidOrder order : orders) {
            SavedFluidOrder saved = SavedFluidOrder.of(order);
            if (saved != null) {
                toSave.add(saved);
            }
        }
        if (!toSave.isEmpty()) {
            output.store(SAVE_KEY, SavedFluidOrder.CODEC.listOf(), toSave);
        }
    }

    public void deserialize(ValueInput input) {
        savedOrders.load(input.read(SAVE_KEY, SavedFluidOrder.CODEC.listOf()).orElse(List.of()));
    }

    /**
     * Turns the orders read back from the saved world into live ones, as the pipes they go to load.
     * Called on the server every tick.
     */
    public void restoreSavedOrders() {
        savedOrders.restore(this::restore, pos);
    }

    private boolean restore(SavedFluidOrder saved) {
        CoreRoutedPipe pipe = saved.target().findPipe();
        if (!(pipe instanceof IRequestFluid destination)) {
            return false;
        }
        orders.addLast(new FluidOrder(saved.fluid(), saved.amount(), destination, saved.type(),
            saved.target().information()));
        listen();
        return true;
    }

    /**
     * An order as saved with the world.
     */
    private record SavedFluidOrder(FluidIdentifier fluid, int amount, ResourceType type,
        SavedOrders.Target target) {

        static final Codec<SavedFluidOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.CODEC.xmap(FluidIdentifier::get, fluid -> fluid.makeFluidStack(1))
                .fieldOf("fluid").forGetter(SavedFluidOrder::fluid),
            Codec.INT.fieldOf("amount").forGetter(SavedFluidOrder::amount),
            Codec.STRING.xmap(ResourceType::valueOf, ResourceType::name)
                .fieldOf("type").forGetter(SavedFluidOrder::type),
            SavedOrders.Target.CODEC.fieldOf("target").forGetter(SavedFluidOrder::target)
        ).apply(instance, SavedFluidOrder::new));

        /**
         * The order as it can be saved, or null when it is done or going nowhere.
         */
        static @Nullable SavedFluidOrder of(FluidOrder order) {
            if (order.isFinished() || order.getAmount() <= 0) {
                return null;
            }
            SavedOrders.Target target = SavedOrders.Target.of(order.getRouter(), order.getInformation());
            if (target == null || target.router().isEmpty()) {
                return null;
            }
            return new SavedFluidOrder(order.getFluid(), order.getAmount(), order.getType(), target);
        }
    }
}
