/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.gui;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.mojang.math.Divisor;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.renderer.HUDDrawContext;

/**
 * Utils class for GUI-related drawing methods.
 */
public final class LPGuiGraphics {

    public static final Identifier WIDGETS_TEXTURE = Identifier.withDefaultNamespace(
        "textures/gui/widgets.png");
    public static final Identifier SLOT_TEXTURE = LPConstants.rl("textures/gui/slot.png");
    public static final Identifier BIG_SLOT_TEXTURE = LPConstants.rl("textures/gui/slot-big.png");
    public static final Identifier SMALL_SLOT_TEXTURE = LPConstants.rl("textures/gui/slot-small.png");
    public static final Identifier BACKGROUND_TEXTURE = LPConstants.rl("textures/gui/guibackground.png");
    public static final Identifier LOCK_ICON = LPConstants.rl("textures/gui/lock.png");
    public static final Identifier LINES_ICON = LPConstants.rl("textures/gui/lines.png");
    public static final Identifier STATS_ICON = LPConstants.rl("textures/gui/stats.png");
    public static final Identifier SLOT_DISK_TEXTURE = LPConstants.rl("textures/gui/slot_disk.png");
    public static final Identifier SLOT_PROGRAMMER_TEXTURE = LPConstants.rl("textures/gui/slot_programmer.png");
    public static float zLevel = 0.0F;

    private LPGuiGraphics() {
    }

    public static void drawToolTip(GuiGraphicsExtractor guiGraphics, int posX, int posY, List<String> msg,
        ChatFormatting rarityColor) {
        if (msg.isEmpty()) {
            return;
        }
        int y = posY;
        for (int i = 0; i < msg.size(); ++i) {
            String line = msg.get(i);

            if (i == 0) {
                line = rarityColor + line;
            } else {
                line = "\u00a77" + line;
            }

            guiGraphics.setComponentTooltipForNextFrame(
                Minecraft.getInstance().font,
                List.of(Component.literal(line)),
                posX,
                y
            );
            y += (i == 0) ? 2 : 10;
        }
    }

