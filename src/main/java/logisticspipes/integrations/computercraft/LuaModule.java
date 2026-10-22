package logisticspipes.integrations.computercraft;

import java.util.HashMap;
import java.util.Map;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import org.jspecify.annotations.Nullable;

import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.SimpleFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * A module in a pipe, for the computer that asked for it. Slots count from 1, as Lua does.
 */
public final class LuaModule {

    private final LogisticsGenericPipeBlockEntity blockEntity;
    private final LogisticsModule module;
    private final int computerId;

    private LuaModule(LogisticsGenericPipeBlockEntity blockEntity, LogisticsModule module, int computerId) {
        this.blockEntity = blockEntity;
        this.module = module;
        this.computerId = computerId;
    }

    static @Nullable LuaModule of(CoreRoutedPipe pipe, @Nullable LogisticsModule module, int computerId) {
        return module == null ? null : new LuaModule(pipe.getContainer(), module, computerId);
    }

    @LuaFunction(mainThread = true)
    public String getName() throws LuaException {
        check();
        return module.getLPName();
    }

    @LuaFunction(mainThread = true)
    public boolean hasFilter() throws LuaException {
        check();
        return filter() != null;
    }

    /**
     * The filter's items by slot; empty slots are left out.
     */
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, Object>> getFilter() throws LuaException {
        IItemIdentifierInventory filter = requireFilter();
        Map<Integer, Map<String, Object>> items = new HashMap<>();
        for (int slot = 0; slot < filter.getContainerSize(); slot++) {
            ItemIdentifierStack stack = filter.getIDStackInSlot(slot);
            if (stack != null) {
                items.put(slot + 1, LuaItems.toLua(stack));
            }
        }
        return items;
    }

    /**
     * Puts an item in a filter slot, or empties the slot when no item is given.
     */
    @LuaFunction(mainThread = true)
    public void setFilterSlot(IArguments arguments) throws LuaException {
        IItemIdentifierInventory filter = requireFilter();
        int slot = arguments.getInt(0);
        if (slot < 1 || slot > filter.getContainerSize()) {
            throw new LuaException("Slot " + slot + " is not between 1 and " + filter.getContainerSize());
        }
        Object spec = arguments.get(1);
        if (spec == null) {
            filter.clearInventorySlotContents(slot - 1);
        } else {
            CoreRoutedPipe pipe = PipeMethods.routedPipe(blockEntity);
            filter.setItem(slot - 1, LuaItems.resolve(spec, PipeMethods.networkItems(pipe)).makeStack(1));
        }
    }

    @LuaFunction(mainThread = true)
    public boolean isDefaultRoute() throws LuaException {
        return requireItemSink().isDefaultRoute();
    }

    @LuaFunction(mainThread = true)
    public void setDefaultRoute(boolean defaultRoute) throws LuaException {
        requireItemSink().setDefaultRoute(defaultRoute);
    }

    private void check() throws LuaException {
        PipeMethods.checkAccess(PipeMethods.routedPipe(blockEntity), computerId);
    }

    private @Nullable IItemIdentifierInventory filter() {
        return module instanceof SimpleFilter simpleFilter
            && simpleFilter.getFilterInventory() instanceof IItemIdentifierInventory inventory ? inventory : null;
    }

    private IItemIdentifierInventory requireFilter() throws LuaException {
        check();
        IItemIdentifierInventory filter = filter();
        if (filter == null) {
            throw new LuaException("The " + module.getLPName() + " module has no filter");
        }
        return filter;
    }

    private ModuleItemSink requireItemSink() throws LuaException {
        check();
        if (!(module instanceof ModuleItemSink itemSink)) {
            throw new LuaException("Only an item sink has a default route");
        }
        return itemSink;
    }
}
