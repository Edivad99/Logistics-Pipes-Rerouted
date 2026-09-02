package logisticspipes.gui.popup;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.pipe.RequestSatellitePipeListMessage;
import logisticspipes.pipes.SatelliteEntry;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSelectSatellitePopup extends SubGuiScreen {

	String GUI_LANG_KEY = "gui.popup.selectsatellite.";

	private final Consumer<UUID> handleResult;
	private List<SatelliteEntry> pipeList = List.of();
	private final TextListDisplay textList;

	public GuiSelectSatellitePopup(BlockPos pos, boolean fluidSatellites, Consumer<UUID> handleResult) {
		super(150, 170, 0, 0);
		this.handleResult = handleResult;
		this.textList = new TextListDisplay(this, 6, 16, 6, 30, 12, new TextListDisplay.List() {

			@Override
			public int getSize() {
				return pipeList.size();
			}

			@Override
			public String getTextAt(int index) {
				return pipeList.get(index).name();
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		});
		ClientPacketDistributor.sendToServer(new RequestSatellitePipeListMessage(pos, fluidSatellites));
	}

	protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
		guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), (int) (xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f)), guiTop + 6, 0xFFFFFFFF, true);
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton sel = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "select"));
		sel.setPressListener(b -> {
			int selected = textList.getSelected();
			if (selected >= 0) {
				handleResult.accept(pipeList.get(selected).routerId());
				exitGui();
			}
		});
		addRenderableWidget(sel);
		SmallGuiButton ex = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, TextUtil.translate(GUI_LANG_KEY + "exit"));
		ex.setPressListener(b -> exitGui());
		addRenderableWidget(ex);
		SmallGuiButton unset = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "unset"));
		unset.setPressListener(b -> { handleResult.accept(null); exitGui(); });
		addRenderableWidget(unset);
		SmallGuiButton up = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
		up.setPressListener(b -> textList.scrollDown());
		addRenderableWidget(up);
		SmallGuiButton dn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
		dn.setPressListener(b -> textList.scrollUp());
		addRenderableWidget(dn);
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
		drawTitle(guiGraphics);

		textList.extractGuiBackground(guiGraphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double i = event.x();
		double j = event.y();
		int k = event.button();
		textList.mouseClicked(i, j, k);
		return super.mouseClicked(event, doubleClick);
	}

	// Deferred: scroll wheel handling not wired

	public void handleSatelliteList(List<SatelliteEntry> list) {
		pipeList = list;
	}
}
