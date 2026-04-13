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
import network.rs485.logisticspipes.gui.guidebook.Screen
import network.rs485.logisticspipes.gui.guidebook.x
import network.rs485.logisticspipes.gui.guidebook.y
import network.rs485.logisticspipes.gui.widget.FuzzyItemSlot
import network.rs485.logisticspipes.util.FuzzyFlag
import network.rs485.logisticspipes.util.FuzzyUtil
import network.rs485.logisticspipes.util.IRectangle
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.math.BorderedRectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import network.rs485.markdown.defaultDrawableState
import logisticspipes.LPConstants
import logisticspipes.utils.Color
import logisticspipes.utils.MinecraftColor
import net.minecraft.world.Container
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.ResourceLocation
import java.lang.Float.min

// TODO: Rendering deferred — GL11/GlStateManager/Tessellator/BufferBuilder rendering methods are stubbed.
// All public method signatures are preserved so callers compile.

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

    fun drawGuiContainerBackground(guiArea: IRectangle, topLeft: Pair<Int, Int>, container: AbstractContainerMenu) {
        // TODO: deferred — migrate to GuiGraphics/PoseStack
    }

    fun drawGuiBackground(guiArea: IRectangle) {
        // TODO: deferred
    }

    fun drawGuiTexturedRect(rect: IRectangle, text: IRectangle, blend: Boolean, color: Int) {
        // TODO: deferred
    }

    fun drawBorderedTile(
        rect: IRectangle,
        hovered: Boolean,
        enabled: Boolean,
        light: Boolean,
        thickerBottomBorder: Boolean,
    ) {
        // TODO: deferred
    }

    fun drawGuideBookFrame(rect: IRectangle, slider: IRectangle) {
        // TODO: deferred
    }

    fun drawTextTooltip(
        text: List<String>,
        x: Int,
        y: Int,
        z: Float,
        horizontalAlign: HorizontalAlignment,
        verticalAlign: VerticalAlignment,
    ) {
        // TODO: deferred
    }

    fun drawGuideBookBackground(rect: IRectangle) {
        // TODO: deferred
    }

    fun drawSliderButton(body: IRectangle, texture: IRectangle) {
        // TODO: deferred
    }

    fun drawCenteredString(text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
        // TODO: deferred
    }

    fun drawInteractionIndicator(mouseX: Float, mouseY: Float) {
        // TODO: deferred
    }

    fun drawRect(area: IRectangle, color: Int) {
        // TODO: deferred
    }

    fun drawHorizontalGradientRect(area: IRectangle, colorLeft: Int, colorRight: Int) {
        // TODO: deferred
    }

    fun drawVerticalGradientRect(area: IRectangle, colorTop: Int, colorBottom: Int) {
        // TODO: deferred
    }

    fun drawLine(start: Pair<Float, Float>, finish: Pair<Float, Float>, color: Int, thickness: Float) {
        // TODO: deferred
    }

    fun drawOutlineRect(rect: IRectangle, color: Int) {
        // TODO: deferred
    }
}

private class Texture(val resource: ResourceLocation, size: Int) {
    val factor: Float = 1.0f / size
}
