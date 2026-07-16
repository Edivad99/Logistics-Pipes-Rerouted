package logisticspipes.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HUDComponent(
        boolean HUDChassie,
        boolean HUDCrafting,
        boolean HUDInvSysCon,
        boolean HUDPowerJunction,
        boolean HUDProvider,
        boolean HUDSatellite) {

    public static final HUDComponent DEFAULT = new HUDComponent(true, true, true, true, true, true);

    public static final Codec<HUDComponent> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("hud_chassie").forGetter(HUDComponent::HUDChassie),
                    Codec.BOOL.fieldOf("hud_crafting").forGetter(HUDComponent::HUDCrafting),
                    Codec.BOOL.fieldOf("hud_inv_sys_con").forGetter(HUDComponent::HUDInvSysCon),
                    Codec.BOOL.fieldOf("hud_power_junction").forGetter(HUDComponent::HUDPowerJunction),
                    Codec.BOOL.fieldOf("hud_provider").forGetter(HUDComponent::HUDProvider),
                    Codec.BOOL.fieldOf("hud_satellite").forGetter(HUDComponent::HUDSatellite)
                    ).apply(instance, HUDComponent::new));

    public static final StreamCodec<FriendlyByteBuf, HUDComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, HUDComponent::HUDChassie,
                    ByteBufCodecs.BOOL, HUDComponent::HUDCrafting,
                    ByteBufCodecs.BOOL, HUDComponent::HUDInvSysCon,
                    ByteBufCodecs.BOOL, HUDComponent::HUDPowerJunction,
                    ByteBufCodecs.BOOL, HUDComponent::HUDProvider,
                    ByteBufCodecs.BOOL, HUDComponent::HUDSatellite,
                    HUDComponent::new);
}

