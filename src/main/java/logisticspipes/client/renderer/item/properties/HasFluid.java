package logisticspipes.client.renderer.item.properties;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import logisticspipes.utils.FluidIdentifier;

public record HasFluid() implements ConditionalItemModelProperty {

    public static final HasFluid INSTANCE = new HasFluid();
    public static final MapCodec<HasFluid> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed,
        ItemDisplayContext context) {
        return FluidIdentifier.get(stack) != null;
    }

    @Override
    public MapCodec<HasFluid> type() {
        return MAP_CODEC;
    }
}
