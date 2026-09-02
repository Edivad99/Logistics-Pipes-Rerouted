package logisticspipes.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.Slot;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.crafting.SlotFinderSlotMessage;
import logisticspipes.utils.gui.SimpleGraphics;

/**
 * Asks the player to point at a slot of an open container screen.
 *
 * <p>The supplier module's pattern pins an entry to one physical slot of the neighbouring
 * inventory. There is no way to name that slot in the pipe's own GUI, so the server opens the
 * neighbour's screen and this highlights its slots until the player clicks one.
 */
public class GuiOverlay {

	@Getter
	private static final GuiOverlay instance = new GuiOverlay();

	/** The module whose pattern is being filled in; null whenever the overlay is off. */
	private @Nullable ModuleTarget target;
	private @Nullable BlockPos inventoryPos;
	private int slot;
	@Setter
	private boolean isOverlaySlotActive;

	private GuiOverlay() {
	}

	/** Starts highlighting the open screen's slots for the given module and pattern entry. */
	public void activate(ModuleTarget target, BlockPos inventoryPos, int slot) {
		this.target = target;
		this.inventoryPos = inventoryPos;
		this.slot = slot;
		isOverlaySlotActive = true;
	}

	public boolean isCompatibleGui() {
		return Minecraft.getInstance().screen instanceof AbstractContainerScreen;
	}

	/**
	 * Draws the highlight over the slot the mouse is on. Called from {@code ScreenEvent.Render.Post},
	 * which is the event that hands us the screen's own {@link GuiGraphicsExtractor} -- the overlay used to run on
	 * {@code RenderFrameEvent.Post} and reach for whichever GuiGraphicsExtractor the last screen render had left on a
	 * static field, which meant it drew against stale state whenever that ordering did not hold.
	 */
	public void renderOverGui(GuiGraphicsExtractor guiGraphics) {
		final Slot hovered = hoveredSlot();
		if (hovered == null) {
			return;
		}
		final AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Minecraft.getInstance().screen;
		final int x = hovered.x + screen.getLeftPos();
		final int y = hovered.y + screen.getTopPos();
		SimpleGraphics.drawGradientRect(guiGraphics, x, y, x + 16, y + 16, 0xa0ff0000, 0xa0ff0000, 0.0);
	}

	/**
	 * Answers the click that picks a slot.
	 *
	 * @return whether the overlay took the click, in which case the screen must not also handle it:
	 *         the player is naming a slot, not moving what is in it
	 */
	public boolean handleClick(int button) {
		final Slot hovered = hoveredSlot();
		final ModuleTarget target = this.target;
		final BlockPos inventoryPos = this.inventoryPos;
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || hovered == null || target == null || inventoryPos == null) {
			return false;
		}
		final AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Minecraft.getInstance().screen;
		ClientPacketDistributor.sendToServer(
				new SlotFinderSlotMessage(target, inventoryPos, screen.getMenu().slots.indexOf(hovered), slot));
		isOverlaySlotActive = false;
		this.target = null;
		this.inventoryPos = null;
		Minecraft.getInstance().player.closeContainer();
		return true;
	}

	/**
	 * The slot the player is pointing at, or null when there is nothing to point at.
	 *
	 * <p>Vanilla's own hover is what the highlight draws on and what the click reads, so the two
	 * cannot disagree about which slot the player meant. The overlay used to hit-test the slots
	 * itself against raw GLFW pixel coordinates it had scaled by hand.
	 *
	 * <p>The player's own inventory is skipped. Its slots are on the same screen but belong to a
	 * different container, and the pattern can only name a slot of the inventory next to the pipe --
	 * offering to pick one would be offering something the server can only answer with "slot not
	 * found".
	 */
	private @Nullable Slot hoveredSlot() {
		if (!isOverlaySlotActive || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
			return null;
		}
		final Slot hovered = screen.hoveredSlot;
		if (hovered == null || hovered.container == Minecraft.getInstance().player.getInventory()) {
			return null;
		}
		return hovered;
	}
}
