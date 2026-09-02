/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */

package network.rs485.logisticspipes.module

import logisticspipes.modules.LogisticsModule
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider

/**
 * A module whose screen still opens through the old GUI provider system.
 *
 * Two providers are needed there, one to find the module in a pipe and one to find it in the
 * player's hand, differing only in that lookup. A module that has moved to a vanilla menu
 * implements [logisticspipes.interfaces.IModuleMenuProvider] instead, where one
 * [logisticspipes.network.ModuleTarget] covers both; this interface goes with the last of them.
 */
interface LegacyModuleGui {
    val module: LogisticsModule
    val pipeGuiProvider: ModuleCoordinatesGuiProvider
    val inHandGuiProvider: ModuleInHandGuiProvider

    companion object {
        @JvmStatic
        fun getPipeGuiProvider(gui: LegacyModuleGui): ModuleCoordinatesGuiProvider =
            gui.pipeGuiProvider.setSlot(gui.module.slot).setPositionInt(gui.module.positionInt)

        @JvmStatic
        fun getInHandGuiProvider(gui: LegacyModuleGui): ModuleInHandGuiProvider =
            gui.inHandGuiProvider.setInvSlot(gui.module.positionInt)
    }
}
