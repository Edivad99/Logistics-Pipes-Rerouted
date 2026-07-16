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

// TODO: Rendering deferred — GL11/GlStateManager/TextureUtil usage stubbed for 1.20.1 migration.

private val buffer = java.nio.ByteBuffer.allocateDirect(10000000).asIntBuffer()

class FontWrapper(private val font: IFont) {
    val textures: List<Int> get() = emptyList()

    private var glyphPosX: Map<Char, Int> = emptyMap()
    private var glyphPosY: Map<Char, Int> = emptyMap()
    private var textureIndex: Map<Int, CharRange> = emptyMap()

    val fontWidth: Int get() = font.width
    val fontHeight: Int get() = font.height
    val fontXOffset: Int get() = font.offsetX
    val fontYOffset: Int get() = font.offsetY
    val fontLineOffset: Int get() = fontHeight + fontYOffset

    val defaultChar = font.defaultChar

    private val maxTexSize = 512

    init {
        // TODO: deferred — texture allocation requires GL context; skipped during migration
    }

    // Getter for the texture indexes, returns -1 if null.
    fun getTextureIndex(c: Char): Int = -1

    // Getter for width, return -1 if char not found.
    fun getFontTextureSize(): Int = maxTexSize

    // Getter for the glyph's X coordinate
    fun getGlyphX(c: Char): Int = glyphPosX[c] ?: -1

    // Getter for the glyph's Y coordinate
    fun getGlyphY(c: Char): Int = glyphPosY[c] ?: -1

    // Getter for the Glyph object
    fun getGlyph(c: Char): IGlyph? = font.glyphs[c] ?: font.glyphs[font.defaultChar]

    private fun Int.powerOf2(): Int {
        var n = this - 1
        n = n or n.ushr(1)
        n = n or n.ushr(2)
        n = n or n.ushr(4)
        n = n or n.ushr(8)
        n = n or n.ushr(16)
        return if (n < 0) 1 else if (n >= Integer.MAX_VALUE) Integer.MAX_VALUE else n + 1
    }
}
