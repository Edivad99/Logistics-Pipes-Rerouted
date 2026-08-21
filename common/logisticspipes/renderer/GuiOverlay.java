package logisticspipes.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.SlotFinderNumberPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.SimpleGraphics;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public class GuiOverlay {

	@Getter
	private static final GuiOverlay instance = new GuiOverlay();

	private int oldX;
	private int oldY;
	private boolean hasBeenSaved;
	private boolean clicked;

	@Setter
	private int targetPosX;
	@Setter
	private int targetPosY;
	@Setter
	private int targetPosZ;
	@Setter
	private int pipePosX;
	@Setter
	private int pipePosY;
	@Setter
	private int pipePosZ;
	@Setter
	private ModulePositionType positionType;
	@Setter
	private int positionInt;
	@Setter
	private int slot;
	@Setter
	private boolean isOverlaySlotActive;

	private GuiOverlay() {
		// Mouse class removed in 1.20.1 (LWJGL 3 uses GLFW); fX/fY reflection no longer needed
	}

	public boolean isCompatibleGui() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen;
	}

	public void preRender() {
		if (isOverlaySlotActive) {
			Minecraft mc = Minecraft.getInstance();
			oldX = (int) mc.mouseHandler.xpos();
			oldY = (int) mc.mouseHandler.ypos();
			hasBeenSaved = true;
		}
	}

	/**
	 * Draws the slot highlight over the open container screen. Called from {@code ScreenEvent.Render.Post},
	 * which is the event that hands us the screen's own {@link GuiGraphics} -- the overlay used to run on
	 * {@code RenderFrameEvent.Post} and reach for whichever GuiGraphics the last screen render had left on a
	 * static field, which meant it drew against stale state whenever that ordering did not hold.
	 */
	public void renderOverGui(GuiGraphics guiGraphics) {
		if (hasBeenSaved) {
			hasBeenSaved = false;
			// Mouse restore removed — GLFW mouse position is not directly settable in 1.20.1
		}
		if (isOverlaySlotActive) {
			Minecraft client = Minecraft.getInstance();
			AbstractContainerScreen gui = (AbstractContainerScreen) client.screen;

			int guiTop = gui.getGuiTop();
			int guiLeft = gui.getGuiLeft();

			int x = oldX * gui.width / client.getWindow().getScreenWidth();
			int y = oldY * gui.height / client.getWindow().getScreenHeight();

			for (Slot slot : gui.getMenu().slots) {
				if (isMouseOverSlot(gui, slot, x, y)) {
					int k1 = slot.x + guiLeft;
					int i1 = slot.y + guiTop;
					SimpleGraphics.drawGradientRect(guiGraphics, k1, i1, k1 + 16, i1 + 16, 0xa0ff0000, 0xa0ff0000, 0.0);
					if (clicked) {
						MainProxy.sendPacketToServer(PacketHandler.getPacket(SlotFinderNumberPacket.class)
								.setInventorySlot(slot.index)
								.setSlot(this.slot)
								.setPipePosX(pipePosX)
								.setPipePosY(pipePosY)
								.setPipePosZ(pipePosZ)
								.setType(positionType)
								.setPositionInt(positionInt)
								.setPosX(targetPosX)
								.setPosY(targetPosY)
								.setPosZ(targetPosZ));
						clicked = false;
						client.player.closeContainer();
						isOverlaySlotActive = false;
					}
					break;
				}
			}
			clicked = false;
		}
	}

	private boolean isMouseOverSlot(AbstractContainerScreen gui, Slot slot, int mouseX, int mouseY) {
		return isPointInRegion(gui, slot.x, slot.y, 16, 16, mouseX, mouseY);
	}

	private boolean isPointInRegion(AbstractContainerScreen gui, int x, int y, int width, int height, int pointX, int pointY) {
		int x0 = gui.getGuiLeft();
		int y0 = gui.getGuiTop();
		pointX -= x0;
		pointY -= y0;
		return pointX >= x - 1 && pointX < x + width + 1 && pointY >= y - 1 && pointY < y + height + 1;
	}
}
