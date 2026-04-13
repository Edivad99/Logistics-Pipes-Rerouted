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

import logisticspipes.LPItems
import logisticspipes.LogisticsPipes
import logisticspipes.modplugins.jei.JEIPluginLoader
import logisticspipes.utils.MinecraftColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.guidebook.BookContents
import network.rs485.logisticspipes.guidebook.BookContents.MAIN_MENU_FILE
import network.rs485.logisticspipes.guidebook.DebugPage
import network.rs485.logisticspipes.guidebook.ItemGuideBook
import network.rs485.logisticspipes.util.cycleMinecraftColorId
import network.rs485.logisticspipes.util.math.MutableRectangle
import network.rs485.markdown.TextFormat
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// TODO: Rendering deferred — full 1.20.1 Screen/GuiGraphics migration pending.

object GuideBookConstants {
    const val Z_TOOLTIP = 500.0f
    const val DRAW_BODY_WIREFRAME = false
}

class GuiGuideBook(private val state: ItemGuideBook.GuideBookState) : Screen(Component.empty()) {

    private val guiBorderThickness = 16
    private val guiShadowThickness = 6
    private val guiSeparatorThickness = 6
    private val guiSliderWidth = 12
    private val guiTabWidth = 24
    private val maxTabs = 100

    private val innerGui = MutableRectangle()
    private val outerGui = MutableRectangle()
    private val sliderSeparator = MutableRectangle()
    private val visibleArea = MutableRectangle()

    private var guiSliderX = 0
    private var guiSliderY0 = 0
    private var guiSliderY1 = 0

    private lateinit var slider: SliderButton
    private lateinit var home: HomeButton
    private lateinit var addOrRemoveTabButton: BookmarkManagingButton

    private val tabButtons = state.bookmarks.map(::createGuiTabButton).toMutableList()
    private var freeColor: Int = state.bookmarks.maxOfOrNull { it.color ?: 0 } ?: 0

    private val cachedPages = state.bookmarks.plus(state.currentPage).associateBy { it.page }.toMutableMap()

    private val actionListener = ActionListener()
    private var currentProgress: Float = state.currentPage.progress
    private var clickedLinkURI: URI? = null

    inner class ActionListener {
        fun onMenuButtonClick(newPage: String) = changePage(newPage)
        fun onPageLinkClick(newPage: String) = changePage(newPage)
        fun onWebLinkClick(webLink: String) {
            try {
                clickedLinkURI = URI(webLink)
                minecraft?.setScreen(net.minecraft.client.gui.screens.ConfirmLinkScreen({ confirmed ->
                    if (confirmed) net.minecraft.Util.getPlatform().openUri(clickedLinkURI!!)
                    minecraft?.setScreen(this@GuiGuideBook)
                }, webLink, true))
            } catch (error: URISyntaxException) {
                LogisticsPipes.log.warn("Could not parse link $webLink in GuiGuideBook", error)
            }
        }
        fun onItemLinkClick(stack: ItemStack) {
            JEIPluginLoader.showRecipe(stack)
        }
    }

    init {
        if (LogisticsPipes.isDEBUG()) {
            val debugSavedPage = cachedPages.getOrPut(DebugPage.FILE) { Page(PageData(DebugPage.FILE)) }
            debugSavedPage.color = 0
            tabButtons.add(createGuiTabButton(debugSavedPage))
        }
    }

    private fun changePage(path: String) {
        val newPage = cachedPages.getOrPut(path) { Page(PageData(path)) }
        state.currentPage = newPage
        currentProgress = state.currentPage.progress
        newPage.setDrawablesPosition(visibleArea)
        updateButtonVisibility()
    }

    private fun calculateGuiConstraints() {
        val marginRatio = 1.0 / 8.0
        val sizeRatio = 6.0 / 8.0
        outerGui.setPos(floor(marginRatio * width).toInt(), floor(marginRatio * height).toInt())
            .setSize((sizeRatio * width).toInt(), (sizeRatio * height).toInt())
        innerGui.setPos(outerGui.x0 + guiBorderThickness, outerGui.y0 + guiBorderThickness)
            .setSize(outerGui.roundedWidth - 2 * guiBorderThickness, outerGui.roundedHeight - 2 * guiBorderThickness)
        sliderSeparator.setPos(innerGui.x1 - guiSliderWidth - guiSeparatorThickness - guiShadowThickness, innerGui.y0 - 1)
            .setSize(2 * guiShadowThickness + guiSeparatorThickness, innerGui.roundedHeight + 2)
        guiSliderX = innerGui.roundedRight - guiSliderWidth
        guiSliderY0 = innerGui.roundedTop
        guiSliderY1 = innerGui.roundedBottom
        visibleArea.setPos(innerGui.x0 + guiShadowThickness, innerGui.y0)
            .setSize(innerGui.roundedWidth - sliderSeparator.roundedWidth - guiSliderWidth, innerGui.roundedHeight)
        state.currentPage.setDrawablesPosition(visibleArea)
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        if (this::home.isInitialized) home.visible = state.currentPage.page != MAIN_MENU_FILE
        if (this::slider.isInitialized) {
            slider.updateSlider(state.currentPage.getExtraHeight(visibleArea), state.currentPage.progress)
        }
        var xOffset = 0
        for (button in tabButtons) {
            button.setPos(outerGui.roundedRight - 2 - 2 * guiTabWidth - xOffset, outerGui.roundedTop)
            xOffset += guiTabWidth
        }
        if (this::addOrRemoveTabButton.isInitialized) {
            addOrRemoveTabButton.updateState()
            addOrRemoveTabButton.setX(outerGui.roundedRight - 20 - guiTabWidth - xOffset)
        }
    }

