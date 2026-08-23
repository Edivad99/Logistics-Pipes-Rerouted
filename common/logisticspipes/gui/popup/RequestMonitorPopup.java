package logisticspipes.gui.popup;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import logisticspipes.LPConstants;

import net.minecraft.world.level.block.Block;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import network.rs485.logisticspipes.util.TextUtil;

public class RequestMonitorPopup extends SubGuiScreen {

	private enum ZOOM_LEVEL {
		NORMAL(1, 165, 224, 1, 0, 0, 0),
		LEVEL_1(0.5F, 330, 465, 1, 50, -200, 100),
		LEVEL_2(0.25F, 660, 950, 2, 100, -400, -100);

		ZOOM_LEVEL(float zoom, int bottom, int right, int line, int moveY, int maxX, int maxY) {
			this.zoom = zoom;
			bottomRenderBorder = bottom;
			rightRenderBorder = right;
			this.line = line;
			this.moveY = moveY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		final float zoom;
		final int bottomRenderBorder;
		final int rightRenderBorder;
		final int line;
		final int moveY;
		final int maxX;
		final int maxY;

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

	private static final ResourceLocation achievementTextures = LPConstants.rl("textures/gui/gui_border.png");
	private final PipeBlockRequestTable table;
	private final int orderId;

	private int isMouseButtonDown;
	private int mouseX;
	private int mouseY;
	private double guiMapX;
	private double guiMapY;
	private int minY = -230;
	private int maxY = 0;
	private int minX = -800;
	private int maxX = 800;
	private ZOOM_LEVEL zoom = ZOOM_LEVEL.NORMAL;

	/** The lines to draw for the order under the cursor, and where. */
	private record OrderTooltip(int x, int y, List<String> lines) {}

	private OrderTooltip tooltip = null;

	public RequestMonitorPopup(PipeBlockRequestTable table, int orderId) {
		super(256, 202, 0, 0);
		this.table = table;
		this.orderId = orderId;
		guiMapY = -200;
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
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		int k = (width - xSize) / 2;
		int l = (height - ySize) / 2;
		if (mx >= k + 8 && mx < k + 8 + 224 && my >= l + 17 && my < l + 17 + 155) {
			guiMapX -= dx / zoom.zoom;
			guiMapY -= dy / zoom.zoom;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton closeBtn = new SmallGuiButton(0, width / 2 - 90, height / 2 + 74, 80, 20, "Close");
		closeBtn.setPressListener(b -> exitGui());
		addRenderableWidget(closeBtn);
		SmallGuiButton saveBtn = new SmallGuiButton(1, width / 2 + 10, height / 2 + 74, 80, 20, "Save as Image");
		saveBtn.setPressListener(b -> saveTreeToImage());
		addRenderableWidget(saveBtn);
	}

	@Override
	protected void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, float par3) {
		if (tooltip != null) {
			guiGraphics.setComponentTooltipForNextFrame(minecraft.font,
					tooltip.lines().stream().map(Component::literal).collect(java.util.stream.Collectors.toList()),
					tooltip.x(), tooltip.y());
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {

	}

	@Override
	protected void renderGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (!table.watchedRequests.containsKey(orderId)) {
			exitGui();
			return;
		}
		isMouseButtonDown = 0;

		if (guiMapY < minY) {
			guiMapY = minY;
		}
		if (guiMapY > maxY) {
			guiMapY = maxY;
		}
		if (guiMapX > maxX) {
			guiMapX = maxX;
		}
		if (guiMapX < minX) {
			guiMapX = minX;
		}

		createBoundary();
		drawTransparentBack(guiGraphics);
		drawMap(guiGraphics, mouseX, mouseY);
	}

	private void createBoundary() {
		int size = table.watchedRequests.get(orderId).getValue2().getTreeRootSize();
		minX = -size * (40 / 2) + (int) (75 * (zoom.zoom));
		maxX = -minX + zoom.maxX;
		maxY = -100;
		findLowest(table.watchedRequests.get(orderId).getValue2(), -200);
	}

	private void drawTransparentBack(GuiGraphics guiGraphics) {
		SimpleGraphics.drawGradientRect(guiGraphics, 0, 0, width, height, Color.BLANK, Color.BLANK, 0.0);
	}

	private void findLowest(LinkedLogisticsOrderList list, int lowerLimit) {
		lowerLimit += 48;
		for (LinkedLogisticsOrderList sub : list.getSubOrders()) {
			findLowest(sub, lowerLimit);
		}
		if (maxY < (lowerLimit + 10) * zoom.zoom) {
			maxY = (int) ((lowerLimit + 10) * zoom.zoom) + zoom.maxY;
		}
	}

	private void saveTreeToImage() {
		// NOT PORTED TO 1.21.5. The 1.21.4 implementation is kept verbatim below.
		//
		// It rendered the whole request tree into an offscreen TextureTarget and handed it to
		// Screenshot.takeScreenshot -- itself already a replacement for LP1's glReadPixels
		// tile-stitching. 1.21.5 removed RenderTarget#bindWrite, #unbindWrite and #setClearColor: a
		// draw no longer goes to "whatever target is currently bound", it goes to the target named by
		// its RenderType's OutputStateShard, resolved at draw time. Every vanilla GUI render type
		// names RenderStateShard.MAIN_TARGET, whose supplier is a fixed
		// () -> Minecraft.getInstance().getMainRenderTarget(), and that field is private final --
		// so there is nothing left to redirect.
		//
		// Reviving it needs one of:
		//  - LP render types carrying their own OutputStateShard for every primitive the tree draws.
		//    Blits would work (gg.blit already takes a render type factory), but text goes through
		//    Font's own render types and items through the item renderer's, neither overridable, so
		//    the image would come out half-empty.
		//  - or dropping the offscreen buffer and capturing the main target instead, which caps the
		//    export at the window size and needs the tree scaled to fit.
		//
		// Note that creating and reading a TextureTarget still works -- takeScreenshot now takes a
		// Consumer<NativeImage> and accepts any RenderTarget -- it is only drawing into one that does
		// not. saveImage below is untouched and is the half that gets reused.
		//
		// The TextureTarget, Screenshot, PoseStack, Matrix4f and ProjectionType imports are kept for
		// the block below; they will read as unused until it comes back. Two calls in it no longer
		// compile as written even setting the target aside: TextureTarget now takes a label as its
		// first argument, and Screenshot.takeScreenshot returns nothing and takes a callback.
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(
				Component.literal("Tree view export has not been ported to this Minecraft version"), false);
		}
		LogisticsPipes.LOG.warn("saveTreeToImage is not ported: 1.21.5 removed RenderTarget#bindWrite");

		// // Renders the whole request tree into an offscreen framebuffer and saves it as a PNG,
		// // replacing LP1's glReadPixels tile-stitching which is gone in 1.20.1.
		// if (!table.watchedRequests.containsKey(orderId)) {
		// 	return;
		// }
		// LinkedLogisticsOrderList list = table.watchedRequests.get(orderId).getValue2();
		// int imgWidth = Math.max(256, list.getTreeRootSize() * 40 + 160);
		// int imgHeight = Math.max(256, treeDepth(list) * 48 + 140);
		// int anchorX = imgWidth / 2 - 8;
		// int anchorY = 60;
		//
		// int oldGuiLeft = guiLeft, oldGuiTop = guiTop, oldXSize = xSize, oldYSize = ySize;
		// //GuiGraphics oldStored = getGuiGraphics();
		//         GuiGraphics gg = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
		//         PoseStack modelView = gg.pose();
		// TextureTarget target = new TextureTarget(imgWidth, imgHeight, true);
		// try {
		//
		//             target.setClearColor(0.15F, 0.15F, 0.15F, 1.0F);
		// 	target.clear();
		// 	target.bindWrite(true);
		// 	RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, imgWidth, imgHeight, 0.0F, 1000.0F, 21000.0F),
		// 			ProjectionType.ORTHOGRAPHIC);
		// 	modelView.pushPose();
		// 	modelView.setIdentity();
		// 	modelView.translate(0.0D, 0.0D, -11000.0D);
		//
		// 	// Widen the clip rect so renderItemAt draws the full tree instead of the popup viewport
		// 	guiLeft = -1;
		// 	guiTop = -1;
		// 	xSize = imgWidth + 17;
		// 	ySize = imgHeight + 17;
		//
		// 	RenderSystem.disableBlend();
		// 	if (!list.isEmpty()) {
		// 		SimpleGraphics.drawVerticalLine(gg, anchorX + 8, anchorY - 17, anchorY, Color.GREEN, 1);
		// 	}
		// 	renderLinkedOrderListLines(gg, list, anchorX, anchorY);
		// 	RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
		// 	String s = Integer.toString(orderId);
		// 	int badgeY = list.isEmpty() ? anchorY + 18 : anchorY - 40;
		// 	gg.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, anchorX - 5, badgeY, 0.0f, 202.0f, 26, 26, 256, 256);
		// 	gg.drawString(minecraft.font, s, anchorX + 9 - minecraft.font.width(s) / 2, badgeY + 10, 0xFFFFFFFF, true);
		// 	renderLinkedOrderListItems(gg, list, anchorX, anchorY, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2);
		// 	RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// 	RenderSystem.enableBlend();
		// 	gg.flush();
		//
		// 	NativeImage image = Screenshot.takeScreenshot(target);
		// 	saveImage(image);
		// } catch (Exception e) {
		// 	LogisticsPipes.LOG.error("Failed to render tree view PNG", e);
		// } finally {
		// 	guiLeft = oldGuiLeft;
		// 	guiTop = oldGuiTop;
		// 	xSize = oldXSize;
		// 	ySize = oldYSize;
		// 	//storedGuiGraphics = oldStored;
		// 	modelView.popPose();
		// 	target.destroyBuffers();
		// 	Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		// }
	}

	private int treeDepth(LinkedLogisticsOrderList list) {
		int depth = 1;
		for (LinkedLogisticsOrderList sub : list.getSubOrders()) {
			depth = Math.max(depth, 1 + treeDepth(sub));
		}
		return depth;
	}

	private void saveImage(com.mojang.blaze3d.platform.NativeImage image) {
		File screenShotsFolder = new File(Minecraft.getInstance().gameDirectory, "screenshots");
		screenShotsFolder.mkdirs();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		String s = dateFormat.format(new Date());
		int i = 1;
		try {
			while (true) {
				File candidate = new File(screenShotsFolder, s + "_tree" + (i == 1 ? "" : "_" + i) + ".png");
				if (!candidate.exists()) {
					image.writeToFile(candidate);
					Minecraft.getInstance().player.displayClientMessage(Component.literal("Saved tree view as " + candidate.getName()), false);
					return;
				}
				++i;
			}
		} catch (IOException e) {
			LogisticsPipes.LOG.error("Failed to save tree view PNG", e);
		} finally {
			image.close();
		}
	}

	private void drawMap(GuiGraphics guiGraphics, int par1, int par2) {
		tooltip = null;
		int mapX = (int) Math.floor(guiMapX);
		int mapY = (int) Math.floor(guiMapY - zoom.moveY);
		int leftSide = ((width - xSize) / 2);
		int topSide = ((height - ySize) / 2);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, leftSide, topSide, 0.0f, 0.0f, xSize, ySize, 256, 256);

		guiTop *= 1 / zoom.zoom;
		guiLeft *= 1 / zoom.zoom;
		xSize *= 1 / zoom.zoom;
		ySize *= 1 / zoom.zoom;
		leftSide *= 1 / zoom.zoom;
		topSide *= 1 / zoom.zoom;
		par1 *= 1 / zoom.zoom;
		par2 *= 1 / zoom.zoom;

		int innerLeftSide = leftSide + 16;
		int innerTopSide = topSide + 17;

		LinkedLogisticsOrderList list = table.watchedRequests.get(orderId).getValue2();
		if (!list.isEmpty()) {
			SimpleGraphics.drawVerticalLine(guiGraphics, innerLeftSide - mapX + 110, innerTopSide - mapY - 197, innerTopSide - mapY - 180, Color.GREEN, zoom.line);
		}
		renderLinkedOrderListLines(guiGraphics, list, innerLeftSide - mapX + 102, innerTopSide - mapY - 180);
		for (Float progress : list.getProgresses()) {
			int pos = (int) (29.0F * progress);
			drawProgressPoint(guiGraphics, innerLeftSide - mapX + 110, innerTopSide - mapY - 197 + pos, 0xff00ff00);
		}

		String s = Integer.toString(orderId);
		if (!list.isEmpty()) {
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, innerLeftSide - mapX + 97, innerTopSide - mapY - 220, 0.0f, 202.0f, 26, 26, 256, 256, ARGB.colorFromFloat(1.0F, 0.7F, 0.7F, 0.7F));
			guiGraphics.drawString(minecraft.font, s, innerLeftSide - mapX + 111 - minecraft.font.width(s) / 2, innerTopSide - mapY - 210, 0xFFFFFFFF, true);
		} else {
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, innerLeftSide - mapX + 97, innerTopSide - mapY - 162, 0.0f, 202.0f, 26, 26, 256, 256, ARGB.colorFromFloat(1.0F, 0.7F, 0.7F, 0.7F));
			guiGraphics.drawString(minecraft.font, s, innerLeftSide - mapX + 111 - minecraft.font.width(s) / 2, innerTopSide - mapY - 152, 0xFFFFFFFF, true);
		}
		renderLinkedOrderListItems(guiGraphics, list, innerLeftSide - mapX + 102, innerTopSide - mapY - 180, par1, par2);

