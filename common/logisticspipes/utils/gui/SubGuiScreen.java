package logisticspipes.utils.gui;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class SubGuiScreen extends Screen implements ISubGuiController, IGuiAccess {

	@Getter
	protected int guiLeft;
	@Getter
	protected int guiTop;
	protected int xCenter;
	protected int yCenter;
	@Getter
	protected int right;
	@Getter
	protected int bottom;
	@Getter
	protected int xSize;
	@Getter
	protected int ySize;
	protected int xCenterOffset;
	protected int yCenterOffset;
	protected ISubGuiController controller;
    @Nullable
	private SubGuiScreen subGui;

    public SubGuiScreen(int xSize, int ySize, int xOffset, int yOffset) {
		super(Component.empty());
		this.xSize = xSize;
		this.ySize = ySize;
		xCenterOffset = xOffset;
		yCenterOffset = yOffset;
	}

	@Override
	public void init() {
		super.init();
		guiLeft = width / 2 - xSize / 2 + xCenterOffset;
		guiTop = height / 2 - ySize / 2 + yCenterOffset;

		right = width / 2 + xSize / 2 + xCenterOffset;
		bottom = height / 2 + ySize / 2 + yCenterOffset;

		xCenter = (right + guiLeft) / 2;
		yCenter = (bottom + guiTop) / 2;
	}


	public void register(ISubGuiController gui) {
		controller = gui;
	}

	public void exitGui() {
		controller.resetSubGui();
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (subGui != null) {
			return subGui.charTyped(event);
		}
		// Legacy 1.12 keyTyped port: keyCode 1 was ESC; kept for callers passing it through
		if (event.modifiers() == 1) {
			exitGui();
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (subGui != null) {
			return subGui.keyPressed(event);
		}
		if (event.isEscape()) { // close only this popup, not the whole GUI
			exitGui();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (subGui != null) {
			return subGui.mouseClicked(event, doubleClick);
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (subGui != null) {
			return subGui.mouseReleased(event);
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (subGui != null) {
			return subGui.mouseDragged(event, dragX, dragY);
		}
		return super.mouseDragged(event, dragX, dragY);
	}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (subGui != null) {
            return subGui.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Background is drawn by renderGuiBackground() — suppress Screen's renderMenuBackground overlay
	}

	@Override
	public final void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // The popup used to lift itself above the screen underneath by translating z by SUB_GUI_Z.
        // 1.21.6 made the GUI pose 2D, and depth is now a property of the render state: a stratum
        // renders entirely after every stratum before it, which is exactly what this needs -- and
        // it nests correctly for popups on top of popups, since each one opens its own.
        guiGraphics.nextStratum();
		renderGuiBackground(guiGraphics, mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderLabels(guiGraphics, mouseX, mouseY);
		if (subGui != null) {
			if (!subGui.hasSubGui()) {
				// Same intent as in LogisticsBaseGuiScreen: dim what this popup covers, nothing else.
				renderTransparentBackground(guiGraphics);
			}
			// Nested popups stack: each one lifts itself another step above the one it covers.
			subGui.render(guiGraphics, mouseX, mouseY, partialTicks);
		}
		renderToolTips(guiGraphics, mouseX, mouseY, partialTicks);
	}

	protected void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, float par3) {}

	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	protected abstract void renderGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY);

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (subGui != null) {
			subGui.resize(width, height);
		}
	}

	@Override
	public void resetSubGui() {
		subGui = null;
	}

	@Override
	public boolean hasSubGui() {
		return subGui != null;
	}

	@Override
	public @Nullable SubGuiScreen getSubGui() {
		return subGui;
	}

	@Override
	public void setSubGui(SubGuiScreen gui) {
		if (subGui == null) {
			subGui = gui;
			subGui.register(this);
			subGui.init(width, height);
		}
	}

	@Override
	public LogisticsBaseGuiScreen getBaseScreen() {
		return controller.getBaseScreen();
	}
}
