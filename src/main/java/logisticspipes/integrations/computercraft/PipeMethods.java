package logisticspipes.integrations.computercraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestHandler;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * What a script can call on a routing pipe. The same table backs the peripheral of the pipe a
 * computer touches and the pipes it reaches through {@code getPipeForUUID}, so each call carries the
 * id of the computer making it: the security station decides by that id.
 *
 * <p>Every call runs on the server thread. They all read the routing tables or the world, which
 * only that thread may touch.</p>
 */
final class PipeMethods {

    @FunctionalInterface
    interface Body {
        @Nullable Object @Nullable [] call(CoreRoutedPipe pipe, int computerId, IArguments arguments)
            throws LuaException;
    }

    /**
     * @param checked whether the security station must allow the computer first
     */
    record Method(String name, boolean checked, Body body) {}

    private PipeMethods() {
    }

    static List<Method> forPipe(CoreRoutedPipe pipe) {
        List<Method> methods = new ArrayList<>();
        methods.add(new Method("getRouterId", true, (p, computer, args) -> values(p.getRouterId())));
        methods.add(new Method("getRouterUUID", true,
            (p, computer, args) -> values(p.getRouter().getId().toString())));
        methods.add(new Method("canAccess", false, (p, computer, args) -> values(canAccess(p, computer))));
        methods.add(new Method("sendMessage", true, (p, computer, args) -> {
            ComputerCraftIntegration.sendMessage(p, computer, args.getInt(0), args.get(1));
            return null;
        }));
        methods.add(new Method("sendBroadcast", true, (p, computer, args) -> {
            ComputerCraftIntegration.sendBroadcast(p, computer, args.get(0));
            return null;
        }));
        methods.add(new Method("getPipeForUUID", true, PipeMethods::getPipeForUUID));
        methods.add(new Method("getModule", true,
            (p, computer, args) -> values(LuaModule.of(p, p.getLogisticsModule(), computer))));

        if (pipe instanceof PipeLogisticsChassis) {
            methods.add(new Method("getChassisSize", true,
                (p, computer, args) -> values(((PipeLogisticsChassis) p).getChassisSize())));
            methods.add(new Method("getModuleInSlot", true, (p, computer, args) -> {
                PipeLogisticsChassis chassis = (PipeLogisticsChassis) p;
                int slot = args.getInt(0);
                if (slot < 1 || slot > chassis.getChassisSize()) {
                    throw new LuaException("Slot " + slot + " is not between 1 and " + chassis.getChassisSize());
                }
                return values(LuaModule.of(p, chassis.getSubModule(slot - 1), computer));
            }));
        }
        if (pipe instanceof PipeItemsRequestLogistics) {
            methods.add(new Method("getAvailableItems", true, (p, computer, args) -> {
                List<Map<String, Object>> items = new ArrayList<>();
                availableItems(p).forEach((item, count) -> items.add(LuaItems.toLua(item, count)));
                return values(items);
            }));
            methods.add(new Method("getCraftableItems", true, (p, computer, args) -> {
                List<Map<String, Object>> items = new ArrayList<>();
                craftableItems(p).forEach(item -> items.add(LuaItems.toLua(item, 0)));
                return values(items);
            }));
            methods.add(new Method("getItemAmount", true, (p, computer, args) -> {
                ItemIdentifier item = LuaItems.resolve(args.get(0), networkItems(p));
                return values(availableItems(p).getOrDefault(item, 0));
            }));
            methods.add(new Method("makeRequest", true, PipeMethods::makeRequest));
        }
        if (pipe instanceof PipeItemsCraftingLogistics) {
            methods.add(new Method("reimport", true, (p, computer, args) -> {
                ((PipeItemsCraftingLogistics) p).reimport();
                return null;
            }));
        }
        return methods;
    }

    static MethodResult invoke(Method method, LogisticsGenericPipeBlockEntity blockEntity, int computerId,
        ILuaContext context, IArguments arguments) throws LuaException {
        arguments.escapes();
        return context.executeMainThreadTask(() -> {
            CoreRoutedPipe pipe = routedPipe(blockEntity);
            if (method.checked()) {
                checkAccess(pipe, computerId);
            }
            return method.body().call(pipe, computerId, arguments);
        });
    }

    static CoreRoutedPipe routedPipe(LogisticsGenericPipeBlockEntity blockEntity) throws LuaException {
        if (blockEntity.isRemoved() || !(blockEntity.pipe instanceof CoreRoutedPipe pipe)) {
            throw new LuaException("The pipe is no longer there");
        }
        return pipe;
    }

    static void checkAccess(CoreRoutedPipe pipe, int computerId) throws LuaException {
        if (!canAccess(pipe, computerId)) {
            throw new LuaException("The security station does not allow this computer");
        }
    }

    private static boolean canAccess(CoreRoutedPipe pipe, int computerId) {
        ISecurityProvider security = pipe.getSecurityProvider();
        return security == null || security.getAllowCC(computerId);
    }

    static Object[] values(@Nullable Object... values) {
        return values;
    }

    private static Object @Nullable [] getPipeForUUID(CoreRoutedPipe pipe, int computerId, IArguments arguments)
        throws LuaException {
        if (!pipe.getUpgradeManager().hasCCRemoteControlUpgrade()) {
            throw new LuaException("This pipe needs a remote control upgrade");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(arguments.getString(0));
        } catch (IllegalArgumentException e) {
            throw new LuaException("Not a UUID: " + arguments.getString(0));
        }
        int id = SimpleServiceLocator.routerManager.getIDforUUID(uuid);
        IRouter router = SimpleServiceLocator.routerManager.getRouter(id);
        CoreRoutedPipe target = router == null ? null : router.getPipe();
        return values(target == null ? null : new RemotePipe(target.getContainer(), computerId));
    }

    /**
     * Returns the outcome ({@code "DONE"}, {@code "MISSING"} or {@code "NO_POWER"}) and the items it
     * concerns: what was ordered, or what the network lacks.
     */
    private static Object[] makeRequest(CoreRoutedPipe pipe, int computerId, IArguments arguments)
        throws LuaException {
        ItemIdentifier item = LuaItems.resolve(arguments.get(0), networkItems(pipe));
        int amount = arguments.getInt(1);
        if (amount < 1) {
            throw new LuaException("Amount must be at least 1");
        }
        boolean craftOnly = arguments.optBoolean(2, false);
        Object[] status = RequestHandler.computerRequest(item.makeStack(amount), pipe, craftOnly);
        List<Map<String, Object>> items = new ArrayList<>();
        if (status.length > 1 && status[1] instanceof Collection<?> resources) {
            for (Object resource : resources) {
                items.add(LuaItems.toLua(((IResource) resource).getDisplayItem()));
            }
        }
        return values(status[0], items);
    }

    private static Map<ItemIdentifier, Integer> availableItems(CoreRoutedPipe pipe) {
        return SimpleServiceLocator.logisticsManager.getAvailableItems(pipe.getRouter().getIRoutersByCost());
    }

    private static List<ItemIdentifier> craftableItems(CoreRoutedPipe pipe) {
        return SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost());
    }

    /**
     * Everything the network can hand out, stored or crafted: where a fingerprint can be looked up.
     */
    static Supplier<Collection<ItemIdentifier>> networkItems(CoreRoutedPipe pipe) {
        return () -> {
            Set<ItemIdentifier> items = new HashSet<>(availableItems(pipe).keySet());
            items.addAll(craftableItems(pipe));
            return items;
        };
    }
}
