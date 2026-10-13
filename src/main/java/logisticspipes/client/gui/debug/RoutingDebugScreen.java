package logisticspipes.client.gui.debug;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;

/**
 * The candidate routes of a routing table update being stepped through.
 *
 * <p>The buttons run the same commands as the prompt in chat, so the update can be driven from
 * here without closing the list. The screen is not opened on its own: the point of this tool is the
 * in-world HUD on the pipes, which a full screen would cover.
 */
public class RoutingDebugScreen extends ScrollingTextScreen {

	private RoutingDebugScreen() {
		super(Component.literal("Routing table debug"));
	}

	public static void open() {
		Minecraft.getInstance().gui.setScreen(new RoutingDebugScreen());
	}

	@Override
	protected List<Component> lines() {
		return ClientViewController.instance().candidateLines();
	}

	@Override
	protected List<Button> footerButtons() {
		return List.of(
				step("Next", "one", 50),
				step("Rest", "all", 50),
				step("Stop", "stop", 50));
	}

	private Button step(String label, String argument, int width) {
		return Button.builder(Component.literal(label),
						ignored -> runCommand(LPConstants.ID + " debug step " + argument))
				.width(width).build();
	}
}
