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

import network.rs485.logisticspipes.gui.guidebook.Drawable
import network.rs485.logisticspipes.gui.guidebook.MouseInteractable
import network.rs485.logisticspipes.gui.widget.FuzzySelectionWidget
import network.rs485.logisticspipes.inventory.container.LPBaseContainer
import network.rs485.logisticspipes.util.IRectangle
import logisticspipes.modules.LogisticsModule
import logisticspipes.utils.gui.LPGuiGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent

// TODO: Rendering deferred — full 1.20.1 rendering migration (PoseStack, AbstractContainerScreen API) pending.

abstract class BaseGuiContainer(
    private val baseContainer: LPBaseContainer<LogisticsModule>,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    private val widgetScreen: WidgetScreen,
) : AbstractContainerScreen<LPBaseContainer<LogisticsModule>>(
    baseContainer,
    Minecraft.getInstance().player!!.inventory,
    Component.empty(),
), Drawable by widgetScreen {

    open val fuzzySelector: FuzzySelectionWidget? = null

    /** Exposes the protected hoveredSlot field from AbstractContainerScreen. */
    val currentHoveredSlot: Slot? get() = hoveredSlot

    override fun init() {
        super.init()
        // WidgetScreen resets its origin to (0, 0) before placeChildren, so SlotGroup.setPos
        // bakes LOCAL panel-relative coords into Slot.x/y — exactly what AbstractContainerScreen
        // expects at render time (it translates the pose by leftPos/topPos before renderSlot).
        // We just copy the centered rect into leftPos/topPos/imageWidth/imageHeight.
        widgetScreen.initGuiWidget(this@BaseGuiContainer, width, height)
        val rect = widgetScreen.relativeBody
        // Round exactly the way the widgets do. Centering puts the panel on a half pixel as often as
        // not, and truncating here while the widgets draw at roundToInt() shifts every slot's
        // contents -- and its hit test -- one pixel off the slot background painted around it.
        imageWidth = rect.roundedWidth
        imageHeight = rect.roundedHeight
        leftPos = rect.roundedLeft
        topPos = rect.roundedTop
    }

    open fun drawBackgroundLayer(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    open fun drawFocalgroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    open fun drawForegroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    private var lastPartialTick: Float = 0f

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        lastPartialTick = partialTick
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        drawHoveredSlotHighlight(guiGraphics)
    }

    /**
     * Re-draws the hover highlight vanilla already drew during [render].
     *
     * The widget layer paints its slot backgrounds from [renderLabels], which
     * `AbstractContainerScreen#render` calls *after* the highlight, so the highlight ends up buried
     * under those backgrounds -- items survive only because they are drawn with depth. Drawing it
     * once more here, on top, gives these slots the same feedback as every vanilla slot.
     *
     * 1.21.3 split the highlight into a back sprite (drawn under the item) and a front sprite drawn
     * over it with [RenderType.guiTexturedOverlay]. Only the front one may be repeated on top --
     * re-blitting the opaque back sprite here would cover the item stack.
     */
    private fun drawHoveredSlotHighlight(guiGraphics: GuiGraphics) {
        val slot = hoveredSlot?.takeIf { it.isActive && it.isHighlightable } ?: return
        val pose = guiGraphics.pose()
        pose.pushMatrix()
        // Slot.x/y are panel-local, the same coords vanilla renders them at.
        pose.translate(leftPos.toFloat(), topPos.toFloat())
        // Same 24x24 quad, offset by 4, that AbstractContainerScreen#renderSlotHighlightFront uses.
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, slot.x - 4, slot.y - 4, 24, 24)
        pose.popMatrix()
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // super.renderLabels draws the menu title + inventory label at local (0,0); we skip those
        // for widget GUIs — widget layout handles its own labels.
        // Pose is translated by (leftPos, topPos); counter-translate so widget absolute coords work.
        val pose = guiGraphics.pose()
        pose.pushMatrix()
        pose.translate(-leftPos.toFloat(), -topPos.toFloat())
        val rect = widgetScreen.relativeBody
        drawBackgroundLayer(mouseX, mouseY, lastPartialTick)
        widgetScreen.widgetContainer.draw(guiGraphics, mouseX.toFloat(), mouseY.toFloat(), lastPartialTick, rect)
        drawForegroundLayer(mouseX.toFloat(), mouseY.toFloat(), lastPartialTick)
        pose.popMatrix()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val hovered = widgetScreen.widgetContainer.getHovered(event.x.toFloat(), event.y.toFloat())
        if (hovered is MouseInteractable) {
            if (hovered.mouseClicked(event.x.toFloat(), event.y.toFloat(), event.button())) return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        val rect = widgetScreen.relativeBody
        val left = rect.x0.toInt()
        val top = rect.y0.toInt()
        val right = left + rect.width.toInt()
        val bottom = top + rect.height.toInt()
        LPGuiGraphics.drawGuiBackGround(guiGraphics, left, top, right, bottom, 0f, true)
    }

    fun List<Drawable>.draw(guiGraphics: GuiGraphics, mouseX: Float, mouseY: Float, partialTicks: Float, visibleArea: IRectangle) =
        forEach {
            it.draw(guiGraphics, mouseX, mouseY, partialTicks, visibleArea)
        }

    /**
     * Returns a list of rectangles that overflow from the main gui area, so that JEI can avoid it.
     */
    abstract fun getExtraGuiAreas(): List<IRectangle>

    companion object {
        // Private in AbstractContainerScreen, re-declared here for [drawHoveredSlotHighlight].
        private val SLOT_HIGHLIGHT_FRONT_SPRITE: ResourceLocation =
            ResourceLocation.withDefaultNamespace("container/slot_highlight_front")
    }
}