		guiTop *= zoom.zoom;
		guiLeft *= zoom.zoom;
		xSize *= zoom.zoom;
		ySize *= zoom.zoom;
		leftSide *= zoom.zoom;
		topSide *= zoom.zoom;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, leftSide, topSide, 0.0f, 0.0f, xSize, ySize, 256, 256);
	}

	private void renderLinkedOrderListItems(GuiGraphics guiGraphics, LinkedLogisticsOrderList list, int xPos, int yPos, int par1, int par2) {
		int size = list.size();
		int startLeft = -(size - 1) * (30 / 2) + xPos;
		yPos += 13;
		for (IOrderInfoProvider aList : list) {
			int badgeTint = aList.isInProgress() ? ARGB.colorFromFloat(1.0F, 0.1F, 0.9F, 0.1F) : ARGB.colorFromFloat(1.0F, 0.7F, 0.7F, 0.7F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RequestMonitorPopup.achievementTextures, startLeft - 5, yPos - 5, 0.0f, 202.0f, 26, 26, 256, 256, badgeTint);
			renderItemAt(guiGraphics, aList.getAsDisplayItem(), startLeft, yPos);
			if (aList.isInProgress() && aList.getMachineProgress() != 0) {
				guiGraphics.fill(startLeft - 4, yPos + 20, startLeft + 20, yPos + 24, 0xff000000);
				guiGraphics.fill(startLeft - 3, yPos + 21, startLeft + 19, yPos + 23, 0xffffffff);
				guiGraphics.fill(startLeft - 3, yPos + 21, startLeft - 3 + (22 * aList.getMachineProgress() / 100), yPos + 23, 0xffff0000);
			}
			if (startLeft - 10 < par1 && par1 < startLeft + 20 && yPos - 6 < par2 && par2 < yPos + 20) {
				if (guiLeft < par1 && par1 < guiLeft + xSize - 16 && guiTop < par2 && par2 < guiTop + ySize - 16) {
					IOrderInfoProvider order = aList;
					List<String> tooltipList = new ArrayList<>();
					tooltipList.add(ChatColor.BLUE + "Request Type: " + ChatColor.YELLOW + order.getType().name());
					tooltipList.add(ChatColor.BLUE + "Send to Router ID: " + ChatColor.YELLOW + order.getRouterId());
					// The display item and the constant `true` that the old Object[] also carried
					// were never read back by the renderer, so they are gone.
					tooltip = new OrderTooltip((int) (par1 * zoom.zoom - 10), (int) (par2 * zoom.zoom), tooltipList);
				}
			}
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

	private void renderItemAt(GuiGraphics guiGraphics, ItemIdentifierStack item, int x, int y) {
		if (guiLeft < x && x < guiLeft + xSize - 16 && guiTop < y && y < guiTop + ySize - 16) {
			ItemStack stack = item.getItem().makeNormalStack(1);
			if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, x, y);
			}
			String s = TextUtil.getThreeDigitFormattedNumber(item.getStackSize(), false);
            guiGraphics.drawString(minecraft.font, s, x + 17 - minecraft.font.width(s), y + 9, 0xFFFFFFFF, true);
		}
	}

	protected void drawProgressPoint(GuiGraphics guiGraphics, int x, int y, int color) {
		int line = zoom.line + 1;
		guiGraphics.fill(x - line + 1, y - line + 1, x + line, y + line, color);
	}

	private TextureAtlasSprite getTexture(Block blockIn) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(blockIn.defaultBlockState());
	}
}
