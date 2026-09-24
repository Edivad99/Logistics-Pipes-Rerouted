package logisticspipes.client.gui.popup;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.Translations;
import logisticspipes.network.to_server.pipe.RequestSatellitePipeListMessage;
import logisticspipes.pipes.SatelliteEntry;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;

public class GuiSelectSatellitePopup extends SubGuiScreen {

    private final Consumer<@Nullable UUID> handleResult;
    private final TextListDisplay textList;
    private List<SatelliteEntry> pipeList = List.of();

    public GuiSelectSatellitePopup(BlockPos pos, boolean fluidSatellites, Consumer<@Nullable UUID> handleResult) {
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
        guiGraphics.centeredText(minecraft.font, Component.translatable(Translations.Screen.SATELLITE_SELECT_TITLE),
            xCenter, guiTop + 6, 0xFFFFFFFF);
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton sel = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10,
            Component.translatable(Translations.Screen.SELECT));
        sel.setPressListener(b -> {
            int selected = textList.getSelected();
            if (selected >= 0) {
                handleResult.accept(pipeList.get(selected).routerId());
                exitGui();
            }
        });
        addRenderableWidget(sel);
        SmallGuiButton ex = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10,
            Component.translatable(Translations.Screen.SATELLITE_SELECT_EXIT));
        ex.setPressListener(b -> exitGui());
        addRenderableWidget(ex);
        SmallGuiButton unset = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10,
            Component.translatable(Translations.Screen.SATELLITE_SELECT_UNSET));
        unset.setPressListener(b -> {
            handleResult.accept(null);
            exitGui();
        });
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
