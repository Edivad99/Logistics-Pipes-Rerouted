/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.gui.HorizontalAlignment;
import logisticspipes.api.gui.VerticalAlignment;
import logisticspipes.api.util.IRectangle;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import network.rs485.logisticspipes.util.FuzzyFlag;

// Every drawing method takes the GuiGraphicsExtractor to draw into, so nothing here depends on ambient state.

/**
 * Drawing methods to help with GUIs (rendering implementation deferred for 1.20.1).
 */
public final class GuiDrawer {

    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");

    // Resolved on first use, not at class load: Minecraft.getInstance() is not ready that early.
    private static @Nullable Font mcFontRenderer = null;

    private GuiDrawer() {
    }

    public static Font getMcFontRenderer() {
        Font font = mcFontRenderer;
        if (font == null) {
            font = Minecraft.getInstance().font;
            mcFontRenderer = font;
        }
        return font;
    }

    public static int getFuzzyColor(FuzzyFlag fuzzyFlag) {
        return switch (fuzzyFlag) {
            case IGNORE_DAMAGE -> Color.FUZZY_IGNORE_DAMAGE_COLOR.getValue();
            case IGNORE_NBT -> Color.FUZZY_IGNORE_NBT_COLOR.getValue();
            case USE_ORE_DICT -> Color.FUZZY_ORE_DICT_COLOR.getValue();
            case USE_ORE_CATEGORY -> Color.FUZZY_ORE_CATEGORY_COLOR.getValue();
        };
    }

    public static void drawGuiBackground(GuiGraphicsExtractor guiGraphics, IRectangle guiArea) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiArea.getRoundedLeft(), guiArea.getRoundedTop(),
            guiArea.getRoundedRight(), guiArea.getRoundedBottom(), 0f, true);
    }

    /**
     * Draws a vanilla button face. {@code light} and {@code thickerBottomBorder} described the
     * hand-drawn borders of the pre-1.21 renderer; the vanilla sprites bake both in, so they no
     * longer select anything.
     */
    public static void drawBorderedTile(GuiGraphicsExtractor guiGraphics, IRectangle rect, boolean hovered,
        boolean enabled, boolean light, boolean thickerBottomBorder) {
        Identifier sprite = !enabled ? BUTTON_DISABLED : hovered ? BUTTON_HIGHLIGHTED : BUTTON;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, rect.getRoundedLeft(), rect.getRoundedTop(),
            rect.getRoundedWidth(), rect.getRoundedHeight());
    }

    public static void drawTextTooltip(GuiGraphicsExtractor guiGraphics, List<String> text, int x, int y, float z,
        HorizontalAlignment horizontalAlign, VerticalAlignment verticalAlign) {
        if (text.isEmpty()) {
            return;
        }
        guiGraphics.setComponentTooltipForNextFrame(getMcFontRenderer(),
            text.stream().<Component>map(Component::literal).toList(), x, y);
    }

    public static void drawLine(GuiGraphicsExtractor guiGraphics, float x1, float y1, float x2, float y2, int color,
        float thickness) {
        int t = (int) Math.max(thickness, 1f);
        if (y1 == y2) {
            guiGraphics.fill((int) Math.min(x1, x2), (int) y1, (int) Math.max(x1, x2), (int) y1 + t, color);
        } else if (x1 == x2) {
            guiGraphics.fill((int) x1, (int) Math.min(y1, y2), (int) x1 + t, (int) Math.max(y1, y2), color);
        }
        // diagonal not supported in widget paths — use axis-aligned only.
    }

    public static void drawOutlineRect(GuiGraphicsExtractor guiGraphics, IRectangle rect, int color) {
        int left = rect.getRoundedLeft();
        int top = rect.getRoundedTop();
        int right = rect.getRoundedRight();
        int bottom = rect.getRoundedBottom();
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }
}
