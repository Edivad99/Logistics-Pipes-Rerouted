package logisticspipes.client.renderer.item.properties;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.MapCodec;
import org.jspecify.annotations.Nullable;

import logisticspipes.renderer.FluidContainerRenderer;

public record FluidTint() implements ItemTintSource {

    public static final FluidTint INSTANCE = new FluidTint();
    public static final MapCodec<FluidTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return FluidContainerRenderer.getFluidColor(stack);
    }

    @Override
    public MapCodec<FluidTint> type() {
        return MAP_CODEC;
    }
}
