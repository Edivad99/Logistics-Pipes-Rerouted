package logisticspipes.blocks.stats;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.ItemStackLoader;
import logisticspipes.utils.item.ItemIdentifier;

public class TrackingTask {

	public int everyNthTick = 20 * 60;
	public ItemIdentifier item;
	public int arrayPos = 0;
	public long[] amountRecorded = new long[1440]; //24h with 20ticks and 60sec

	public void tick(int tickCount, CoreRoutedPipe pipe) {
		if (tickCount % everyNthTick != 0) {
			return;
		}
		amountRecorded[arrayPos++] = SimpleServiceLocator.logisticsManager.getAmountFor(item, pipe.getRouter().getIRoutersByCost());
		if (arrayPos >= amountRecorded.length) {
			arrayPos = 0;
		}
	}

	public void deserialize(ValueInput input) {
		int[] amountRecorded_A = input.getIntArray("amountRecorded_A").orElse(new int[0]);
		int[] amountRecorded_B = input.getIntArray("amountRecorded_B").orElse(new int[0]);
		for (int i = 0; i < amountRecorded.length; i++) {
			if (i >= amountRecorded_A.length || i >= amountRecorded_B.length) {
				break;
			}
			amountRecorded[i] = (((long) amountRecorded_B[i]) << 32) | amountRecorded_A[i];
		}
		arrayPos = input.getIntOr("arrayPos", 0);
		item = ItemIdentifier.get(ItemStackLoader.loadItemStack(input));
	}

	public void serialize(ValueOutput output) {
		int[] amountRecorded_A = new int[amountRecorded.length];
		int[] amountRecorded_B = new int[amountRecorded.length];
		for (int i = 0; i < amountRecorded.length; i++) {
			amountRecorded_A[i] = (int) amountRecorded[i];
			amountRecorded_B[i] = (int) (amountRecorded[i] >> 32);
		}
		output.putIntArray("amountRecorded_A", amountRecorded_A);
		output.putIntArray("amountRecorded_B", amountRecorded_B);
		output.putInt("arrayPos", arrayPos);
		ItemStackLoader.saveItemStack(output, item.makeNormalStack(1));
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, TrackingTask> STREAM_CODEC =
			StreamCodec.of((buffer, task) -> {
				ItemIdentifier.STREAM_CODEC.encode(buffer, task.item);
				buffer.writeVarInt(task.arrayPos);
				buffer.writeLongArray(task.amountRecorded);
			}, buffer -> {
				TrackingTask task = new TrackingTask();
				task.item = ItemIdentifier.STREAM_CODEC.decode(buffer);
				task.arrayPos = buffer.readVarInt();
				final long[] recorded = buffer.readLongArray();
				System.arraycopy(recorded, 0, task.amountRecorded, 0,
						Math.min(recorded.length, task.amountRecorded.length));
				return task;
			});

}
