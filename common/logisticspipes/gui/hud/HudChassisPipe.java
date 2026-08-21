package logisticspipes.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HudChassisPipe extends BasicHUDGui {

	// Inner panel that the selected module's HUD draws into. The module renderers live in their own classes
	// but position their content against this frame, so the bounds are shared rather than duplicated.
	public static final int MODULE_PANEL_LEFT = -23;
	public static final int MODULE_PANEL_TOP = -35;
	public static final int MODULE_PANEL_RIGHT = 45;
	public static final int MODULE_PANEL_BOTTOM = 45;
	// Item grids sit one unit inside the panel edge, the way an item sits inside its 18x18 slot. The module
	// renderers used to hardcode -25, which put the first column two units *outside* the panel's left border.
	public static final int MODULE_CONTENT_LEFT = MODULE_PANEL_LEFT + 1;

	private final PipeLogisticsChassis pipe;
	private final ItemIdentifierInventory moduleInventory;

	private int selected = -1;
	private int modulePage = 0;

	private int xCursor;
	private int yCursor;

	public HudChassisPipe(PipeLogisticsChassis pipeLogisticsChassis, ItemIdentifierInventory moduleInventory) {
		pipe = pipeLogisticsChassis;
		this.moduleInventory = moduleInventory;
		for (int i = 0; i < pipe.getChassisSize(); i++) {
			addRenderableWidget(new ItemButton(moduleInventory, i, -45, -35 + ((i % 3) * 27), 20, 25));
		}

		addRenderableWidget(new BasicHUDButton("<", -45, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return !isSlotSelected();
			}

			@Override
			public void clicked() {
				modulePage--;
			}

			@Override
			public boolean buttonEnabled() {
				return modulePage > 0;
			}
		});
		addRenderableWidget(new BasicHUDButton(">", -33, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return !isSlotSelected();
			}

			@Override
			public void clicked() {
				modulePage++;
			}

			@Override
			public boolean buttonEnabled() {
				return modulePage < ((pipe.getChassisSize() - 1) / 3);
			}
		});
		addRenderableWidget(new BasicHUDButton("x", 37, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return isSlotSelected();
			}

			@Override
			public void clicked() {
				resetSelection();
			}

			@Override
			public boolean buttonEnabled() {
				return true;
			}
		});
	}

	@Override
	public void renderHeadUpDisplay(GuiGraphics guiGraphics, double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, -50, -50, 50, 50, 0, false);
		super.renderHeadUpDisplay(guiGraphics, distance, day, shifted, minecraft, config);
		int textColor = day ? 0xff404040 : 0xff7f7f7f;
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule == null) {
				return;
			}

			LPGuiGraphics.drawGuiBackGround(guiGraphics, MODULE_PANEL_LEFT, MODULE_PANEL_TOP, MODULE_PANEL_RIGHT,
					MODULE_PANEL_BOTTOM, 0, false);

			if (selectedmodule instanceof IHUDModuleHandler && ((IHUDModuleHandler) selectedmodule).getHUDRenderer() != null) {
				((IHUDModuleHandler) selectedmodule).getHUDRenderer().renderContent(guiGraphics, shifted);
				if (((IHUDModuleHandler) selectedmodule).getHUDRenderer().getButtons() != null) {
					for (IHUDButton button : ((IHUDModuleHandler) selectedmodule).getHUDRenderer().getButtons()) {
						button.renderAlways(guiGraphics, shifted);
						if (button.shouldRenderButton()) {
							button.renderButton(guiGraphics, button.isFocused(), button.isblockFocused(), shifted);
						}
						if (!button.buttonEnabled() || !button.shouldRenderButton()) {
							continue;
						}
						if ((button.getX() - 1 < (xCursor - 11) && (xCursor - 11) < (button.getX() + button.sizeX() + 1)) && (button.getY() - 1 < (yCursor - 5) && (yCursor - 5) < (button.getY() + button.sizeY() + 1))) {
							if (!button.isFocused() && !button.isblockFocused()) {
								button.setFocused();
							} else if (button.focusedTime() > 400 && !button.isblockFocused()) {
								button.clicked();
								button.blockFocused();
							}
						} else if (button.isFocused() || button.isblockFocused()) {
							button.clearFocused();
						}
					}
				}
			} else {
				guiGraphics.drawString(minecraft.font, "Nothing", -5, -15, textColor, false);
				guiGraphics.drawString(minecraft.font, "to", 9, -5, textColor, false);
				guiGraphics.drawString(minecraft.font, "display", -5, 5, textColor, false);
			}
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, pipe.displayList, null, 0, -15, -35, 3, 12, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		if (!config.isChassisHUD()) {
			return false;
		}
		for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
			if (moduleInventory.getIDStackInSlot(i) != null) {
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -50 < x && x < 50 && -50 < y && y < 50;
	}

	@Override
	public void handleCursor(int x, int y) {
		super.handleCursor(x, y);
		xCursor = x;
		yCursor = y;
	}

	private void moduleClicked(int number) {
		selected = number;
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule instanceof IHUDModuleHandler) {
				((IHUDModuleHandler) selectedmodule).startHUDWatching();
			}
		}
	}

	private void resetSelection() {
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule instanceof IHUDModuleHandler) {
				((IHUDModuleHandler) selectedmodule).stopHUDWatching();
			}
		}
		selected = -1;
	}

	private boolean isSlotSelected() {
		return selected != -1;
	}

	private boolean isSlotSelected(int number) {
		return selected == number;
	}

	private boolean shouldDisplayButton(int number) {
		return modulePage * 3 <= number && number < (modulePage + 1) * 3;
	}

	public void stopWatching() {
		resetSelection();
	}

	private class ItemButton extends BasicHUDButton {

		private ItemIdentifierInventory inv;
		private int position;

		public ItemButton(ItemIdentifierInventory inv, int position, int x, int y, int width, int height) {
			super("item." + position, x, y, width, height);
			this.inv = inv;
			this.position = position;
		}

		@Override
		public void clicked() {
			moduleClicked(position);
		}

		@Override
		public void renderButton(GuiGraphics guiGraphics, boolean hover, boolean clicked, boolean shifted) {

			if (shifted || hover || isSlotSelected(position)) {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1F);
			} else {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
			}

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0F, 0.0F, BasicHUDButton.BUTTON_Z);
			guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
			if (isSlotSelected(position)) {
				LPGuiGraphics.drawGuiBackGround(guiGraphics, posX * 2, posY * 2, (posX + sizeX) * 2 + 19, (posY + sizeY) * 2, 0, false, true, true, true, false);
			} else {
				LPGuiGraphics.drawGuiBackGround(guiGraphics, posX * 2, posY * 2, (posX + sizeX) * 2, (posY + sizeY) * 2, 0, false);
			}
			guiGraphics.pose().popPose();

			ItemIdentifierStack module = inv.getIDStackInSlot(position);

			if (module != null) {
				boolean renderInColor = buttonEnabled() || isSlotSelected(position);
				ItemStackRenderer itemStackRenderer = new ItemStackRenderer(posX + ((sizeX - 16) / 2), posY + ((sizeY - 16) / 2), -0.002F, shifted, renderInColor);
				itemStackRenderer.setItemIdentStack(module).setDisplayAmount(DisplayAmount.NEVER);

                itemStackRenderer.renderInGui(guiGraphics);
			}
		}

		@Override
		public void renderAlways(GuiGraphics guiGraphics, boolean shifted) {
			if (inv.getIDStackInSlot(position) == null && shouldDisplayButton(position)) {
				if (shifted) {
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				} else {
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.3F);
				}
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, 0.0F, BasicHUDButton.BUTTON_Z);
                guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
				LPGuiGraphics.drawGuiBackGround(guiGraphics, posX * 2, posY * 2, (posX + sizeX) * 2, (posY + sizeY) * 2, 0, false);
                guiGraphics.pose().popPose();
			}
		}

		@Override
		public boolean shouldRenderButton() {
			return inv.getIDStackInSlot(position) != null && shouldDisplayButton(position);
		}

		@Override
		public boolean buttonEnabled() {
			return !isSlotSelected();
		}
	}
}
