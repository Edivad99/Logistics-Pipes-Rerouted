package logisticspipes.logic.gui;


import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.logic.LogicController;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

// import net.minecraft.client.gui.Gui; // removed — Gui is HUD class in 1.20.1, not GUI base

public class LogicLayoutGui extends LogisticsBaseGuiScreen {

	private enum ZOOM_LEVEL {
		NORMAL(1, 165, 224, 1, 0),
		LEVEL_1(0.5F, 330, 465, 1, 50),
		LEVEL_2(0.25F, 660, 950, 2, 100);

		ZOOM_LEVEL(float zoom, int bottom, int right, int line, int moveY) {
			this.zoom = zoom;
			bottomRenderBorder = bottom;
			rightRenderBorder = right;
			this.line = line;
			this.moveY = moveY;
		}

		final float zoom;
		final int bottomRenderBorder;
		final int rightRenderBorder;
		final int line;
		final int moveY;

		ZOOM_LEVEL next() {
			int id = ordinal();
			if (id + 1 >= ZOOM_LEVEL.values().length) {
				return this;
			} else {
				return ZOOM_LEVEL.values()[id + 1];
			}
		}

		ZOOM_LEVEL prev() {
			int id = ordinal();
			if (id - 1 < 0) {
				return this;
			} else {
				return ZOOM_LEVEL.values()[id - 1];
			}
		}
	}

	private static final ResourceLocation achievementTextures = ResourceLocation.withDefaultNamespace("textures/gui/achievement/achievement_background.png");

	private final ItemRenderer renderItem = Minecraft.getInstance().getItemRenderer();

	private int isMouseButtonDown;
	private int mouseX;
	private int mouseY;
	private double guiMapX;
	private double guiMapY;
	private ZOOM_LEVEL zoom = ZOOM_LEVEL.NORMAL;


	public LogicLayoutGui(LogicController controller, Player player) {
		super(256, 202 + 90, 0, 0);
		guiMapY = -200;
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addNormalSlotsForPlayerInventory(50, 205);
		// inventorySlots removed in 1.20.1 — menu set via super constructor
	}

	@Override
	public void init() {
		super.init();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		isMouseButtonDown = 0;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY < 0) {
			zoom = zoom.next();
		} else if (scrollY > 0) {
			zoom = zoom.prev();
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		int k = (width - imageWidth) / 2;
		int l = (height - imageHeight) / 2;
		if (mouseX >= k + 8 && mouseX < k + 8 + 224 && mouseY >= l + 17 && mouseY < l + 17 + 155) {
			guiMapX -= dx / zoom.zoom;
			guiMapY -= dy / zoom.zoom;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
		super.renderBg(guiGraphics, f, i, j);
		drawTransparentBack(guiGraphics);
		drawMap(guiGraphics, i, j);
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos + 180, right, bottom, 0.0f, true, false, true, true, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 50, topPos + 205);
	}

	private void drawTransparentBack(GuiGraphics guiGraphics) {
		SimpleGraphics.drawGradientRect(guiGraphics, 0, 0, width, height, Color.BLANK, Color.BLANK, 0.0);
	}

	private void drawMap(GuiGraphics guiGraphics, int par1, int par2) {
		int leftSide = ((width - imageWidth) / 2);
		int topSide = ((height - imageHeight) / 2);

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LogicLayoutGui.achievementTextures, leftSide, topSide, 0.0f, 0.0f, 256, 202, 256, 256, ARGB.colorFromFloat(1.0F, 0.7F, 0.7F, 0.7F));

		topPos = (int) (topPos * 1 / zoom.zoom);
		leftPos = (int) (leftPos * 1 / zoom.zoom);
		imageWidth = (int) (imageWidth * 1 / zoom.zoom);
		imageHeight = (int) (imageHeight * 1 / zoom.zoom);
		leftSide *= 1 / zoom.zoom;
		topSide *= 1 / zoom.zoom;

