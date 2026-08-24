package logisticspipes.utils.gui;

import java.util.Collections;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.utils.Color;

public class TextListDisplay {

	public interface List {

		int getSize();

		String getTextAt(int index);

		int getTextColor(int index);
	}

	private final List list;
	private final IGuiAccess gui;

	private final int borderTop;
	private final int borderRight;
	private final int borderBottom;
	private final int borderLeft;
	private final int elementPerPage;

	private int mouseClickX = 0;
	private int mouseClickY = 0;
	private int mousePosX = 0;
	private int mousePosY = 0;
	private int scroll = 0;
	@Getter
	@Setter
	private int selected = -1;
	private int hover = -1;

	public TextListDisplay(IGuiAccess gui, int borderLeft, int borderTop, int borderRight, int borderBottom, int elementPerPage, List list) {
		this.list = list;
		this.gui = gui;
		this.borderTop = borderTop;
		this.borderRight = borderRight;
		this.borderBottom = borderBottom;
		this.borderLeft = borderLeft;
		this.elementPerPage = elementPerPage;
	}

	public boolean mouseClicked(double i, double j, int k) {
		mouseClickX = (int)i;
		mouseClickY = (int)j;
		return false;
	}

	public void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		mousePosX = mouseX;
		mousePosY = mouseY;

        guiGraphics.fill(gui.getLeftPos() + borderLeft, gui.getTopPos() + borderTop, gui.getRight() - borderRight, gui.getBottom() - borderBottom, Color.getValue(Color.GREY));

		if (scroll + elementPerPage > list.getSize()) {
			scroll = list.getSize() - elementPerPage;
		}
		if (scroll < 0) {
			scroll = 0;
		}

		boolean flag = false;

		hover = -1;
		if (gui.getLeftPos() + borderLeft + 2 < this.mousePosX
				&& this.mousePosX < gui.getRight() - borderRight - 2 && gui.getTopPos() + borderTop + 2 < this.mousePosY
				&& this.mousePosY < gui.getTopPos() + borderTop + 3 + (elementPerPage * 10)) {
			hover = scroll + (this.mousePosY - gui.getTopPos() - borderTop - 3) / 10;
		}
		if (list.getSize() == 0 || hover >= list.getSize()) {
			hover = -1;
		}

		if (gui.getLeftPos() + borderLeft + 2 < this.mouseClickX
				&& this.mouseClickX < gui.getRight() - borderRight - 2 && gui.getTopPos() + borderTop + 2 < this.mouseClickY
				&& this.mouseClickY < gui.getTopPos() + borderTop + 3 + (elementPerPage * 10)) {
			selected = scroll + (this.mouseClickY - gui.getTopPos() - borderTop - 3) / 10;
			mouseClickX = -1;
			mouseClickY = -1;
		}

		for (int i = scroll; i < list.getSize() && (i - scroll) < elementPerPage; i++) {
			if (i == selected) {
                guiGraphics.fill(gui.getLeftPos() + borderLeft + 2, gui.getTopPos() + borderTop + 2 + ((i - scroll) * 10), gui.getRight() - borderRight - 2, gui.getTopPos() + borderTop + 13 + ((i - scroll) * 10), Color.getValue(Color.DARKER_GREY));
				flag = true;
			}
            String name = list.getTextAt(i);

            int minX = gui.getLeftPos() + borderLeft + 4;
            int maxX = gui.getLeftPos() + gui.getImageWidth() - borderRight - 2;

            // The collector takes a vertical band rather than a baseline -- it centres the line at
            // (minY + maxY - 9) / 2 + 1 -- so the band is built around the old baseline to land on
            // the same pixel. The colour rides on the component's style; there is no colour
            // argument any more.
            int lineY = gui.getTopPos() + borderTop + 4 + ((i - scroll) * 10);
            int colour = list.getTextColor(i);
            guiGraphics.textRenderer().acceptScrollingWithDefaultCenter(
                Component.literal(name).withStyle(style -> style.withColor(colour)),
                minX,
                maxX,
                lineY - 1,
                lineY + 8
            );
		}

		if (!flag) {
			selected = -1;
		}
	}

	public void renderGuiForeground(GuiGraphicsExtractor guiGraphics) {
		if (hover != -1) {
			LPGuiGraphics.drawToolTip(guiGraphics, mousePosX - gui.getLeftPos(), mousePosY - gui.getTopPos(), Collections.singletonList(list.getTextAt(hover)), ChatFormatting.WHITE);
		}
	}

	public void scrollUp() {
		scroll++;
	}

	public void scrollDown() {
		if (scroll > 0) {
			scroll--;
		}
	}

	public void mouseScrollUp() {
		if (gui.getLeftPos() + borderLeft < mousePosX
				&& mousePosX < gui.getRight() - borderRight && gui.getTopPos() + borderTop < mousePosY && mousePosY < gui.getBottom() + borderBottom) {
			scrollUp();
		}
	}

	public void mouseScrollDown() {
		if (gui.getLeftPos() + borderLeft < mousePosX
				&& mousePosX < gui.getRight() - borderRight && gui.getTopPos() + borderTop < mousePosY && mousePosY < gui.getBottom() + borderBottom) {
			scrollDown();
		}
	}
}
