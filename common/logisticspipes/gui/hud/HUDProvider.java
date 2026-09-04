package logisticspipes.gui.hud;

import net.minecraft.client.Minecraft;

import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDProvider extends BasicHUDGui {

	private final PipeItemsProviderLogistics pipe;
	private int page = 0;
	private int pageB = 0;

	public HUDProvider(final PipeItemsProviderLogistics pipe) {
		this.pipe = pipe;
		addRenderableWidget(new BasicHUDButton("<", -2, -50, 8, 8) {

			@Override
			public void clicked() {
				if (page > 0) {
					page--;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public boolean buttonEnabled() {
				return page > 0;
			}
		});
		addRenderableWidget(new BasicHUDButton(">", 37, -50, 8, 8) {

			@Override
			public void clicked() {
				if (page + 1 < getMaxPage()) {
					page++;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public boolean buttonEnabled() {
				return page + 1 < getMaxPage();
			}
		});
		addRenderableWidget(new BasicHUDButton("<", -2, 21, 8, 8) {

			@Override
			public void clicked() {
				if (pageB > 0) {
					pageB--;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public boolean buttonEnabled() {
				return pageB > 0;
			}
		});
		addRenderableWidget(new BasicHUDButton(">", 37, 21, 8, 8) {

			@Override
			public void clicked() {
				if (pageB + 1 < getMaxPageOrderer()) {
					pageB++;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return true;
			}

			@Override
			public boolean buttonEnabled() {
				return pageB + 1 < getMaxPageOrderer();
			}
		});
	}

	@Override
	public void renderHeadUpDisplay(HUDDrawContext context, double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.drawGuiBackGround(context, -50, -55, 50, 55, 0, false);
		super.renderHeadUpDisplay(context, distance, day, shifted, minecraft, config);

		float scaleX = 1.125F;
		float scaleY = 1.125F;
		float scaleZ = -0.0001F;
		ItemStackRenderer itemStackRenderer = new ItemStackRenderer(0, 0, 0.0F, shifted, true);
		itemStackRenderer.setDisplayAmount(DisplayAmount.ALWAYS);
		itemStackRenderer.setScaleX(scaleX).setScaleY(scaleY).setScaleZ(scaleZ);
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, pipe.getDisplayList(), null, page, -36, -37, 4, 12, 18, 18, itemStackRenderer);
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, pipe.itemListOrderer, null, pageB, -36, 23, 4, 4, 18, 18, itemStackRenderer);
		int textColor = day ? 0xFF404040 : 0xff7f7f7f;
		context.drawString(minecraft.font, String.format("(%d/%d)", page + 1, getMaxPage()), 9, -50, textColor, false);
		context.drawString(minecraft.font, String.format("(%d/%d)", pageB + 1, getMaxPageOrderer()), 9, 23, textColor, false);
	}

	public int getMaxPage() {
		int ret = pipe.getDisplayList().size() / 12;
		if (pipe.getDisplayList().size() % 12 != 0 || ret == 0) {
			ret++;
		}
		return ret;
	}

	public int getMaxPageOrderer() {
		int ret = pipe.itemListOrderer.size() / 4;
		if (pipe.itemListOrderer.size() % 4 != 0 || ret == 0) {
			ret++;
		}
		return ret;
	}

	@Override
	public boolean display(IHUDConfig config) {
		return pipe.getDisplayList().size() > 0 && config.isHUDProvider();
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -50 < x && x < 50 && -55 < y && y < 55;
	}
}
