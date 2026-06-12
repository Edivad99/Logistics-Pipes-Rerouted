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

import logisticspipes.LogisticsPipes
import net.minecraft.world.inventory.Slot
import java.lang.reflect.Field

/**
 * Slot.x and Slot.y are public final in 1.20.1, so widget layout repositions them
 * reflectively. The field NAME differs per environment: Mojmap ("x"/"y") in dev,
 * SRG ("f_40220_"/"f_40221_") in the reobfuscated production jar — try both, like
 * NewGuiHandler does for AbstractContainerMenu.containerId. A silent single-name
 * lookup here was GitHub issue #2: slots stayed at their container-constructor
 * positions in production while the widget grid drew at the layout positions.
 */
private val slotXField: Field? = findSlotField("x", "f_40220_")
private val slotYField: Field? = findSlotField("y", "f_40221_")

private fun findSlotField(mojmapName: String, srgName: String): Field? {
    val cls = Slot::class.java
    val field = try {
        cls.getDeclaredField(mojmapName)
    } catch (e: NoSuchFieldException) {
        try {
            cls.getDeclaredField(srgName)
        } catch (e2: NoSuchFieldException) {
            LogisticsPipes.log.error("Could not resolve Slot.{} / {} — GUI slots will be misaligned", mojmapName, srgName)
            null
        }
    }
    return field?.also { it.isAccessible = true }
}

internal fun Slot.setXY(newX: Int, newY: Int) {
    try {
        slotXField?.setInt(this, newX)
        slotYField?.setInt(this, newY)
    } catch (e: Exception) {
        LogisticsPipes.log.error("Failed to reposition GUI slot", e)
    }
}
