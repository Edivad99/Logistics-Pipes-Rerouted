package logisticspipes.world.item.component;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.mojang.serialization.Codec;

import logisticspipes.LPConstants;

public class LPDataComponents {

    private static final DeferredRegister.DataComponents deferredRegister =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LPConstants.ID);

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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HUDComponent>> HUD =
        deferredRegister.registerComponentType(
            "hud",
            builder -> builder
                .persistent(HUDComponent.CODEC)
                .networkSynchronized(HUDComponent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RECIPE_TARGET =
        deferredRegister.register(
            "recipe_target",
            () -> DataComponentType.<String>builder()
                .persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                .build()
        );
}