		topPos = (int) (topPos * zoom.zoom);
		leftPos = (int) (leftPos * zoom.zoom);
		imageWidth = (int) (imageWidth * zoom.zoom);
		imageHeight = (int) (imageHeight * zoom.zoom);
		leftSide *= zoom.zoom;
		topSide *= zoom.zoom;

		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LogicLayoutGui.achievementTextures, leftSide, topSide, 0.0f, 0.0f, 256, 202, 256, 256);
	}

	private void renderLinkedOrderListItems(GuiGraphics guiGraphics, LinkedLogisticsOrderList list, int xPos, int yPos, int par1, int par2) {
		int size = list.size();
		int startLeft = -(size - 1) * (30 / 2) + xPos;
		yPos += 13;
		for (IOrderInfoProvider aList : list) {
			int badgeTint = aList.isInProgress() ? ARGB.colorFromFloat(1.0F, 0.1F, 0.9F, 0.1F) : ARGB.colorFromFloat(1.0F, 0.7F, 0.7F, 0.7F);
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LogicLayoutGui.achievementTextures, startLeft - 5, yPos - 5, 0.0f, 202.0f, 26, 26, 256, 256, badgeTint);
			//renderItemAt(aList.getAsDisplayItem(), startLeft, yPos);
			if (aList.isInProgress() && aList.getMachineProgress() != 0) {
				guiGraphics.fill(startLeft - 4, yPos + 20, startLeft + 20, yPos + 24, 0xff000000);
				guiGraphics.fill(startLeft - 3, yPos + 21, startLeft + 19, yPos + 23, 0xffffffff);
				guiGraphics.fill(startLeft - 3, yPos + 21, startLeft - 3 + (22 * aList.getMachineProgress() / 100), yPos + 23, 0xffff0000);
			}
			// A hover tooltip was built here but never rendered -- this class has no renderToolTips
			// and nothing ever read the field back -- so it only cost an ItemStack allocation per
			// frame while hovering. Removed rather than wired up; see RequestMonitorPopup, which
			// does render the same kind of order tooltip.
			startLeft += 30;
		}
		startLeft = xPos + 20 - list.getSubTreeRootSize() * (40 / 2);
		if (!list.getSubOrders().isEmpty()) {
			for (int i = 0; i < list.getSubOrders().size(); i++) {
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
				renderLinkedOrderListItems(guiGraphics, list.getSubOrders().get(i), startLeft - 20, yPos + 48, par1, par2);
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
			}
		}
	}

	private void renderLinkedOrderListLines(GuiGraphics guiGraphics, LinkedLogisticsOrderList list, int xPos, int yPos) {
		int size = list.size();
		if (list.isEmpty()) {
			size = 1;
		}
		int startLeft = -(size - 1) * (30 / 2) + xPos;
		yPos += 13;
		int left = startLeft;
		for (int i = 0; i < list.size(); i++) {
			SimpleGraphics.drawVerticalLine(guiGraphics, startLeft + 8, yPos - 13, yPos - 3, Color.GREEN, zoom.line);
			if (!list.getSubOrders().isEmpty()) {
				SimpleGraphics.drawVerticalLine(guiGraphics, startLeft + 8, yPos + 18, yPos + 28, Color.GREEN, zoom.line);
			}
			startLeft += 30;
		}
		if (!list.isEmpty()) {
			SimpleGraphics.drawHorizontalLine(guiGraphics, left + 8, startLeft - 22, yPos - 13, Color.GREEN, zoom.line);
		}
		if (!list.getSubOrders().isEmpty()) {
			if (!list.isEmpty()) {
				SimpleGraphics.drawHorizontalLine(guiGraphics, left + 8, startLeft - 22, yPos + 28, Color.GREEN, zoom.line);
				startLeft -= 30;
			}
			SimpleGraphics.drawVerticalLine(guiGraphics, left + ((startLeft - left) / 2) + 8, yPos + 28, yPos + 38, Color.GREEN, zoom.line);
			startLeft = xPos + 20 - list.getSubTreeRootSize() * (40 / 2);
			left = startLeft;
			for (int i = 0; i < list.getSubOrders().size(); i++) {
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
				SimpleGraphics.drawVerticalLine(guiGraphics, startLeft - 12, yPos + 38, yPos + 48, Color.GREEN, zoom.line);
				drawPointFor(guiGraphics, list, xPos, yPos, i, startLeft);
				renderLinkedOrderListLines(guiGraphics, list.getSubOrders().get(i), startLeft - 20, yPos + 48);
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
			}
			if (!list.getSubOrders().isEmpty()) {
				left += list.getSubOrders().get(0).getTreeRootSize() * (40 / 2);
				startLeft -= list.getSubOrders().get(list.getSubOrders().size() - 1).getTreeRootSize() * (40 / 2);
			}
			SimpleGraphics.drawHorizontalLine(guiGraphics, left - 12, startLeft - 12, yPos + 38, Color.GREEN, zoom.line);
		}
	}

	private void drawPointFor(GuiGraphics guiGraphics, LinkedLogisticsOrderList list, int xPos, int yPos, int i, int startLeft) {
		float totalLine = 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10;
		for (Float point : list.getSubOrders().get(i).getProgresses()) {
			int pos = (int) (totalLine * (1.0F - point));
			if (pos < 13) {
				int newSize = list.getSubOrders().get(i).size();
				int newStartLeft = -(newSize - 1) * (30 / 2) + startLeft - 20;
				for (int j = 0; j < newSize; j++) {
					drawProgressPoint(guiGraphics, newStartLeft + 8, yPos + 48 + 12 - pos, 0xff00ff00);
					newStartLeft += 30;
				}
			} else if (pos < 10 + 1 + 10 + 1) {
				pos -= 10;
				drawProgressPoint(guiGraphics, startLeft - 20 + 8, yPos + 38 + 12 - pos, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1;
				if (startLeft < xPos + 20) {
					pos *= -1;
				}
				drawProgressPoint(guiGraphics, startLeft - 12 - pos, yPos + 38, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1;
				drawProgressPoint(guiGraphics, xPos + 8, yPos + 27 - pos, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1 + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1;
				int newSize = list.size();
				int newStartLeft = -(newSize - 1) * (30 / 2) + xPos;
				for (int j = 0; j < newSize; j++) {
					drawProgressPoint(guiGraphics, newStartLeft + 8, yPos + 16 - pos, 0xff00ff00);
					newStartLeft += 30;
				}
			}
		}
	}

	protected void drawProgressPoint(GuiGraphics guiGraphics, int x, int y, int color) {
		int line = zoom.line + 1;
		guiGraphics.fill(x - line + 1, y - line + 1, x + line, y + line, color);
	}
}
