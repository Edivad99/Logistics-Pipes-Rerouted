package logisticspipes.client.gui.debug;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import logisticspipes.client.debug.PipeLogBuffer;

public class PipeLogScreen extends ScrollingTextScreen {

	private enum View {
		CONSOLE,
		STATUS,
	}

	private final PipeLogBuffer buffer;
	private View view = View.CONSOLE;

	private PipeLogScreen(PipeLogBuffer buffer) {
		super(Component.literal("Pipe log — " + buffer.title()));
		this.buffer = buffer;
	}

	/** Shows {@code buffer}, replacing whatever screen is open. */
	public static void open(PipeLogBuffer buffer) {
		Minecraft.getInstance().gui.setScreen(new PipeLogScreen(buffer));
	}

	@Override
	protected List<Component> lines() {
		return view == View.CONSOLE ? buffer.logLines() : buffer.statusLines();
	}

	@Override
	protected List<Button> footerButtons() {
		return List.of(
				tab("Console", View.CONSOLE),
				tab("Status", View.STATUS));
	}

	private Button tab(String label, View target) {
		final Component message = Component.literal(view == target ? "[" + label + "]" : label);
		return Button.builder(message, ignored -> {
			view = target;
			// Rebuilds the footer so the pressed tab is the one drawn as selected.
			rebuildWidgets();
		}).width(70).build();
	}
}