    private fun isTabAbsent(page: Page): Boolean = state.bookmarks.none { it.pageEquals(page) }

    override fun init() {
        calculateGuiConstraints()
        slider = addRenderableWidget(
            SliderButton(
                x = innerGui.roundedRight - guiSliderWidth,
                y = innerGui.roundedTop,
                railHeight = innerGui.roundedHeight,
                width = guiSliderWidth,
                progress = state.currentPage.progress,
                setProgressCallback = { progress -> state.currentPage.progress = progress }
            )
        )
        home = addRenderableWidget(
            HomeButton(
                x = outerGui.roundedRight,
                y = outerGui.roundedTop
            ) { mouseButton ->
                if (mouseButton == 0) changePage(MAIN_MENU_FILE)
                true
            }
        )
        addOrRemoveTabButton = addRenderableWidget(
            BookmarkManagingButton(
                x = outerGui.roundedRight - 18 - guiTabWidth + 4,
                y = outerGui.roundedTop - 2,
                onClickAction = { buttonState ->
                    when (buttonState) {
                        BookmarkManagingButton.ButtonState.ADD -> addBookmark().let { true }
                        BookmarkManagingButton.ButtonState.REMOVE -> removeBookmark(state.currentPage)
                        BookmarkManagingButton.ButtonState.DISABLED -> false
                    }
                },
                additionStateUpdater = {
                    when {
                        state.currentPage.isBookmarkable() && isTabAbsent(state.currentPage) -> BookmarkManagingButton.ButtonState.ADD
                        !isTabAbsent(state.currentPage) -> BookmarkManagingButton.ButtonState.REMOVE
                        else -> BookmarkManagingButton.ButtonState.DISABLED
                    }
                }
            )
        )
        tabButtons.forEach { addRenderableWidget(it) }
        updateButtonVisibility()
    }

    override fun onClose() {
        LPItems.getItemGuideBook().saveState(state)
        if (LogisticsPipes.isDEBUG()) BookContents.clear()
        super.onClose()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // TODO: deferred — migrate drawScreen body to GuiGraphics
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun isPauseScreen(): Boolean = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (visibleArea.contains(mouseX.toInt(), mouseY.toInt())) {
            state.currentPage.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button, visibleArea, actionListener)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (state.currentPage.getExtraHeight(visibleArea) > 0 && this::slider.isInitialized) {
            slider.changeProgress((delta * -GuiDrawer.lpFontRenderer.getFontHeight(1.0f)).toInt())
        }
        return super.mouseScrolled(mouseX, mouseY, delta)
    }

    private fun addBookmark() = state.currentPage.takeIf { isTabAbsent(it) && tabButtons.size < maxTabs }
        ?.also { state.bookmarks.add(it); tabButtons.add(createGuiTabButton(it)) }

    private fun createGuiTabButton(tabPage: Page): TabButton =
        TabButton(tabPage, outerGui.roundedRight - 2 - 2 * guiTabWidth, outerGui.roundedTop, object : TabButtonReturn {
            override fun onLeftClick(): Boolean {
                if (!isPageActive()) { changePage(tabPage.page); return true }
                return false
            }
            override fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean): Boolean {
                if (!isPageActive()) return false
                if (ctrlClick && shiftClick) removeBookmark(tabPage) else tabPage.cycleColor(inverted = shiftClick)
                return true
            }
            override fun getColor(): Int =
                tabPage.color ?: cycleMinecraftColorId(freeColor).also { freeColor = it; tabPage.color = it }
            override fun isPageActive(): Boolean = tabPage.pageEquals(state.currentPage)
        })

    private fun removeBookmark(page: Page): Boolean {
        val removedFromState = state.bookmarks.removeIf { it.pageEquals(page) }
        val removedFromButtons = tabButtons.removeIf { it.tabPage.pageEquals(page) }
        return removedFromState || removedFromButtons
    }
}
