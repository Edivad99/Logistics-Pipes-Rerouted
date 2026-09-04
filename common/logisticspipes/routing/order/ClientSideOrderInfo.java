package logisticspipes.routing.order;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * An order as the client holds it: everything the monitor draws, and nothing else.
 *
 * <p>The server's own orders hang off a router and a crafting template, neither of which exists on
 * the client, so what arrives is flattened into this.
 */
public class ClientSideOrderInfo implements IOrderInfoProvider {

	@Getter
	private final ItemIdentifierStack asDisplayItem;
	@Getter
	private final int routerId;
	@Getter
	private final ResourceType type;
	@Getter
	private final boolean isFinished;
	@Getter
	private final boolean inProgress;
	@Getter
	private final byte machineProgress;
	@Getter
	private final List<Float> progresses;
	@Getter
	private final @Nullable DoubleCoordinates targetPosition;
	@Getter
	private final @Nullable ItemIdentifier targetType;

	/** Always false: a client cannot ask to watch an order it only receives. */
	@Getter
	private final boolean isWatched = false;

	public ClientSideOrderInfo(ItemIdentifierStack asDisplayItem, int routerId, ResourceType type,
			Progress progress, Optional<Target> target) {
		this.asDisplayItem = asDisplayItem;
		this.routerId = routerId;
		this.type = type;
		isFinished = progress.finished();
		inProgress = progress.inProgress();
		machineProgress = progress.machineProgress();
		progresses = progress.steps();
		targetPosition = target.map(Target::position).orElse(null);
		targetType = target.map(Target::type).orElse(null);
	}

	/** Ignored: watching is decided on the server, and this order is a copy. */
	@Override
	public void setWatched() {}
}
