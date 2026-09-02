package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.to_server.config.SetPlayerConfigMessage;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseTabGuiScreen;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiLogisticsSettings extends LogisticsBaseTabGuiScreen {

	private static final String PREFIX = "gui.settings.";

	public GuiLogisticsSettings(final Player player) {
		super(buildDummy(player), 180, 220);
		addTab(new PipeRenderSettings());
	}
	private static DummyContainer buildDummy(final Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addNormalSlotsForPlayerInventory(10, 135);
		return dummy;
	}


	private class PipeRenderSettings extends TabSubGui {

		private InputBar renderDistance;
		private InputBar contentRenderDistance;
		private GuiCheckBox useNewRendererButton;
		private GuiCheckBox useFallbackRendererButton;

		private PipeRenderSettings() {}

		@Override
		public void initTab() {
			

			ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
			if (renderDistance == null) {
				renderDistance = new InputBar(font, getBaseScreen(), getLeftPos() + 15, getTopPos() + 75, 30, 15, false, true, InputBar.Align.RIGHT);
				renderDistance.setInteger(config.getRenderPipeDistance());
			}
			renderDistance.reposition(getLeftPos() + 15, getTopPos() + 80, 30, 15);
			if (contentRenderDistance == null) {
				contentRenderDistance = new InputBar(font, getBaseScreen(), getLeftPos() + 15, getTopPos() + 105, 30, 15, false, true, InputBar.Align.RIGHT);
				contentRenderDistance.setInteger(config.getRenderPipeContentDistance());
			}
			contentRenderDistance.reposition(getLeftPos() + 15, getTopPos() + 110, 30, 15);
            GuiLogisticsSettings.this.addRenderableWidget(renderDistance);
            GuiLogisticsSettings.this.addRenderableWidget(contentRenderDistance);
			//useNewRendererButton = (GuiCheckBox) addRenderableWidget(new GuiCheckBox(0, leftPos + 15, topPos + 30, 16, 16, config.isUseNewRenderer()));
			//useFallbackRendererButton = (GuiCheckBox) addRenderableWidget(new GuiCheckBox(0, leftPos + 15, topPos + 50, 16, 16, config.isUseFallbackRenderer()));
		}

		@Override
		public void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y) {
			// Deferred: tab icon requires an LP item texture selection; left blank for now.
		}

		@Override
		public void renderBackgroundContent(GuiGraphicsExtractor guiGraphics) {}

		@Override
		public void buttonClicked(AbstractButton button) {
			if (button == useNewRendererButton) {
				useNewRendererButton.change();
			}
			if (button == useFallbackRendererButton) {
				useFallbackRendererButton.change();
			}
		}

		@Override
		public void renderForegroundContent(GuiGraphicsExtractor guiGraphics) {
			//guiGraphics.text(font, StringUtil.translate(PREFIX + "pipenewrenderer"), 38, 34, 0xFF404040, false);
			//guiGraphics.text(font, StringUtil.translate(PREFIX + "pipefallbackrenderer"), 38, 54, 0xFF404040, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "piperenderdistance"), 10, 70, 0xFF404040, false);
			guiGraphics.text(font, TextUtil.translate(PREFIX + "pipecontentrenderdistance"), 10, 100, 0xFF404040, false);
		}

		@Override
		public boolean handleClick(int x, int y, int type) {
			boolean val1 = renderDistance.handleClick(x - leftPos, y - topPos, type);
			boolean val2 = contentRenderDistance.handleClick(x - leftPos, y - topPos, type);
			return val1 || val2;
		}

		@Override
		public boolean handleKey(int code, char c) {
			return renderDistance.handleKey(c, code) || contentRenderDistance.handleKey(c, code);
		}

		@Override
		public void guiClose() {
			ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
			try {
				config.setRenderPipeDistance(renderDistance.getInteger());
				config.setRenderPipeContentDistance(contentRenderDistance.getInteger());
			} catch (Exception e) {
				LogisticsPipes.LOG.error("Failed to update render distance config", e);
			}
			//config.setUseNewRenderer(useNewRendererButton.getState());
			//config.setUseFallbackRenderer(useFallbackRendererButton.getState());

			ClientPacketDistributor.sendToServer(SetPlayerConfigMessage.of(config));

		}
	}
}
