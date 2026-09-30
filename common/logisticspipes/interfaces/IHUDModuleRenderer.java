package logisticspipes.interfaces;

import java.util.List;

import logisticspipes.client.renderer.hud.HUDDrawContext;

public interface IHUDModuleRenderer {

	void renderContent(HUDDrawContext context, boolean shifted);

	List<IHUDButton> getButtons();
}
