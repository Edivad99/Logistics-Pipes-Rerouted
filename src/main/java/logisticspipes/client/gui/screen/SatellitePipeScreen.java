/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.network.to_server.pipe.SetSatelliteNameMessage;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.world.inventory.SatelliteMenu;
import network.rs485.logisticspipes.util.TextUtil;

public class SatellitePipeScreen extends LogisticsBaseGuiScreen<SatelliteMenu> {

	private final SatellitePipe satellitePipe;

	private String response = "";

	private InputBar input;

	public SatellitePipeScreen(SatelliteMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 116, 77, 0, 0);
		this.satellitePipe = menu.getPipe();
	}

	@Override
	public void init() {
		

		super.init();
		SmallGuiButton saveBtn = new SmallGuiButton(0, (width / 2) - (30 / 2) + 35, (height / 2) + 20, 30, 10, "Save");
		saveBtn.setPressListener(b -> ClientPacketDistributor.sendToServer(new SetSatelliteNameMessage(
				Objects.requireNonNull(satellitePipe.getContainer()).getBlockPos(), input.getValue())));
		addRenderableWidget(saveBtn);
		input = new InputBar(font, this, leftPos + 8, topPos + 40, 100, 16);
        addRenderableWidget(input);
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		drawCenteredString(guiGraphics, TextUtil.translate("gui.satellite.SatelliteName"), 59, 7, 0xFF404040);
		String name = TextUtil.getTrimmedString(satellitePipe.getSatellitePipeName(), 100, minecraft.font, "...");
		int yOffset = 0;
		if (!response.isEmpty()) {
			drawCenteredString(guiGraphics, TextUtil.translate("gui.satellite.naming_result." + response), panelWidth / 2, 30, response.equals("success") ? 0xFF404040 : 0xFF5c1111);
			yOffset = 4;
		}
		drawCenteredString(guiGraphics, name, panelWidth / 2, 24 - yOffset, 0xFF404040);
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
		super.extractGuiBackground(guiGraphics, x, y, f);
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x();
		double y = event.y();
		int k = event.button();
		if (!input.handleClick(x, y, k)) {
			return super.mouseClicked(event, doubleClick);
		}
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char c = (char) event.codepoint();
		int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (!input.handleKey(c, i)) {
			return super.charTyped(event);
		}
		return true;
	}

	public void handleResponse(SatelliteNamingResult result, String newName) {
		response = result.toString();
		if (result == SatelliteNamingResult.SUCCESS) {
			satellitePipe.setSatellitePipeName(newName);
		}
	}
}
