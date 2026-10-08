package logisticspipes.integrations.probe;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What Jade and The One Probe show for a pipe: finished lines, which each mod only has to draw.
 *
 * <p>The settings they describe only exist on the server. The One Probe asks there; Jade draws on
 * the client, so it sends the lines across with {@link #STREAM_CODEC}.
 */
public record PipeTooltip(List<Line> lines) {

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeTooltip> STREAM_CODEC =
        Line.STREAM_CODEC.apply(ByteBufCodecs.list()).map(PipeTooltip::new, PipeTooltip::lines);

    /** Item icons followed by text. Either may be empty. */
    public record Line(List<ItemStack> icons, Component text) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Line::icons,
            ComponentSerialization.STREAM_CODEC, Line::text,
            Line::new);
    }
}
