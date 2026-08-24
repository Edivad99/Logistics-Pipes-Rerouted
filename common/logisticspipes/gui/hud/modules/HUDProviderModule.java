package logisticspipes.gui.hud.modules;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDProviderModule implements IHUDModuleRenderer {

	private List<IHUDButton> buttons = new ArrayList<>();

	private int page = 0;

	private final ModuleProvider module;

	public HUDProviderModule(ModuleProvider moduleProvider) {
		buttons.add(new BasicHUDButton("<", 8, -35, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public void clicked() {
				page--;
			}

			@Override
			public boolean buttonEnabled() {
				return page > 0;
			}
		});
		buttons.add(new BasicHUDButton(">", 20, -35, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public void clicked() {
				page++;
			}

			@Override
			public boolean buttonEnabled() {
				return page + 1 < getMaxPage();
			}
		});
		module = moduleProvider;
	}

	public int getMaxPage() {
		int ret = module.displayList.size() / 9;
		if (module.displayList.size() % 9 != 0 || ret == 0) {
			ret++;
		}
		return ret;
	}

	@Override
	public void renderContent(HUDDrawContext context, boolean shifted) {
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, module.displayList, null, page, HudChassisPipe.MODULE_CONTENT_LEFT, -24, 3, 9, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return buttons;
	}
}
