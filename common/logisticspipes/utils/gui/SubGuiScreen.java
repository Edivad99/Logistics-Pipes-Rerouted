package logisticspipes.utils.gui;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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

	/**
	 * How far a popup lifts itself above the screen it covers, in GUI z units.
	 * <p>
	 * The parent screen has already drawn itself by the time a popup renders, and its content writes depth, so
	 * a popup sharing the parent's z loses the LEQUAL test wherever the parent drew something in front. The
	 * value has to clear the highest z the parent can reach, which is further out than it looks because
	 * {@code AbstractContainerScreen.renderSlot} wraps every slot in a {@code translate(0, 0, 100)} baseline:
	 * <ul>
	 * <li>slot item icon: 100 + 150 = 250</li>
	 * <li>slot stack-count label: 100 + 200 = 300</li>
	 * <li>tooltip layer: 400</li>
	 * <li>carried item's count label: {@code renderFloatingItem}'s 232 + 200 = 432</li>
	 * </ul>
	 * Hence 500. At exactly 300 the count labels tied with the panel and won the tie, because a shadowed
	 * string draws its main glyph pass 0.03 in front of the shadow -- so the digits survived while everything
	 * else was covered.
	 * <p>
	 * {@link RenderSystem#disableDepthTest()} is not an alternative here: {@code blit} draws immediately and
	 * does obey it, but {@code fill} and {@code drawString} batch through RenderTypes that re-apply their own
	 * depth state when the batch is drawn, and the popups use a mix of all three.
	 */
	private static final float SUB_GUI_Z = 500.0F;

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
	public boolean charTyped(char par1, int par2) {
		if (subGui != null) {
			return subGui.charTyped(par1, par2);
		}
		// Legacy 1.12 keyTyped port: keyCode 1 was ESC; kept for callers passing it through
		if (par2 == 1) {
			exitGui();
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (subGui != null) {
			return subGui.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == 256) { // GLFW_KEY_ESCAPE: close only this popup, not the whole GUI
			exitGui();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseClicked(mouseX, mouseY, button);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseReleased(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (subGui != null) {
			return subGui.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, SUB_GUI_Z);
		renderGuiBackground(guiGraphics, mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
		poseStack.popPose();
	}

	protected void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, float par3) {}

	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	protected abstract void renderGuiBackground(GuiGraphics guiGraphics, int mouseX, int mouseY);

	@Override
	public void resize(Minecraft mc, int width, int height) {
		super.resize(mc, width, height);
		if (subGui != null) {
			subGui.resize(mc, width, height);
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
			subGui.init(minecraft, width, height);
		}
	}

	@Override
	public LogisticsBaseGuiScreen getBaseScreen() {
		return controller.getBaseScreen();
	}
}
