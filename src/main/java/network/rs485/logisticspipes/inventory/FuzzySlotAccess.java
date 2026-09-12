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

package network.rs485.logisticspipes.inventory;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.api.property.IBitSet;

/** Moves a slot's fuzzy flags along with its contents. */
public class FuzzySlotAccess implements SlotAccess {

    private final SlotAccess slotAccess;
    private final BitSetProperty fuzzyFlags;

    public FuzzySlotAccess(SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
        this.slotAccess = slotAccess;
        this.fuzzyFlags = fuzzyFlags;
    }

    private IBitSet bitsForSlot(int idx) {
        return fuzzyFlags.get(idx * 4, idx * 4 + 3);
    }

    @Override
    public void mergeSlots(int intoSlot, int fromSlot) {
        slotAccess.mergeSlots(intoSlot, fromSlot);
        bitsForSlot(intoSlot).replaceWith(bitsForSlot(fromSlot));
        bitsForSlot(fromSlot).clear();
    }

    @Override
    public boolean canMerge(int intoSlot, int fromSlot) {
        return slotAccess.canMerge(intoSlot, fromSlot)
            && (isSlotEmpty(intoSlot) || bitsForSlot(intoSlot).equals(bitsForSlot(fromSlot)));
    }

    @Override
    public boolean isSlotEmpty(int idx) {
        return slotAccess.isSlotEmpty(idx);
    }
}
