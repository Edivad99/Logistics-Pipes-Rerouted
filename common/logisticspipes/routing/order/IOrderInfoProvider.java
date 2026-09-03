package logisticspipes.routing.order;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public interface IOrderInfoProvider {

	/**
	 * One order, as the client's monitor shows it.
	 *
	 * <p>Encodes any order the server holds and decodes into a {@link ClientSideOrderInfo}: the
	 * client has no routers or crafting templates to hang a real order off, and only ever displays
	 * one.
	 *
	 * <p>Two of the components are groups rather than plain fields: {@link Progress} is everything
	 * that answers "how far along is it", and {@link Target} the pair that is only meaningful
	 * together -- an order either has a destination or has not, and the old format wrote both
	 * fields behind one shared boolean to say so.
	 */
	StreamCodec<RegistryFriendlyByteBuf, IOrderInfoProvider> STREAM_CODEC =
			StreamCodec.composite(
					ItemIdentifierStack.STREAM_CODEC, IOrderInfoProvider::getAsDisplayItem,
					ByteBufCodecs.VAR_INT, IOrderInfoProvider::getRouterId,
					NeoForgeStreamCodecs.enumCodec(ResourceType.class),
					IOrderInfoProvider::getType,
					Progress.STREAM_CODEC, Progress::of,
					ByteBufCodecs.optional(Target.STREAM_CODEC), Target::of,
					ClientSideOrderInfo::new);

	/** How far along an order is. */
	record Progress(boolean finished, boolean inProgress, byte machineProgress, List<Float> steps) {

		public static final StreamCodec<RegistryFriendlyByteBuf, Progress> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.BOOL, Progress::finished,
						ByteBufCodecs.BOOL, Progress::inProgress,
						ByteBufCodecs.BYTE, Progress::machineProgress,
						ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), Progress::steps,
						Progress::new);

		public static Progress of(IOrderInfoProvider order) {
			return new Progress(order.isFinished(), order.isInProgress(), order.getMachineProgress(),
					List.copyOf(order.getProgresses()));
		}
	}

	/**
	 * Where an order is headed.
	 *
	 * <p>Absent for an order that has not been assigned one yet. The two fields travel together
	 * because the old format wrote them behind one shared boolean, and reading either without the
	 * other says nothing.
	 */
	record Target(DoubleCoordinates position, ItemIdentifier type) {

		public static final StreamCodec<RegistryFriendlyByteBuf, Target> STREAM_CODEC =
				StreamCodec.composite(
						DoubleCoordinates.STREAM_CODEC, Target::position,
						ItemIdentifier.STREAM_CODEC, Target::type,
						Target::new);

		public static Optional<Target> of(IOrderInfoProvider order) {
			final DoubleCoordinates position = order.getTargetPosition();
			return position == null ? Optional.empty() : Optional.of(new Target(position, order.getTargetType()));
		}
	}

	boolean isFinished();

	ItemIdentifierStack getAsDisplayItem();

	ResourceType getType();

	int getRouterId();

	boolean isInProgress();

	boolean isWatched();

	void setWatched();

	List<Float> getProgresses();

	byte getMachineProgress();

	ItemIdentifier getTargetType();

	DoubleCoordinates getTargetPosition();


	enum ResourceType {
		PROVIDER,
		CRAFTING,
		EXTRA
	}
}
