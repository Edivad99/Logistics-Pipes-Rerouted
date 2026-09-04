package logisticspipes.pipes.basic.debug;

import java.util.Collection;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One line of a pipe's debug status, with the lines nested under it.
 */
public record StatusEntry(String name, List<StatusEntry> subEntries) {

    public static final StreamCodec<RegistryFriendlyByteBuf, StatusEntry> STREAM_CODEC =
            StreamCodec.recursive(self -> StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, StatusEntry::name,
                    self.apply(ByteBufCodecs.list()), StatusEntry::subEntries,
                    StatusEntry::new));

    public StatusEntry(String name) {
        this(name, List.of());
    }

    /** An entry whose children are the string forms of {@code parts}. */
    public static StatusEntry of(String name, Collection<?> parts) {
        return new StatusEntry(name, parts.stream().map(part -> new StatusEntry(String.valueOf(part))).toList());
    }
}
