package logisticspipes.client.gui.popup;

import java.awt.Rectangle;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.pipe.ToggleDisconnectionUpgradeSideMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class DisconnectionConfigurationPopup extends SubGuiScreen {

    private static final String PREFIX = "gui.pipecontroller.popup.";

    private SideConfigDisplay configDisplay;
    private final CoreRoutedPipe pipe;
    private Rectangle bounds;
    private final UpgradeSlot pos;

    public DisconnectionConfigurationPopup(CoreRoutedPipe pipe, UpgradeSlot pos) {
        super(250, 250, 0, 0);
        this.pipe = pipe;
        this.pos = pos;
    }

    @Override
    public void init() {
        super.init();
        configDisplay = new SideConfigDisplay(pipe) {

            @Override
            public void handleSelection(SelectedFace selection) {
                DisconnectionConfigurationPopup.this.handleSelection(selection);
            }
        };
        configDisplay.init();
        configDisplay.renderNeighbours = true;

        SmallGuiButton okBtn = new SmallGuiButton(0, right - 106, bottom - 26, 100, 20, "OK");
        okBtn.setPressListener(b -> exitGui());
        addRenderableWidget(okBtn);

        bounds = new Rectangle(guiLeft + 5, guiTop + 20, this.xSize - 10, this.ySize - 50);
    }

    public void handleSelection(SideConfigDisplay.SelectedFace selection) {
        //ItemStack stack = pipe.getOriginalUpgradeManager().getInv().getItem(pos);
        ClientPacketDistributor.sendToServer(
            new ToggleDisconnectionUpgradeSideMessage(pos.index, Optional.ofNullable(selection.face)));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0xff000000);

        int scale = Minecraft.getInstance().getWindow().getGuiScale();
        int vpx = bounds.x * scale;
        int vpy = (bounds.y + 10) * scale;
        int w = bounds.width * scale;
        int h = (bounds.height - 1) * scale;

        guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "disconnectTitle"), guiLeft + 8, guiTop + 8,
            logisticspipes.utils.Color.getValue(logisticspipes.utils.Color.DARKER_GREY), false);

        configDisplay.drawScreen(mouseX, mouseY, 0.0f, new Rectangle(vpx, vpy, w, h), bounds);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
            int scale = Minecraft.getInstance().getWindow().getGuiScale();
            int vpx = bounds.x * scale;
            int vpy = (bounds.y + 10) * scale;
            int w = bounds.width * scale;
            int h = (bounds.height - 1) * scale;
            configDisplay.onMouseClicked((int) mouseX, (int) mouseY, new Rectangle(vpx, vpy, w, h));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
            configDisplay.onMouseDragged(dx, dy, button);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
            configDisplay.onMouseScrolled(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
    }

}
