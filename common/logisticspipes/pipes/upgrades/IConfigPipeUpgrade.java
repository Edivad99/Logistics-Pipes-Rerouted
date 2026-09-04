package logisticspipes.pipes.upgrades;

import logisticspipes.network.to_client.pipe.UpgradeConfigPopupMessage;

public interface IConfigPipeUpgrade extends IPipeUpgrade {

	/** Which settings popup this upgrade opens. */
	UpgradeConfigPopupMessage.Kind getConfigPopup();
}
