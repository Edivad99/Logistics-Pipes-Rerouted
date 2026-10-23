package logisticspipes.util;

import java.time.Duration;
import java.util.Arrays;

import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.logistics.LogisticsManager;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifier;

public class TrackingTask {

    /** How often a sample is taken. */
    public static final int TICKS_PER_SAMPLE = SharedConstants.TICKS_PER_MINUTE;

    /** How far back the graph reaches. */
    private static final Duration HISTORY = Duration.ofHours(24);

    private static final int SAMPLES =
        (int) (HISTORY.toSeconds() * SharedConstants.TICKS_PER_SECOND / TICKS_PER_SAMPLE);

    public static final Codec<TrackingTask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("item").forGetter(task -> task.item.makeNormalStack(1)),
        Codec.INT.optionalFieldOf("arrayPos", 0).forGetter(task -> task.arrayPos),
        Codec.LONG_STREAM.fieldOf("amountRecorded").forGetter(task -> Arrays.stream(task.amountRecorded))
    ).apply(instance, (stack, arrayPos, recorded) ->
        new TrackingTask(ItemIdentifier.get(stack), arrayPos, recorded.toArray())));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingTask> STREAM_CODEC =
        StreamCodec.of((buffer, task) -> {
            ItemIdentifier.STREAM_CODEC.encode(buffer, task.item);
            buffer.writeVarInt(task.arrayPos);
            buffer.writeLongArray(task.amountRecorded);
        }, buffer -> new TrackingTask(
            ItemIdentifier.STREAM_CODEC.decode(buffer),
            buffer.readVarInt(),
            buffer.readLongArray()));

    public final ItemIdentifier item;
    public final long[] amountRecorded = new long[SAMPLES];
    public int arrayPos = 0;

    /** A task that has recorded nothing yet. */
    public TrackingTask(ItemIdentifier item) {
        this.item = item;
    }

    /**
     * A task read back from disk or off the network.
     *
     * <p>Takes as many samples as fit, so a run saved with a different history length still loads
     * rather than throwing.
     */
    private TrackingTask(ItemIdentifier item, int arrayPos, long[] recorded) {
        this.item = item;
        this.arrayPos = arrayPos;
        System.arraycopy(recorded, 0, amountRecorded, 0, Math.min(recorded.length, amountRecorded.length));
    }

    /** Takes a sample now, overwriting the oldest one once the history is full. */
    public void record(CoreRoutedPipe pipe) {
        amountRecorded[arrayPos++] = LogisticsManager.getAmountFor(item, pipe.getRouter().getIRoutersByCost());
        if (arrayPos >= amountRecorded.length) {
            arrayPos = 0;
        }
    }
}
