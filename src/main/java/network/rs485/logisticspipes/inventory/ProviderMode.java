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

/** The constants are saved/restored by their ordinal, so the order is part of the save format. */
public enum ProviderMode {
    DEFAULT("Normal", false, false, 0, 0),
    LEAVE_FIRST("LeaveFirst", false, false, 1, 0),
    LEAVE_LAST("LeaveLast", false, false, 0, 1),
    LEAVE_FIRST_AND_LAST("LeaveFirstAndLast", false, false, 1, 1),
    LEAVE_ONE_PER_STACK("Leave1PerStack", true, false, 0, 0),
    LEAVE_ONE_PER_TYPE("Leave1PerType", false, true, 0, 0);

    private final String modeTranslationKey;
    private final boolean hideOnePerStack;
    private final boolean hideOnePerType;
    private final int cropStart;
    private final int cropEnd;

    ProviderMode(String translationName, boolean hideOnePerStack, boolean hideOnePerType, int cropStart, int cropEnd) {
        this.modeTranslationKey = "misc.extractionmode." + translationName;
        this.hideOnePerStack = hideOnePerStack;
        this.hideOnePerType = hideOnePerType;
        this.cropStart = cropStart;
        this.cropEnd = cropEnd;
    }

    public static ProviderMode modeFromIntSafe(int id) {
        ProviderMode[] modes = values();
        return id >= 0 && id < modes.length ? modes[id] : DEFAULT;
    }

    public String getModeTranslationKey() {
        return modeTranslationKey;
    }

    public boolean getHideOnePerStack() {
        return hideOnePerStack;
    }

    public boolean getHideOnePerType() {
        return hideOnePerType;
    }

    public int getCropStart() {
        return cropStart;
    }

    public int getCropEnd() {
        return cropEnd;
    }
}
