/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
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

package network.rs485.logisticspipes.gui.guidebook

import net.minecraft.client.gui.GuiGraphics
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.util.Rectangle

// TODO: Rendering deferred — TabButton migrated to 1.20.1 stub.

interface TabButtonReturn {
    fun onLeftClick(): Boolean
    fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean): Boolean
    fun getColor(): Int
    fun isPageActive(): Boolean
}

private val buttonTextureArea = Rectangle(40, 64, 24, 32)
private val circleAreaTexture = Rectangle(32, 96, 16, 16)

class TabButton(
    internal val tabPage: Page,
    x: Int,
    y: Int,
    private val whisky: TabButtonReturn,
) : LPGuiButton(99, x, y - 24, 24, 32) {

    override val bodyTrigger = Rectangle(1, 1, 22, 22)
    private val circleArea = Rectangle(4, 4, 16, 16)
    val isPageActive: Boolean
        get() = whisky.isPageActive()
    val isInactive: Boolean
        get() = !isPageActive

    fun onLeftClick() = whisky.onLeftClick()

    fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean) = whisky.onRightClick(shiftClick, ctrlClick)

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // TODO: deferred rendering
    }

    override fun getTooltipText(): String {
        return tabPage.title
    }

    override fun setPos(newX: Int, newY: Int) {
        body.setPos(newX, newY - 24)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseXi = mouseX.toInt(); val mouseYi = mouseY.toInt()
        if (!visible || !isPageActive) return false
        return bodyTrigger
            .translated(body)
            .translated(0, if (whisky.isPageActive()) -3 else 0)
            .contains(mouseXi, mouseYi)
    }
}
