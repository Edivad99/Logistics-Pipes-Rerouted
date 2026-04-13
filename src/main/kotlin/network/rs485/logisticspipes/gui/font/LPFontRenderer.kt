/*
 * Copyright (c) 2022  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2022  RS485
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

package network.rs485.logisticspipes.gui.font

import network.rs485.grow.Coroutines
import network.rs485.markdown.*
import logisticspipes.LPConstants
import logisticspipes.LogisticsPipes
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.tan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// TODO: Rendering deferred — GL11/GlStateManager/Tessellator drawing methods stubbed for 1.20.1 migration.

class LPFontRenderer(private val fontName: String) {
    companion object Factory {
        private val fontRenderersOfThisWorld = ConcurrentHashMap<String, LPFontRenderer>()
        private val preloadFonts = listOf("ter-u12n")
        fun get(fontName: String): LPFontRenderer = fontRenderersOfThisWorld.getOrPut(fontName) { LPFontRenderer(fontName) }

        @ExperimentalCoroutinesApi
        fun asyncPreload() {
            Coroutines.ioScope.launch {
                preloadFonts.map {
                    async {
                        get(it).apply { ::fontPlain.get() }
                    }
                }.forEach { deferred ->
                    deferred.invokeOnCompletion {
                        if (it != null) {
                            LogisticsPipes.log.error("Error while preloading fonts:\n${it.stackTraceToString()}")
                        } else {
                            val fontRenderer = deferred.getCompleted()
                            LogisticsPipes.log.info("Preloaded font files: ${fontRenderer.fontName}")
                            Minecraft.getInstance().execute {
                                fontRenderer::wrapperPlain.get()
                                LogisticsPipes.log.info("Created font textures for: ${fontRenderer.fontName}")
                            }
                        }
                    }
                }
            }
        }
    }

    private val fontPlain: IFont by lazy {
        val initialTime = System.currentTimeMillis()
        val fontResourcePlain = ResourceLocation(LPConstants.LP_MOD_ID, "fonts/$fontName.bdf")
        FontParser.read(fontResourcePlain).also { LogisticsPipes.log.info("Elapsed time parsing font: ${System.currentTimeMillis() - initialTime}ms") }
            ?: throw IOException("Failed to load ${fontResourcePlain.path}, this is not tolerated.")
    }

    private val wrapperPlain: FontWrapper by lazy {
        val initialTime = System.currentTimeMillis()
        FontWrapper(fontPlain).also { LogisticsPipes.log.info("Elapsed time wrapping font: ${System.currentTimeMillis() - initialTime}ms") }
    }

    var zLevel: Float = 5f

    fun width(text: String): Int = getStringWidth(text)

    /**
     * Draws the given string — stubbed until rendering is migrated to 1.20.1 GuiGraphics/PoseStack.
     */
    fun drawString(string: String, x: Float, y: Float, color: Int, format: Set<TextFormat>, scale: Float): Int {
        // TODO: deferred — migrate to GuiGraphics drawString
        return 0
    }

    fun drawSpace(x: Float, y: Float, width: Int, color: Int, italic: Boolean, underline: Boolean, strikethrough: Boolean, shadow: Boolean, scale: Float): Int {
        // TODO: deferred
        return width
    }

    fun drawCenteredString(string: String, x: Float, y: Float, color: Int, tags: Set<TextFormat>, scale: Float): Int {
        // TODO: deferred
        return 0
    }

    fun getFontHeight(scale: Float = 1f): Int = (wrapperPlain.fontHeight * scale).toInt()

    fun getStringWidth(string: String, italics: Boolean, bold: Boolean, scale: Float): Int {
        val italicsOffset = if (italics) scale else 0f
        return (string.fold(0.0) { currentX, char ->
            val glyph = wrapperPlain.getGlyph(char)
            currentX + ((glyph?.dWidthX ?: 0) * scale)
        } + italicsOffset).toInt()
    }

    fun getStringWidth(string: String, tags: Set<TextFormat>, scale: Float): Int =
        getStringWidth(string, tags.italic(), tags.bold(), scale)

    fun getStringWidth(string: String): Int = getStringWidth(string = string, italics = false, bold = false, scale = 1f)
}
