package logisticspipes.world.item.component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HUDComponent(
    boolean chassis,
    boolean crafting,
    boolean invSysCon,
    boolean powerJunction,
    boolean provider,
    boolean satellite) {

    public static final HUDComponent DEFAULT = new HUDComponent(true, true, true, true, true, true);

    /** Every panel off, for the routing debug HUD, which brings its own panels. */
    public static final HUDComponent NONE = new HUDComponent(false, false, false, false, false, false);

    public static final Codec<HUDComponent> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("chassis").forGetter(HUDComponent::chassis),
            Codec.BOOL.fieldOf("crafting").forGetter(HUDComponent::crafting),
            Codec.BOOL.fieldOf("inv_sys_con").forGetter(HUDComponent::invSysCon),
            Codec.BOOL.fieldOf("power_junction").forGetter(HUDComponent::powerJunction),
            Codec.BOOL.fieldOf("provider").forGetter(HUDComponent::provider),
            Codec.BOOL.fieldOf("satellite").forGetter(HUDComponent::satellite)
        ).apply(instance, HUDComponent::new));

    public static final StreamCodec<FriendlyByteBuf, HUDComponent> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, HUDComponent::chassis,
            ByteBufCodecs.BOOL, HUDComponent::crafting,
            ByteBufCodecs.BOOL, HUDComponent::invSysCon,
            ByteBufCodecs.BOOL, HUDComponent::powerJunction,
            ByteBufCodecs.BOOL, HUDComponent::provider,
            ByteBufCodecs.BOOL, HUDComponent::satellite,
            HUDComponent::new);

    public boolean get(HudSetting setting) {
        return switch (setting) {
            case CHASSIS -> chassis;
            case CRAFTING -> crafting;
            case INV_SYS_CON -> invSysCon;
            case POWER_JUNCTION -> powerJunction;
            case PROVIDER -> provider;
            case SATELLITE -> satellite;
        };
    }

    public HUDComponent with(HudSetting setting, boolean enabled) {
        return new HUDComponent(
            setting == HudSetting.CHASSIS ? enabled : chassis,
            setting == HudSetting.CRAFTING ? enabled : crafting,
            setting == HudSetting.INV_SYS_CON ? enabled : invSysCon,
            setting == HudSetting.POWER_JUNCTION ? enabled : powerJunction,
            setting == HudSetting.PROVIDER ? enabled : provider,
            setting == HudSetting.SATELLITE ? enabled : satellite);
    }
}
