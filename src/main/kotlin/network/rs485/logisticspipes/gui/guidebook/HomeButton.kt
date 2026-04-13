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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED.
 */

package network.rs485.logisticspipes.gui.guidebook

import net.minecraft.client.gui.GuiGraphics
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.TextUtil
import network.rs485.logisticspipes.util.math.MutableRectangle

// TODO: Rendering deferred — HomeButton migrated to 1.20.1 stub.

private val homeButtonTexture = Rectangle(16, 64, 24, 32)
private val homeIconTexture = Rectangle(128, 0, 16, 16)

class HomeButton(x: Int, y: Int, onClickAction: (Int) -> Boolean) :
    LPGuiButton(1, x - 24, y - 24, homeButtonTexture.roundedWidth, homeButtonTexture.roundedHeight) {

    override val bodyTrigger = Rectangle(1, 1, 22, 22)

    init {
        this.setOnClickAction(onClickAction)
    }

    override fun setPos(newX: Int, newY: Int) {
        body.setPos(newX - 24, newY + 8)
    }

    override fun getTooltipText(): String = TextUtil.translate("misc.guide_book.home_button")

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // TODO: deferred rendering
    }
}
