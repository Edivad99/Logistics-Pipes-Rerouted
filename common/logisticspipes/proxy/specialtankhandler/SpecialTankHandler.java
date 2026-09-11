package logisticspipes.proxy.specialtankhandler;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.google.common.collect.Lists;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.provider.ISpecialTankHandler;
import logisticspipes.api.event.RegisterTankHandlersEvent;
import logisticspipes.api.provider.ISpecialTankUtilProvider;
import logisticspipes.api.util.ITankUtil;

public class SpecialTankHandler {

    private final List<ISpecialTankUtilProvider> tankUtilProviders;
    private final List<ISpecialTankHandler> handlers;

    private SpecialTankHandler(List<ISpecialTankUtilProvider> tankUtilProviders, List<ISpecialTankHandler> handlers) {
        this.tankUtilProviders = tankUtilProviders;
        this.handlers = handlers;
    }

    /** Collects everything registered on {@code event}, which is closed from here on. */
    public static SpecialTankHandler from(RegisterTankHandlersEvent event) {
        return new SpecialTankHandler(event.registeredProviders(), event.registeredHandlers());
    }

    /**
     * An {@link ITankUtil} from a provider that claims {@code blockEntity}, or null so the caller falls back
     * to the block's fluid-handler capability.
     */
    @Nullable
    public ITankUtil getSpecialTankUtilFor(@Nullable BlockEntity blockEntity, @Nullable Direction dir) {
        if (blockEntity == null) {
            return null;
        }
        for (ISpecialTankUtilProvider provider : tankUtilProviders) {
            if (provider.isType(blockEntity, dir)) {
                ITankUtil util = provider.getTankUtilFor(blockEntity, dir);
                if (util != null) {
                    return util;
                }
            }
        }
        return null;
    }

    /**
     * Whether a provider claims {@code blockEntity}, without building the util. Storage networks have
     * no fluid-handler capability to find them by, so callers that gate on "is this a tank at all"
     * -- {@code FluidRoutedPipe#isConnectableTank} above all -- have to ask here as well.
     */
    public boolean hasSpecialTankUtilFor(@Nullable BlockEntity blockEntity, @Nullable Direction dir) {
        if (blockEntity == null) {
            return false;
        }
        for (ISpecialTankUtilProvider provider : tankUtilProviders) {
            if (provider.isType(blockEntity, dir)) {
                return true;
            }
        }
        return false;
    }

    public List<BlockEntity> getBaseTileFor(BlockEntity blockEntity) {
        for (ISpecialTankHandler handler : handlers) {
            if (handler.isType(blockEntity)) {
                return handler.getBaseTilesFor(blockEntity);
            }
        }
        return Lists.newArrayList(blockEntity);
    }

    public boolean hasHandlerFor(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        for (ISpecialTankHandler handler : handlers) {
            if (handler.isType(blockEntity)) {
                return true;
            }
        }
        return false;
    }

    public ISpecialTankHandler getTankHandlerFor(@Nullable BlockEntity blockEntity) {
        for (ISpecialTankHandler handler : handlers) {
            if (handler.isType(blockEntity)) {
                return handler;
            }
        }
        String name = "null";
        if (blockEntity != null) {
            name = blockEntity.getClass().getName();
        }
        throw new RuntimeException("Unknown TankBlockEntity Request, '" + name + "'");
    }
}
