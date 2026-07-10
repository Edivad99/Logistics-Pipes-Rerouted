package logisticspipes.gui.popup;

import java.util.List;
import java.util.function.Consumer;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.SmallGuiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSelectChannelPopup extends GuiManageChannelPopup {

	private static final String GUI_LANG_KEY = "gui.popup.selectchannel.";

	private final Consumer<ChannelInformation> handleResult;

	public GuiSelectChannelPopup(List<ChannelInformation> channelList, BlockPos pos, Consumer<ChannelInformation> handleResult) {
		super(channelList, pos);
		this.handleResult = handleResult;
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton selBtn = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10, "Select");
		selBtn.setPressListener(b -> {
			int selected = textList.getSelected();
			if (selected >= 0) {
				ChannelInformation info = channelList.get(selected);
				if (info != null) {
					handleResult.accept(info);
				}
				exitGui();
			}
		});
		addRenderableWidget(selBtn);
	}

	protected void drawTitle(GuiGraphics guiGraphics) {
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), (int) (xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f)), guiTop + 6, 0xFFFFFF, true);
	}

}
