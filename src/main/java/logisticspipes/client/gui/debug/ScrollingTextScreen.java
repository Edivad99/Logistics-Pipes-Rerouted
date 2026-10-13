package logisticspipes.client.gui.debug;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A scrolling page of text, drawn over the world.
 *
 * <p>Lines are re-read every frame from whatever buffer the subclass points at, so the view keeps
 * up with a running debug session, and closing the screen loses nothing. The screen does not pause
 * a single player game — the log it is showing would stop arriving.
 */
public abstract class ScrollingTextScreen extends Screen {

	private static final int LINE_HEIGHT = 10;
	private static final int MARGIN = 12;
	/** Room above the list for the title. */
	private static final int HEADER = 30;
	/** Room below the list for the buttons. */
	private static final int FOOTER = 32;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;

	private static final int PANEL_COLOR = 0xB0000000;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int DIM_TEXT_COLOR = 0xFF909090;

	private int scroll = 0;
	/** Whether to stick to the bottom as new lines arrive. Any scroll up lets go. */
	private boolean follow = true;

	protected ScrollingTextScreen(Component title) {
		super(title);
	}

	/** The lines to show, oldest first. Called every frame. */
	protected abstract List<Component> lines();

	protected List<Button> footerButtons() {
		return List.of();
	}

	private int listTop() {
		return HEADER;
	}

	private int listBottom() {
		return height - FOOTER;
	}

	private int visibleLines() {
		return Math.max(1, (listBottom() - listTop()) / LINE_HEIGHT);
	}

	private int maxScroll() {
		return Math.max(0, lines().size() - visibleLines());
	}

	@Override
	protected void init() {
		int x = MARGIN;
		for (Button button : footerButtons()) {
			button.setPosition(x, height - FOOTER + BUTTON_GAP);
			button.setHeight(BUTTON_HEIGHT);
			addRenderableWidget(button);
			x += button.getWidth() + BUTTON_GAP;
		}
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		final List<Component> lines = lines();
		final int visible = visibleLines();
		final int maxScroll = Math.max(0, lines.size() - visible);
		scroll = follow ? maxScroll : Mth.clamp(scroll, 0, maxScroll);

		graphics.fill(MARGIN - 4, listTop() - 4, width - MARGIN + 4, listBottom() + 4, PANEL_COLOR);
		graphics.text(font, title, MARGIN, MARGIN, TEXT_COLOR);

		final Component position = Component.literal(lines.isEmpty()
				? "empty"
				: (scroll + 1) + "-" + Math.min(scroll + visible, lines.size()) + " of " + lines.size()
						+ (follow ? " (live)" : ""));
		graphics.text(font, position, width - MARGIN - font.width(position), MARGIN, DIM_TEXT_COLOR);

		for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
			graphics.text(font, lines.get(scroll + i), MARGIN, listTop() + i * LINE_HEIGHT, TEXT_COLOR);
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scrollBy(-(int) Math.signum(scrollY) * 3);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		final int page = visibleLines();
		switch (event.key()) {
			case InputConstants.KEY_UP -> scrollBy(-1);
			case InputConstants.KEY_DOWN -> scrollBy(1);
			case InputConstants.KEY_PAGEUP -> scrollBy(-page);
			case InputConstants.KEY_PAGEDOWN -> scrollBy(page);
			case InputConstants.KEY_HOME -> scrollTo(0);
			case InputConstants.KEY_END -> scrollTo(maxScroll());
			default -> {
				return super.keyPressed(event);
			}
		}
		return true;
	}

	private void scrollBy(int amount) {
		scrollTo(scroll + amount);
	}

	private void scrollTo(int target) {
		final int maxScroll = maxScroll();
		scroll = Mth.clamp(target, 0, maxScroll);
		// Following again once the view is back at the bottom means a glance at the history does
		// not permanently freeze a log that is still being written.
		follow = scroll >= maxScroll;
	}

	/** Runs a command as the player, for buttons that mirror one of the chat prompts. */
	protected void runCommand(String command) {
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.connection.sendCommand(command);
		}
	}
}
