package logisticspipes.integrations.computercraft;

import java.util.List;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * A pipe reached through {@code getPipeForUUID}: the same methods as its peripheral, for the computer
 * that asked for it.
 */
final class RemotePipe implements IDynamicLuaObject {

    private final LogisticsGenericPipeBlockEntity blockEntity;
    private final int computerId;
    private final List<PipeMethods.Method> methods;
    private final String[] methodNames;

    RemotePipe(LogisticsGenericPipeBlockEntity blockEntity, int computerId) throws LuaException {
        CoreRoutedPipe pipe = PipeMethods.routedPipe(blockEntity);
        this.blockEntity = blockEntity;
        this.computerId = computerId;
        this.methods = PipeMethods.forPipe(pipe);
        this.methodNames = methods.stream().map(PipeMethods.Method::name).toArray(String[]::new);
    }

    @Override
    public String[] getMethodNames() {
        return methodNames;
    }

    @Override
    public MethodResult callMethod(ILuaContext context, int method, IArguments arguments) throws LuaException {
        return PipeMethods.invoke(methods.get(method), blockEntity, computerId, context, arguments);
    }
}
