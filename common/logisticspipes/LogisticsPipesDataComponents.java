package logisticspipes;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class LogisticsPipesDataComponents {
    private static final DeferredRegister.DataComponents deferredRegister =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LPConstants.LP_MOD_ID);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> UUID =
            deferredRegister.register(
                    "uuid",
                    () -> DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build()
            );
}
