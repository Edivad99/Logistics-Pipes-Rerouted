package logisticspipes.request.order;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.item.ItemIdentifier;

@Accessors(chain = true)
public abstract class Order implements IOrderInfoProvider {

	private static final int MIN_DISTANCE_TO_DISPLAY = 4;

	@Getter
	private final @Nullable IAdditionalTargetInformation information;
	@Getter
	@Setter
	private boolean isFinished = false;

	/*
	 * Display Information
	 */
	@Getter
	private final ResourceType type;
	@Getter
	@Setter
	private boolean inProgress;
	@Getter
	private boolean isWatched = false;
	@Getter
	@Setter
	private byte machineProgress = 0;
	private final List<IDistanceTracker> trackers = new ArrayList<>();

	public Order(ResourceType type, @Nullable IAdditionalTargetInformation info) {
		this.type = type;
		information = info;
	}

	@Override
	public int getRouterId() {
		if (getRouter() == null) {
			return -1;
		}
		return getRouter().getSimpleID();
	}

	public abstract @Nullable IRouter getRouter();

	@Override
	public void setWatched() {
		isWatched = true;
	}

	public void addDistanceTracker(IDistanceTracker tracker) {
		trackers.add(tracker);
	}

	@Override
	public List<Float> getProgresses() {
		List<Float> progresses = new ArrayList<>();
		for (IDistanceTracker tracker : trackers) {
			if (!tracker.hasReachedDestination() && !tracker.isTimeout()) {
				float f;
				if (tracker.getInitialDistanceToTarget() != 0) {
					f = ((float) tracker.getCurrentDistanceToTarget()) / ((float) tracker.getInitialDistanceToTarget());
				} else {
					f = 1.0F;
				}
				if (!progresses.contains(f)) {
					if (tracker.getInitialDistanceToTarget() > Order.MIN_DISTANCE_TO_DISPLAY || tracker.getInitialDistanceToTarget() == 0) {
						progresses.add(f);
					}
				}
			}
		}
		return progresses;
	}

	public abstract void sendFailed();

	public abstract int getAmount();

	public abstract void reduceAmountBy(int amount);

	@Override
	public @Nullable ItemIdentifier getTargetType() {
		if (getRouter() == null || getRouter().getPipe() == null) {
			return null;
		}
		return ItemIdentifier.get(getRouter().getPipe().item);
	}

	@Override
	public @Nullable BlockPos getTargetPosition() {
		if (getRouter() == null) {
			return null;
		}
		return getRouter().getPos();
	}
}
