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

package network.rs485.logisticspipes.gui;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

public interface MouseInteractable extends MouseHoverable {

    /**
     * A mouse click event should run this.
     *
     * @param mouseX      X position of the mouse (absolute, screen)
     * @param mouseY      Y position of the mouse (absolute, screen)
     * @param mouseButton button of the mouse that was pressed.
     * @return true, if click was handled
     */
    default boolean mouseClicked(float mouseX, float mouseY, int mouseButton) {
        return false;
    }

    /**
     * Mouse scroll event, run this.
     *
     * @param mouseX       X position of the mouse (absolute, screen)
     * @param mouseY       Y position of the mouse (absolute, screen)
     * @param scrollAmount how much the scroll wheel has turned since the last event.
     */
    default boolean mouseScrolled(float mouseX, float mouseY, float scrollAmount) {
        return false;
    }

    /**
     * A mouse release event should run this.
     *
     * @param mouseX      X position of the mouse (absolute, screen)
     * @param mouseY      Y position of the mouse (absolute, screen)
     * @param mouseButton button of the mouse that was pressed.
     */
    default boolean mouseReleased(float mouseX, float mouseY, int mouseButton) {
        return false;
    }

    /**
     * Always call this method when mouse clicked is successful.
     *
     * @param soundHandler minecraft's sound handler (unused — TODO: migrate to 1.20.1 SoundManager API)
     */
    default void playPressedSound(@Nullable Object soundHandler) {
        playPressedSound(soundHandler, SoundEvents.UI_BUTTON_CLICK.value());
    }

    default void playPressedSound(@Nullable Object soundHandler, SoundEvent sound) {
        // TODO: deferred — migrate to net.minecraft.client.sounds.SoundManager in 1.20.1
    }
}
