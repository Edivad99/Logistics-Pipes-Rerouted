package logisticspipes.client.gui.popup;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.IGUIChannelInformationReceiver;
import logisticspipes.network.to_server.channel.DeleteChannelMessage;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiManageChannelPopup extends SubGuiScreen implements IGUIChannelInformationReceiver {

    private static final String GUI_LANG_KEY = "gui.popup.managechannel.";

    protected final List<ChannelInformation> channelList;
    protected final TextListDisplay textList;
    private final BlockPos position;

    public GuiManageChannelPopup(List<ChannelInformation> channelList, BlockPos pos) {
        super(150, 170, 0, 0);
        this.channelList = channelList;
        this.position = pos;
        this.textList = new TextListDisplay(this, 6, 16, 6, 30, 12, new TextListDisplay.List() {

            @Override
            public int getSize() {
                return channelList.size();
            }

            @Override
            public String getTextAt(int index) {
                return channelList.get(index).getName();
            }

            @Override
            public int getTextColor(int index) {
                return 0xFFFFFF;
            }
        });
    }

    /**
     * The station these channels answer to, or null when it has none yet.
     *
     * <p>Read from the client's own copy of the station: both popups used to be opened by asking
     * the server for this id and for a channel the client was already holding.
     */
    private @Nullable UUID securityStationId() {
        final Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return level.getBlockEntity(position) instanceof LogisticsSecurityTileEntity station
            ? station.getSecId()
            : null;
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton delBtn = new SmallGuiButton(10, xCenter + 16, bottom - 27, 50, 10, "Delete");
        delBtn.setPressListener(b -> {
            int selected = textList.getSelected();
            if (selected >= 0) {
                this.setSubGui(new ActionChoicePopup(TextUtil.translate(GUI_LANG_KEY + "deletedialog.title"),
                    TextUtil.translate(GUI_LANG_KEY + "deletedialog.yes"), () ->
                    ClientPacketDistributor.sendToServer(
                        new DeleteChannelMessage(channelList.get(selected).getChannelIdentifier())),
                    TextUtil.translate(GUI_LANG_KEY + "deletedialog.no"), () -> {}));
            }
        });
        addRenderableWidget(delBtn);
        SmallGuiButton exitBtn = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, "Exit");
        exitBtn.setPressListener(b -> exitGui());
        addRenderableWidget(exitBtn);
        SmallGuiButton addBtn = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, "Add");
        addBtn.setPressListener(b -> setSubGui(new GuiAddChannelPopup(securityStationId())));
        addRenderableWidget(addBtn);
        SmallGuiButton editBtn = new SmallGuiButton(3, xCenter - 66, bottom - 15, 50, 10, "Edit");
        editBtn.setPressListener(b -> {
            int selected = textList.getSelected();
            if (selected >= 0) {
                setSubGui(new GuiEditChannelPopup(securityStationId(), channelList.get(selected)));
            }
        });
        addRenderableWidget(editBtn);
        SmallGuiButton upBtn = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
        upBtn.setPressListener(b -> textList.scrollDown());
        addRenderableWidget(upBtn);
        SmallGuiButton dnBtn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
        dnBtn.setPressListener(b -> textList.scrollUp());
        addRenderableWidget(dnBtn);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        drawTitle(guiGraphics);

        textList.extractGuiBackground(guiGraphics, mouseX, mouseY);
    }

    protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.text(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"),
            (int) (xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f)), guiTop + 6,
            0xFFFFFFFF, true);
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

    @Override
    public void handleChannelInformation(ChannelInformation channel, boolean flag) {
        if (!flag) {
            if (channel.getName() == null) {
                channelList.removeIf(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier()));
            } else {
                if (channelList.stream()
                    .anyMatch(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier()))) {
                    channelList.stream()
                        .filter(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier()))
                        .forEach(chan -> {
                            chan.setName(channel.getName());
                            chan.setRights(channel.getRights());
                            chan.setResponsibleSecurityID(channel.getResponsibleSecurityID());
                        });
                } else {
                    channelList.add(channel);
                }
            }
        }
    }
}
