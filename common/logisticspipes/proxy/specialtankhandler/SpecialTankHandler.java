package logisticspipes.proxy.specialtankhandler;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.google.common.collect.Lists;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ISpecialTankHandler;
import logisticspipes.interfaces.ISpecialTankUtilProvider;
import logisticspipes.interfaces.ITankUtil;

public class SpecialTankHandler {

    private final List<ISpecialTankUtilProvider> tankUtilProviders = new ArrayList<>();
    private final List<ISpecialTankHandler> handlers = new ArrayList<>();

    public void registerProvider(ISpecialTankUtilProvider provider) {
        try {
            if (provider.init()) {
                tankUtilProviders.add(provider);
                LogisticsPipes.LOG.info("Loaded ISpecialTankUtilProvider: {}", provider.getClass().getName());
            } else {
                LogisticsPipes.LOG.warn("Didn't load ISpecialTankUtilProvider: {}", provider.getClass().getName());
            }
        } catch (Exception e) {
            LogisticsPipes.LOG.error("Failed to register ISpecialTankUtilProvider", e);
        }
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

    public void registerHandler(ISpecialTankHandler handler) {
        try {
            if (handler.init()) {
                handlers.add(handler);
                LogisticsPipes.LOG.info("Loaded SpecialTankHandler: {}", handler.getClass().getName());
            } else {
                LogisticsPipes.LOG.warn("Didn't load SpecialTankHandler: {}", handler.getClass().getName());
            }
        } catch (Exception e) {
            LogisticsPipes.LOG.error("Failed to register SpecialTankHandler", e);
        }
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
