package logisticspipes.gui.hud.modules;

import java.util.ArrayList;
import java.util.List;
import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;

public class HUDAdvancedExtractor implements IHUDModuleRenderer {

	private final List<IHUDButton> buttons = new ArrayList<>();
	private final AsyncAdvancedExtractor module;
	private int selected = 0;

	public HUDAdvancedExtractor(AsyncAdvancedExtractor moduleAdvancedExtractor) {
		module = moduleAdvancedExtractor;
		buttons.add(new TabButton("Side", 0, -30, -50, 25, 10));
		buttons.add(new TabButton("Inv", 1, -5, -50, 25, 10));
	}

	@Override
	public void renderContent(GuiGraphics gg, boolean shifted) {
		Minecraft mc = Minecraft.getInstance();
		if (selected == 0) {
			Direction d = module.getSneakyDirection();
			String label = "Sneaky: " + (d == null ? "Default" : d.getName());
			gg.drawString(mc.font, label, -mc.font.width(label) / 2, -30, 0xff404040, false);
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(gg,
					ItemIdentifierStack.getListFromInventory(module.getFilterInventory()), null, 0, HudChassisPipe.MODULE_CONTENT_LEFT, -32, 3, 9, 18,
					18, 100.0F, DisplayAmount.NEVER, false, shifted);
			gg.drawString(mc.font, "Filter", -mc.font.width("Filter") / 2, 25, 0xff404040, false);
		}
	}

	@Override
	public List<IHUDButton> getButtons() {
		return buttons;
	}

	private class TabButton extends BasicHUDButton {

		private final int mode;

		public TabButton(String name, int mode, int x, int y, int width, int height) {
			super(name, x, y, width, height);
			this.mode = mode;
		}

		@Override
		public void clicked() {
			selected = mode;
		}

		@Override
		public void renderButton(GuiGraphics gg, boolean hover, boolean clicked, boolean shifted) {
			Minecraft mc = Minecraft.getInstance();
			gg.pose().pushPose();
			gg.pose().translate(0.0F, 0.0F, BUTTON_Z);
			LPGuiGraphics.drawGuiBackGround(
                    gg,
                    posX * 2,
					posY * 2,
					(posX + sizeX) * 2,
					(posY + sizeY) * 2 + 15,
					0,
					false,
					true,
					true,
					false,
					true);

			int color;
			if (hover && !clicked) {
				color = Color.getValue(Color.LIGHT_YELLOW);
			} else if (!clicked) {
				color = Color.getValue(Color.BLACK);
			} else {
				color = Color.getValue(Color.DARK_GREY);
			}
			gg.pose().translate(0.0F, 0.0F, BUTTON_LABEL_Z);
			int tx = -(mc.font.width(label) / 2) + posX + sizeX / 2;
			int ty = posY + (sizeY - 8) / 2 + 2;
			gg.drawString(mc.font, label, tx, ty, color, false);
			gg.pose().popPose();
		}

		@Override
		public boolean shouldRenderButton() {
			return true;
		}

		@Override
		public boolean buttonEnabled() {
			return mode != selected;
		}
	}
}
