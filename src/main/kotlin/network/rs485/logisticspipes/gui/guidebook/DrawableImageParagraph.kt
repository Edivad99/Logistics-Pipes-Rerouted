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

package network.rs485.logisticspipes.gui.guidebook

// TODO: Rendering deferred — DrawableImageParagraph migrated to 1.20.1 stub (Tessellator/GlStateManager removed).

import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.util.IRectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import logisticspipes.LogisticsPipes
import logisticspipes.utils.MinecraftColor
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import java.io.IOException

class DrawableImageParagraph(private val alternativeText: List<DrawableWord>, val image: DrawableImage) : DrawableParagraph() {
    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        relativeBody.setPos(x, y)
        // This has to be done in two steps because setChildrenPos() depends on the width already being set.
        relativeBody.setSize(parent!!.width, 0)
        relativeBody.setSize(relativeBody.roundedWidth, setChildrenPos())
        return relativeBody.roundedWidth to relativeBody.roundedHeight
    }

    override fun setChildrenPos(): Int {
        var currentY = 0
        currentY += if (image.broken) {
            splitAndInitialize(alternativeText, 5, currentY, width - 10, false)
        } else {
            image.setPos(0, currentY).y
        }
        return currentY
    }

    override fun getHovered(mouseX: Float, mouseY: Float): Drawable? = if (image.broken) {
        alternativeText.firstOrNull { it.isMouseHovering(mouseX, mouseY) }
    } else {
        null
    }

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        if (image.broken) {
            super.draw(mouseX, mouseY, delta, visibleArea)
            for (drawableWord in alternativeText.filter { it.visible(visibleArea) }) {
                drawableWord.draw(mouseX, mouseY, delta, visibleArea)
                GuiDrawer.drawOutlineRect(absoluteBody, MinecraftColor.WHITE.colorCode)
            }
        } else {
            image.draw(mouseX, mouseY, delta, visibleArea)
        }
    }
}

class DrawableImage(private var imageResource: ResourceLocation) : Drawable {

    override var relativeBody: MutableRectangle = MutableRectangle()
    override var parent: Drawable? = null

    // In 1.20.1 there is no PngSizeInfo; we detect image availability via resource presence.
    // Width/height are stored separately after a successful read.
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    val broken: Boolean

    init {
        var success = false
        try {
            val resource = Minecraft.getInstance().resourceManager.getResource(imageResource)
            if (resource.isPresent) {
                // TODO: read actual PNG dimensions; for now treat as available with 0x0 placeholder
                success = true
            }
        } catch (e: IOException) {
            LogisticsPipes.log.error("File not found: ${imageResource.path}")
        }
        broken = !success
    }

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        if (!broken) {
            // TODO: deferred rendering — drawImage using GuiGraphics blit
        } else {
            GuiDrawer.drawOutlineRect(absoluteBody, MinecraftColor.WHITE.colorCode)
        }
    }

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        if (!broken) {
            relativeBody.setSize(imageWidth, imageHeight)
            if (imageWidth > (parent?.width ?: 0) && (parent?.width ?: 0) > 0) {
                val downScaleFactor = parent!!.width.toFloat() / width
                relativeBody.scale(downScaleFactor)
            }
        } else {
            relativeBody.setSize(20, 20)
        }
        return super.setPos(x, y)
    }
}
