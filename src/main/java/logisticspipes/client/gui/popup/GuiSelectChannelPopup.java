package logisticspipes.client.gui.popup;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import logisticspipes.Translations;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.SmallGuiButton;

public class GuiSelectChannelPopup extends GuiManageChannelPopup {

    private final Consumer<ChannelInformation> handleResult;

    public GuiSelectChannelPopup(List<ChannelInformation> channelList, BlockPos pos,
        Consumer<ChannelInformation> handleResult) {
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

    protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.centeredText(minecraft.font, Component.translatable(Translations.Screen.CHANNEL_SELECT_TITLE),
            xCenter, guiTop + 6, 0xFFFFFFFF);
    }

}
