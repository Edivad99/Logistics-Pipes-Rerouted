package logisticspipes.integrations.computercraft;

import java.util.BitSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * Turns routing pipes into CC: Tweaked peripherals, adds methods to the solid blocks, and carries the
 * messages computers send each other over a logistics network.
 *
 * <p>A computer next to a routing pipe can query and order from the network it belongs to; see
 * {@link PipeMethods} for what it can call, and {@link SolidBlockMethods} for the blocks. A computer
 * receives {@value #MESSAGE_EVENT} and {@value #BROADCAST_EVENT} events with the sending computer's
 * id and the message.</p>
 */
public final class ComputerCraftIntegration {

    public static final String MESSAGE_EVENT = "lp_message";
    public static final String BROADCAST_EVENT = "lp_broadcast";

    private ComputerCraftIntegration() {
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LPConstants.ID);

    /**
     * One peripheral per pipe, whichever side a computer is on: it keeps the computers attached to
     * the pipe, which is where messages for them are delivered. Never saved: computers attach again
     * when the world loads.
     */
    private static final Supplier<AttachmentType<PipePeripheral>> PIPE_PERIPHERAL =
        ATTACHMENT_TYPES.register("computer_peripheral", () -> AttachmentType.builder(
            holder -> new PipePeripheral((LogisticsGenericPipeBlockEntity) holder)).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static void registerGenericMethods() {
        ComputerCraftAPI.registerGenericSource(new SolidBlockMethods());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), LPBlockEntityTypes.PIPE.get(),
            (blockEntity, side) -> pipePeripheral(blockEntity));
    }

    private static @Nullable PipePeripheral pipePeripheral(LogisticsGenericPipeBlockEntity blockEntity) {
        return blockEntity.pipe instanceof CoreRoutedPipe ? blockEntity.getData(PIPE_PERIPHERAL) : null;
    }

    static void sendMessage(CoreRoutedPipe from, int sourceId, int targetId, @Nullable Object message) {
        forEachPipe(from, pipe -> computersAt(pipe, computer -> {
            if (computer.getID() == targetId) {
                computer.queueEvent(MESSAGE_EVENT, sourceId, message);
            }
        }));
    }

    static void sendBroadcast(CoreRoutedPipe from, int sourceId, @Nullable Object message) {
        forEachPipe(from, pipe -> computersAt(pipe, computer -> computer.queueEvent(BROADCAST_EVENT, sourceId, message)));
    }

    /**
     * Runs {@code action} on every pipe of the network, each once, as the routers get to it.
     */
    private static void forEachPipe(CoreRoutedPipe from, Consumer<CoreRoutedPipe> action) {
        BitSet seen = new BitSet();
        for (ExitRoute exit : from.getRouter().getIRoutersByCost()) {
            IRouter destination = exit.destination;
            if (!seen.get(destination.getSimpleID())) {
                seen.set(destination.getSimpleID());
                destination.queueTask(10, (pipe, router) -> action.accept(pipe));
            }
        }
    }

    private static void computersAt(CoreRoutedPipe pipe, Consumer<IComputerAccess> action) {
        PipePeripheral peripheral = pipe.getContainer().getExistingDataOrNull(PIPE_PERIPHERAL);
        if (peripheral != null) {
            peripheral.computers.forEach(action);
        }
    }
}
