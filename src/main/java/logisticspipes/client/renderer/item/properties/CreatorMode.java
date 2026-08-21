package logisticspipes.client.renderer.item.properties;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import logisticspipes.world.item.ItemPipeSignCreator;

public record CreatorMode() implements RangeSelectItemModelProperty {

    public static final CreatorMode INSTANCE = new CreatorMode();
    public static final MapCodec<CreatorMode> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return ItemPipeSignCreator.getMode(stack);
    }

    @Override
    public MapCodec<CreatorMode> type() {
        return MAP_CODEC;
    }
}
