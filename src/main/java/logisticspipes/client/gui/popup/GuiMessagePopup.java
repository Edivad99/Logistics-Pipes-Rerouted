package logisticspipes.client.gui.popup;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiMessagePopup extends SubGuiScreen {

    private final String[] text;
    private int mWidth = 0;

    public GuiMessagePopup(Object... message) {
        super(200, (message.length * 10) + 40, 0, 0);
        text = new String[message.length];
        int i = 0;
        for (Object o : message) {
            if (o instanceof Object[]) {
                for (Object oZwei : (Object[]) o) {
                    text[i++] = oZwei.toString();
                }
            } else {
                text[i++] = o.toString();
            }
        }
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton ok = new SmallGuiButton(0, xCenter - 25, bottom - 25, 50, 20, "OK");
        ok.setPressListener(b -> exitGui());
        addRenderableWidget(ok);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (mWidth == 0) {
            int lWidth = 0;
            for (String msg : text) {
                int tWidth = minecraft.font.width(msg);
                if (tWidth > lWidth) {
                    lWidth = tWidth;
                }
            }
            xSize = mWidth = Math.clamp(lWidth + 20, 120, 400);
            super.init();
        }
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        for (int i = 0; i < 9 && i < text.length; i++) {
            if (text[i] == null) {
                continue;
            }
            String msg = TextUtil.getTrimmedString(text[i], mWidth - 10, font, "");
            int stringWidth = minecraft.font.width(msg);
            guiGraphics.text(minecraft.font, msg, xCenter - (stringWidth / 2), guiTop + 10 + (i * 10), 0xFF404040,
                false);
        }
    }

}
