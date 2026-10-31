package logisticspipes.request;

import java.util.List;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.OrderTree;
import logisticspipes.request.resources.IResource;

public interface RequestLog {

	void handleMissingItems(List<IResource> resources);

	void handleSucessfullRequestOf(IResource item, @Nullable OrderTree paticipating);

	void handleSucessfullRequestOfList(List<IResource> resources, OrderTree paticipating);
}
