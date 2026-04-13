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
import network.rs485.logisticspipes.gui.guidebook.Screen
import network.rs485.logisticspipes.gui.widget.FuzzyItemSlot
import network.rs485.logisticspipes.gui.widget.FuzzySelectionWidget
import network.rs485.logisticspipes.gui.widget.GhostSlot
import network.rs485.logisticspipes.gui.widget.Tooltipped
import network.rs485.logisticspipes.inventory.container.LPBaseContainer
import network.rs485.logisticspipes.util.IRectangle
import logisticspipes.modules.LogisticsModule
import logisticspipes.utils.gui.DummySlot
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.math.roundToInt

// TODO: Rendering deferred — full 1.20.1 rendering migration (PoseStack, AbstractContainerScreen API) pending.

abstract class BaseGuiContainer(
    private val baseContainer: LPBaseContainer<LogisticsModule>,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    private val widgetScreen: WidgetScreen,
) : AbstractContainerScreen<LPBaseContainer<LogisticsModule>>(
    baseContainer,
    Inventory(Minecraft.getInstance().player!!),
    Component.empty(),
), Drawable by widgetScreen {

    open val fuzzySelector: FuzzySelectionWidget? = null

    /** Exposes the protected hoveredSlot field from AbstractContainerScreen. */
    val currentHoveredSlot: Slot? get() = hoveredSlot

    override fun init() {
        super.init()
        widgetScreen.initGuiWidget(this@BaseGuiContainer, width, height)
    }

    open fun drawBackgroundLayer(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // TODO: deferred rendering — migrate to PoseStack-based rendering
    }

    open fun drawFocalgroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    open fun drawForegroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {
        // TODO: deferred rendering
    }

    override fun renderBg(
        guiGraphics: net.minecraft.client.gui.GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        // TODO: deferred rendering
    }

    fun List<Drawable>.draw(mouseX: Float, mouseY: Float, partialTicks: Float, visibleArea: IRectangle) =
        forEach {
            it.draw(mouseX, mouseY, partialTicks, visibleArea)
        }

    /**
     * Returns a list of rectangles that overflow from the main gui area, so that JEI can avoid it.
     */
    abstract fun getExtraGuiAreas(): List<IRectangle>
}
