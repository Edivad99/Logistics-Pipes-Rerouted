/*
 * Copyright (c) Krapht, 2012
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.module.SetSneakyDirectionMessage;
import logisticspipes.world.inventory.SneakyDirectionMenu;
import network.rs485.logisticspipes.module.SneakyDirection;

public class GuiSneakyConfigurator extends ModuleBaseGui {

	private final SneakyDirection directionReceiver;
	private final logisticspipes.utils.gui.SmallGuiButton[] dirButtons = new logisticspipes.utils.gui.SmallGuiButton[7];

	public GuiSneakyConfigurator(SneakyDirectionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, menu.getModule(), 160, 200);
		directionReceiver = (SneakyDirection) menu.getModule();
	}

	@Override
	public void init() {
		super.init();

		int left = width / 2 - panelWidth / 2;
		int top = height / 2 - panelHeight / 2;

		dirButtons[0] = wire(new logisticspipes.utils.gui.SmallGuiButton(0, left + 110, top + 103, 40, 20, "")); //DOWN
		dirButtons[1] = wire(new logisticspipes.utils.gui.SmallGuiButton(1, left + 110, top + 43, 40, 20, "")); //UP
		dirButtons[2] = wire(new logisticspipes.utils.gui.SmallGuiButton(2, left + 50, top + 53, 50, 20, "")); //NORTH
		dirButtons[3] = wire(new logisticspipes.utils.gui.SmallGuiButton(3, left + 50, top + 93, 50, 20, "")); //SOUTH
		dirButtons[4] = wire(new logisticspipes.utils.gui.SmallGuiButton(4, left + 10, top + 73, 40, 20, "")); //WEST
		dirButtons[5] = wire(new logisticspipes.utils.gui.SmallGuiButton(5, left + 100, top + 73, 40, 20, "")); //EAST
		dirButtons[6] = wire(new logisticspipes.utils.gui.SmallGuiButton(6, left + 10, top + 23, 60, 20, "")); //DEFAULT
		for (logisticspipes.utils.gui.SmallGuiButton b : dirButtons) addRenderableWidget(b);

		refreshButtons();
	}

	private logisticspipes.utils.gui.SmallGuiButton wire(logisticspipes.utils.gui.SmallGuiButton btn) {
		btn.setPressListener(b -> {
			directionReceiver.setSneakyDirection(b.id == 6 ? null : Direction.from3DDataValue(b.id));
			ClientPacketDistributor.sendToServer(new SetSneakyDirectionMessage(
					ModuleTarget.of(module), Optional.ofNullable(directionReceiver.getSneakyDirection())));
			refreshButtons();
		});
		return btn;
	}

	private void refreshButtons() {
		for (logisticspipes.utils.gui.SmallGuiButton button : dirButtons) {
			if (button == null) continue;
			button.setMessage(Component.literal(getButtonOrientationString(button.id == 6 ? null : Direction.from3DDataValue(button.id))));
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {

		refreshButtons();

		super.extractLabels(guiGraphics, mouseX, mouseY);

		guiGraphics.text(minecraft.font, "Sneaky orientation", panelWidth / 2 - minecraft.font.width("Sneaky orientation") / 2, 10, 0xFF404040, false);
	}

	private static final Identifier TEXTURE = LPConstants.rl("textures/gui/sneaky.png");

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
		// texture: GuiSneakyConfigurator.TEXTURE
		int j = leftPos;
		int k = topPos;
		//guiGraphics.fill(width/2 - panelWidth / 2, height / 2 - panelHeight /2, width/2 + panelWidth / 2, height / 2 + panelHeight /2, 0xFF404040);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiSneakyConfigurator.TEXTURE, j, k, 0.0f, 0.0f, panelWidth, panelHeight, 256, 256);
	}

	private String getButtonOrientationString(Direction orientation) {
		String s = (orientation == null ? "DEFAULT" : orientation.name());
		if (orientation == directionReceiver.getSneakyDirection()) {
			return "\u00a7a>" + s + "<";
		}
		return s.toLowerCase(Locale.US);
	}

}
