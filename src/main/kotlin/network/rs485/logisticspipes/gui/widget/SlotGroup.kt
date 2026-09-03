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

package network.rs485.logisticspipes.gui.widget

import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.Margin
import network.rs485.logisticspipes.gui.Size
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.gui.Drawable
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.util.FuzzyFlag
import network.rs485.logisticspipes.util.FuzzyUtil
import network.rs485.logisticspipes.util.IRectangle
import logisticspipes.utils.gui.LPGuiGraphics
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.Slot

class SlotGroup(
    parent: Drawable,
    xPosition: HorizontalAlignment,
    yPosition: VerticalAlignment,
    margin: Margin,
    val slots: List<Slot>,
    val columns: Int,
    val rows: Int
) : LPGuiWidget(
    parent = parent,
    xPosition = xPosition,
    yPosition = yPosition,
    xSize = Size.FIXED,
    ySize = Size.FIXED,
    margin = margin,
) {
    override val minWidth: Int = columns * 18
    override val minHeight: Int = rows * 18
    override val maxWidth: Int = minWidth
    override val maxHeight: Int = minHeight

    override fun initWidget() {
        assert(slots.size == columns * rows)
        setSize(minWidth, minHeight)
    }

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        super.setPos(x, y)
        val startX = absoluteBody.roundedX + 1
        val startY = absoluteBody.roundedY + 1
        val slotSize = 18
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                slots[column + row * columns].setXY(startX + column * slotSize, startY + row * slotSize)
            }
        }
        return width to height
    }

    override fun draw(guiGraphics: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        super.draw(guiGraphics, mouseX, mouseY, delta, visibleArea)
        val startX = absoluteBody.roundedX
        val startY = absoluteBody.roundedY
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val x = startX + column * 18
                val y = startY + row * 18
                LPGuiGraphics.drawSlotBackground(guiGraphics, x, y)
                (slots[column + row * columns] as? FuzzyItemSlot)?.also { slot ->
                    drawFuzzyCorners(guiGraphics, slot, x + 1, y + 1)
                }
            }
        }
    }

    /**
     * The corner marks telling which fuzzy flags a slot has set, one corner per flag, in the same
     * places and colours the older screens draw them.
     *
     * Only the flags the slot actually offers get a corner: a slot that cannot use the ore
     * dictionary should not claim a corner for it.
     */
    private fun drawFuzzyCorners(guiGraphics: GuiGraphicsExtractor, slot: FuzzyItemSlot, x: Int, y: Int) {
        val flags = slot.flagGetter.invoke()
        slot.usedFlags.filter { FuzzyUtil.get(flags, it) }.forEach { flag ->
            val color = GuiDrawer.getFuzzyColor(flag)
            when (flag) {
                FuzzyFlag.USE_ORE_DICT -> {
                    guiGraphics.fill(x + 8, y - 1, x + 17, y, color)
                    guiGraphics.fill(x + 16, y, x + 17, y + 8, color)
                }
                FuzzyFlag.IGNORE_DAMAGE -> {
                    guiGraphics.fill(x - 1, y - 1, x + 8, y, color)
                    guiGraphics.fill(x - 1, y, x, y + 8, color)
                }
                FuzzyFlag.IGNORE_NBT -> {
                    guiGraphics.fill(x - 1, y + 16, x + 8, y + 17, color)
                    guiGraphics.fill(x - 1, y + 8, x, y + 17, color)
                }
                FuzzyFlag.USE_ORE_CATEGORY -> {
                    guiGraphics.fill(x + 8, y + 16, x + 17, y + 17, color)
                    guiGraphics.fill(x + 16, y + 8, x + 17, y + 17, color)
                }
            }
        }
    }
}
