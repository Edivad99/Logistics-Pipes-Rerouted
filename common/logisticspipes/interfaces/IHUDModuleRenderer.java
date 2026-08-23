package logisticspipes.interfaces;

import logisticspipes.renderer.HUDDrawContext;
import java.util.List;


public interface IHUDModuleRenderer {

	void renderContent(HUDDrawContext context, boolean shifted);

	List<IHUDButton> getButtons();
}
