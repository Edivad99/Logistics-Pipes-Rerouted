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

package network.rs485.logisticspipes.gui

import network.rs485.logisticspipes.gui.font.LPFontRenderer
import network.rs485.logisticspipes.util.FuzzyFlag
import network.rs485.logisticspipes.util.IRectangle
import logisticspipes.utils.Color
import logisticspipes.utils.gui.LPGuiGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.lang.Float.min

// Every drawing method takes the GuiGraphics to draw into, so nothing here depends on ambient state.
// BDF custom-font paths remain deferred — those live on LPFontRenderer.

/**
 * Drawing methods to help with GUIs (rendering implementation deferred for 1.20.1).
 */
object GuiDrawer {

    private const val BORDER: Int = 4
    private const val NORMAL_SLOT_SIZE = 18

    val lpFontRenderer: LPFontRenderer by lazy {
        LPFontRenderer.get("ter-u12n")
    }
    val mcFontRenderer: Font by lazy {
        Minecraft.getInstance().font
    }

    fun getFuzzyColor(fuzzyFlag: FuzzyFlag) = when (fuzzyFlag) {
        FuzzyFlag.IGNORE_DAMAGE -> Color.FUZZY_IGNORE_DAMAGE_COLOR.value
        FuzzyFlag.IGNORE_NBT -> Color.FUZZY_IGNORE_NBT_COLOR.value
        FuzzyFlag.USE_ORE_DICT -> Color.FUZZY_ORE_DICT_COLOR.value
        FuzzyFlag.USE_ORE_CATEGORY -> Color.FUZZY_ORE_CATEGORY_COLOR.value
    }

    fun drawGuiBackground(guiGraphics: GuiGraphics, guiArea: IRectangle) {
        val left = guiArea.roundedLeft
        val top = guiArea.roundedTop
        val right = guiArea.roundedRight
        val bottom = guiArea.roundedBottom
        LPGuiGraphics.drawGuiBackGround(guiGraphics, left, top, right, bottom, 0f, true)
    }

    fun drawGuiTexturedRect(rect: IRectangle, text: IRectangle, blend: Boolean, color: Int) {
        // TODO: texture-atlas sprite blit — no widget GUI calls this yet; port alongside guide book work.
    }

    private val BUTTON = ResourceLocation.withDefaultNamespace("widget/button")
    private val BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled")
    private val BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted")

    /**
     * Draws a vanilla button face. [light] and [thickerBottomBorder] described the hand-drawn borders
     * of the pre-1.21 renderer; the vanilla sprites bake both in, so they no longer select anything.
     */
    fun drawBorderedTile(
        guiGraphics: GuiGraphics,
        rect: IRectangle,
        hovered: Boolean,
        enabled: Boolean,
        light: Boolean,
        thickerBottomBorder: Boolean,
    ) {
        val sprite = when {
            !enabled -> BUTTON_DISABLED
            hovered -> BUTTON_HIGHLIGHTED
            else -> BUTTON
        }
        guiGraphics.blitSprite(RenderType::guiTextured, sprite, rect.roundedLeft, rect.roundedTop, rect.roundedWidth, rect.roundedHeight)
    }

    fun drawGuideBookFrame(rect: IRectangle, slider: IRectangle) {
        // TODO: guide book frame — deferred with guide book rendering port.
    }

    fun drawTextTooltip(
        guiGraphics: GuiGraphics,
        text: List<String>,
        x: Int,
        y: Int,
        z: Float,
        horizontalAlign: HorizontalAlignment,
        verticalAlign: VerticalAlignment,
    ) {
        if (text.isEmpty()) return
        val components = text.map { Component.literal(it) }
        guiGraphics.renderComponentTooltip(mcFontRenderer, components, x, y)
    }

    fun drawGuideBookBackground(rect: IRectangle) {
        // TODO: guide book background — deferred with guide book rendering port.
    }

    fun drawSliderButton(body: IRectangle, texture: IRectangle) {
        // TODO: guide book slider — deferred with guide book rendering port.
    }

    fun drawInteractionIndicator(mouseX: Float, mouseY: Float) {
        // TODO: guide book hover indicator — deferred.
    }

    fun drawLine(guiGraphics: GuiGraphics, start: Pair<Float, Float>, finish: Pair<Float, Float>, color: Int, thickness: Float) {
        val (x1, y1) = start
        val (x2, y2) = finish
        val t = thickness.coerceAtLeast(1f).toInt()
        if (y1 == y2) {
            val xMin = min(x1, x2).toInt()
            val xMax = kotlin.math.max(x1, x2).toInt()
            guiGraphics.fill(xMin, y1.toInt(), xMax, y1.toInt() + t, color)
        } else if (x1 == x2) {
            val yMin = min(y1, y2).toInt()
            val yMax = kotlin.math.max(y1, y2).toInt()
            guiGraphics.fill(x1.toInt(), yMin, x1.toInt() + t, yMax, color)
        } else {
            // diagonal not supported in widget paths — use axis-aligned only.
        }
    }

    fun drawOutlineRect(guiGraphics: GuiGraphics, rect: IRectangle, color: Int) {
        val left = rect.roundedLeft
        val top = rect.roundedTop
        val right = rect.roundedRight
        val bottom = rect.roundedBottom
        guiGraphics.fill(left, top, right, top + 1, color)
        guiGraphics.fill(left, bottom - 1, right, bottom, color)
        guiGraphics.fill(left, top, left + 1, bottom, color)
        guiGraphics.fill(right - 1, top, right, bottom, color)
    }
}

private class Texture(val resource: ResourceLocation, size: Int) {
    val factor: Float = 1.0f / size
}