    public static void drawPlayerInventoryBackground(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        //Player "backpack"
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                LPGuiGraphics.drawSlotBackground(guiGraphics, xOffset + column * 18, yOffset + row * 18);
            }
        }
        //Player "hotbar"
        for (int i1 = 0; i1 < 9; i1++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, xOffset + i1 * 18, yOffset + 58);
        }
    }

    public static void drawPlayerHotbarBackground(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        //Player "hotbar"
        for (int i1 = 0; i1 < 9; i1++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, xOffset + i1 * 18, yOffset);
        }
    }

    public static void drawPlayerArmorBackground(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        //Player "armor"
        for (int i1 = 0; i1 < 4; i1++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, xOffset, yOffset - i1 * 18);
        }
    }

    private static void doDrawSlotBackground(GuiGraphicsExtractor guiGraphics, int x, int y, Identifier slotDiskTexture) {
        LPGuiGraphics.zLevel = 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, slotDiskTexture, x, y, 0.0f, 0.0f, 18, 18, 18, 18);
        // 1-pixel darker inset border so the slot visually separates from the panel on light backgrounds.
        final int borderColor = 0x80373737;
        guiGraphics.fill(x, y, x + 18, y + 1, borderColor);
        guiGraphics.fill(x, y + 17, x + 18, y + 18, borderColor);
        guiGraphics.fill(x, y, x + 1, y + 18, borderColor);
        guiGraphics.fill(x + 17, y, x + 18, y + 18, borderColor);
    }

    public static void drawSlotDiskBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        doDrawSlotBackground(guiGraphics, x, y, LPGuiGraphics.SLOT_DISK_TEXTURE);
    }

    public static void drawSlotProgrammerBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        doDrawSlotBackground(guiGraphics, x, y, LPGuiGraphics.SLOT_PROGRAMMER_TEXTURE);
    }

    public static void drawSlotBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        doDrawSlotBackground(guiGraphics, x, y, LPGuiGraphics.SLOT_TEXTURE);
    }

    public static void drawSlotBackground(GuiGraphicsExtractor guiGraphics, int x, int y, int color) {
        doDrawSlotBackground(guiGraphics, x, y, LPGuiGraphics.SLOT_TEXTURE);
    }

    public static void drawBigSlotBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        LPGuiGraphics.zLevel = 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.BIG_SLOT_TEXTURE, x, y, 0.0f, 0.0f, 26, 26, 26, 26);
    }

    public static void drawSmallSlotBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        LPGuiGraphics.zLevel = 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.SMALL_SLOT_TEXTURE, x, y, 0.0f, 0.0f, 8, 8, 8, 8);
    }

    public static void renderIconAt(GuiGraphicsExtractor guiGraphics, int x, int y, float zLevel, TextureAtlasSprite icon) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
    }

    public static void drawLockBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        LPGuiGraphics.zLevel = 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.LOCK_ICON, x, y, 0.0f, 0.0f, 14, 15, 14, 15);
    }

    private static void drawTexture16by16(GuiGraphicsExtractor guiGraphics, int x, int y, Identifier tex) {
        LPGuiGraphics.zLevel = 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, 16, 16, 16, 16);
    }

    public static void drawLinesBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        drawTexture16by16(guiGraphics, x, y, LPGuiGraphics.LINES_ICON);
    }

    public static void drawStatsBackground(GuiGraphicsExtractor guiGraphics, int x, int y) {
        drawTexture16by16(guiGraphics, x, y, LPGuiGraphics.STATS_ICON);
    }

    public static void drawGuiBackGround(@Nullable GuiGraphicsExtractor guiGraphics, int guiLeft, int guiTop, int right, int bottom,
        float zLevel, boolean resetColor) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, zLevel, resetColor, true, true,
            true, true);
    }

    public static void drawGuiBackGround(@Nullable GuiGraphicsExtractor guiGraphics, int guiLeft, int guiTop, int right,
        int bottom, float zLevel, boolean resetColor, boolean displayTop, boolean displayLeft, boolean displayBottom,
        boolean displayRight) {
        if (resetColor) {
        }
        if (guiGraphics == null) {
            return;
        }
        final int panelW = right - guiLeft;
        final int panelH = bottom - guiTop;
        if (panelW <= 0 || panelH <= 0) {
            return;
        }

        // GuiGraphicsExtractor.blit() draws immediately -- innerBlit() goes straight to BufferUploader.drawWithShader()
        // instead of a RenderType -- so it obeys the live RenderSystem state and writes depth. That is fine in
        // a 2D GUI, but the world-space HUD stacks every panel layer within a fraction of a block of the next,
        // so those depth writes make the following coplanar content (inner panels, slots, text, fills) fail the
        // LEQUAL test and come out stippled. A background never has to occlude what is drawn after it, so run
        // it with depth writes off and let draw order decide the layering, exactly as a GUI does.
        try {
            drawGuiBackGroundLayers(
                (texture, x, y, u, v, w, h, texW, texH) ->
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH),
                guiLeft, guiTop, right, bottom, displayTop, displayLeft, displayBottom, displayRight);
        } finally {
        }
    }

    /**
     * The one drawing operation the nine-slice needs. The GUI and the world-space HUD reach it
     * through different objects -- {@link GuiGraphicsExtractor} and {@link HUDDrawContext} -- so the routine
     * is written against this instead of against either of them.
     */
    @FunctionalInterface
    public interface Blitter {
        void blit(Identifier texture, int x, int y, float u, float v, int width, int height, int texWidth,
            int texHeight);
    }

    /** Nine-slices the panel background onto the world-space HUD. */
    public static void drawGuiBackGround(HUDDrawContext context, int guiLeft, int guiTop, int right, int bottom,
        float zLevel, boolean resetColor) {
        drawGuiBackGround(context, guiLeft, guiTop, right, bottom, zLevel, resetColor, true, true, true, true);
    }

    public static void drawGuiBackGround(HUDDrawContext context, int guiLeft, int guiTop, int right, int bottom,
        float zLevel, boolean resetColor, boolean displayTop, boolean displayLeft, boolean displayBottom,
        boolean displayRight) {
        if (right - guiLeft <= 0 || bottom - guiTop <= 0) {
            return;
        }
        drawGuiBackGroundLayers(context::blit, guiLeft, guiTop, right, bottom, displayTop, displayLeft, displayBottom,
            displayRight);
    }

    private static void drawGuiBackGroundLayers(Blitter blitter, int guiLeft, int guiTop, int right,
        int bottom, boolean displayTop, boolean displayLeft, boolean displayBottom, boolean displayRight) {

        // 9-slice the 45×45 background texture (15px borders).
        // blit(rl, x, y, u, v, w, h, texW, texH) — no stretching, src == dst size
        // blitRepeating(rl, dstX, dstY, dstW, dstH, srcX, srcY, srcW, srcH, texW, texH) — tiles
        final int BORDER = 15;
        final int TEX = 45;
        final int innerX = guiLeft + BORDER;
        final int innerY = guiTop + BORDER;
        final int innerW = right - BORDER - innerX;  // right-15 - (guiLeft+15)
        final int innerH = bottom - BORDER - innerY;  // bottom-15 - (guiTop+15)

        // Corners
        if (displayTop && displayLeft) {
            blitter.blit(BACKGROUND_TEXTURE, guiLeft, guiTop, 0.0f, 0.0f, BORDER, BORDER, TEX, TEX);
        }
        if (displayTop && displayRight) {
            blitter.blit(BACKGROUND_TEXTURE, right - BORDER, guiTop, 30.0f, 0.0f, BORDER, BORDER, TEX, TEX);
        }
        if (displayBottom && displayLeft) {
            blitter.blit(BACKGROUND_TEXTURE, guiLeft, bottom - BORDER, 0.0f, 30.0f, BORDER, BORDER, TEX, TEX);
        }
        if (displayBottom && displayRight) {
            blitter.blit(BACKGROUND_TEXTURE, right - BORDER, bottom - BORDER, 30.0f, 30.0f, BORDER, BORDER, TEX,
                TEX);
        }

        // Edges (tiled)
        if (innerW > 0) {
            if (displayTop) {
                blitRepeating(blitter, BACKGROUND_TEXTURE, innerX, guiTop, innerW, BORDER, BORDER, 0, BORDER,
                    BORDER, TEX, TEX);
            }
            if (displayBottom) {
                blitRepeating(blitter, BACKGROUND_TEXTURE, innerX, bottom - BORDER, innerW, BORDER, BORDER, 30,
                    BORDER, BORDER, TEX, TEX);
            }
        }
        if (innerH > 0) {
            if (displayLeft) {
                blitRepeating(blitter, BACKGROUND_TEXTURE, guiLeft, innerY, BORDER, innerH, 0, BORDER, BORDER,
                    BORDER, TEX, TEX);
            }
            if (displayRight) {
                blitRepeating(blitter, BACKGROUND_TEXTURE, right - BORDER, innerY, BORDER, innerH, 30, BORDER,
                    BORDER, BORDER, TEX, TEX);
            }
        }

        // Center (always drawn)
        if (innerW > 0 && innerH > 0) {
            blitRepeating(blitter, BACKGROUND_TEXTURE, innerX, innerY, innerW, innerH, BORDER, BORDER, BORDER,
                BORDER, TEX, TEX);
        }
    }

    private static void blitRepeating(Blitter blitter, Identifier p_283059_, int p_283575_, int p_283192_,
        int p_281790_, int p_283642_, int p_282691_, int p_281912_, int p_281728_, int p_282324_, int textureWidth,
        int textureHeight) {
        int i = p_283575_;

        int j;
        for (IntIterator intiterator = slices(p_281790_, p_281728_); intiterator.hasNext(); i += j) {
            j = intiterator.nextInt();
            int k = (p_281728_ - j) / 2;
            int l = p_283192_;

            int i1;
            for (IntIterator intiterator1 = slices(p_283642_, p_282324_); intiterator1.hasNext(); l += i1) {
                i1 = intiterator1.nextInt();
                int j1 = (p_282324_ - i1) / 2;
                blitter.blit(p_283059_, i, l, p_282691_ + k, p_281912_ + j1, j, i1, textureWidth, textureHeight);
            }
        }
    }

    private static IntIterator slices(int p_282197_, int p_282161_) {
        int i = Mth.positiveCeilDiv(p_282197_, p_282161_);
        return new Divisor(p_282197_, i);
    }
}
