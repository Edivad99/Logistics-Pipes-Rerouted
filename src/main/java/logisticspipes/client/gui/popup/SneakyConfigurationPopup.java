package logisticspipes.client.gui.popup;

import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.pipe.SetSneakyUpgradeSideMessage;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class SneakyConfigurationPopup extends SubGuiScreen {

    private static final String PREFIX = "gui.pipecontroller.popup.";
    private final List<DoubleCoordinates> config;
    private final UpgradeSlot pos;
    private SideConfigDisplay configDisplay;
    private Rectangle bounds;

    public SneakyConfigurationPopup(List<DoubleCoordinates> config, UpgradeSlot pos) {
        super(250, 250, 0, 0);
        this.config = config;
        this.pos = pos;
    }

    @Override
    public void init() {
        super.init();
        configDisplay = new SideConfigDisplay(config) {

            @Override
            public void handleSelection(SelectedFace selection) {
                SneakyConfigurationPopup.this.handleSelection(selection);
            }
        };
        configDisplay.init();
        configDisplay.renderNeighbours = true;

        SmallGuiButton cancel = new SmallGuiButton(0, right - 106, bottom - 26, 100, 20, "Cancel");
        cancel.setPressListener(b -> exitGui());
        addRenderableWidget(cancel);

        bounds = new Rectangle(guiLeft + 5, guiTop + 20, this.xSize - 10, this.ySize - 50);
    }

    public void handleSelection(SideConfigDisplay.SelectedFace selection) {
        ClientPacketDistributor.sendToServer(
            new SetSneakyUpgradeSideMessage(pos.index, Optional.ofNullable(selection.face)));
        this.exitGui();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0xff000000);

        Minecraft mc = Minecraft.getInstance();
        int vpx = bounds.x * Minecraft.getInstance().getWindow().getGuiScale();
        int vpy = (bounds.y + 10) * Minecraft.getInstance().getWindow().getGuiScale();
        int w = bounds.width * Minecraft.getInstance().getWindow().getGuiScale();
        int h = (bounds.height - 1) * Minecraft.getInstance().getWindow().getGuiScale();

        guiGraphics.text(font, TextUtil.translate(PREFIX + "sneakyTitle"), guiLeft + 8, guiTop + 8,
            Color.getValue(Color.DARKER_GREY), false);

        configDisplay.drawScreen(mouseX, mouseY, 0.0f, new Rectangle(vpx, vpy, w, h), bounds);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
            int vpx = bounds.x * Minecraft.getInstance().getWindow().getGuiScale();
            int vpy = (bounds.y + 10) * Minecraft.getInstance().getWindow().getGuiScale();
            int w = bounds.width * Minecraft.getInstance().getWindow().getGuiScale();
            int h = (bounds.height - 1) * Minecraft.getInstance().getWindow().getGuiScale();
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
