package logisticspipes.integrations.computercraft;

import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * A routing pipe as a peripheral. Its type is the pipe's item id, with {@value #PIPE_TYPE} added so
 * scripts can find any routing pipe; its methods depend on the kind of pipe.
 */
final class PipePeripheral implements IDynamicPeripheral {

    static final String PIPE_TYPE = LPConstants.rl("pipe").toString();

    private final LogisticsGenericPipeBlockEntity blockEntity;
    private final String type;
    private final List<PipeMethods.Method> methods;
    private final String[] methodNames;
    final AttachedComputerSet computers = new AttachedComputerSet();

    /**
     * @param blockEntity holding a routing pipe
     */
    PipePeripheral(LogisticsGenericPipeBlockEntity blockEntity) {
        CoreRoutedPipe pipe = (CoreRoutedPipe) blockEntity.pipe;
        this.blockEntity = blockEntity;
        this.type = BuiltInRegistries.ITEM.getKey(pipe.item).toString();
        this.methods = PipeMethods.forPipe(pipe);
        this.methodNames = methods.stream().map(PipeMethods.Method::name).toArray(String[]::new);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of(PIPE_TYPE);
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @Override
    public Object getTarget() {
        return blockEntity;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof PipePeripheral peripheral && peripheral.blockEntity == blockEntity;
    }

    @Override
    public String[] getMethodNames() {
        return methodNames;
    }

    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments)
        throws LuaException {
        return PipeMethods.invoke(methods.get(method), blockEntity, computer.getID(), context, arguments);
    }
}
