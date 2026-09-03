/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */
package network.rs485.logisticspipes.gui

import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.minecraft.client.Minecraft

/**
 * When a widget-based LP GUI (any subclass of [BaseGuiContainer]) is the active screen,
 * suppress the vanilla HUD overlays that would otherwise leak through behind the panel —
 * principally the hotbar, but also crosshair, health/food bars, and chat. Other inventory
 * screens (chest, furnace, etc.) are left alone.
 *
 * 1.20.1 renders `gui.render(...)` unconditionally while a screen is open
 * ([GameRenderer.java:947]); the screen's semi-transparent `renderBackground` gradient only
 * dims the HUD, it doesn't hide it. Cancelling the overlay pre-event is the clean fix.
 */
object WidgetScreenHudSuppressor {

    private val SUPPRESSED = setOf(
        VanillaGuiLayers.HOTBAR.toString(),
        VanillaGuiLayers.CROSSHAIR.toString(),
        VanillaGuiLayers.PLAYER_HEALTH.toString(),
        VanillaGuiLayers.FOOD_LEVEL.toString(),
        VanillaGuiLayers.ARMOR_LEVEL.toString(),
        VanillaGuiLayers.AIR_LEVEL.toString(),
        VanillaGuiLayers.VEHICLE_HEALTH.toString(),
        // 1.21.6 folded the experience bar and the horse jump meter into the contextual bar
        // system, so EXPERIENCE_BAR and JUMP_METER no longer exist as separate layers. Note that
        // EXPERIENCE_LEVEL -- the number above the bar -- is a layer of its own and, as before, is
        // deliberately left visible.
        VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND.toString(),
        VanillaGuiLayers.CONTEXTUAL_INFO_BAR.toString(),
        VanillaGuiLayers.CHAT.toString(),
    )

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderGuiLayerEvent.Pre) {
        if (Minecraft.getInstance().screen !is BaseGuiContainer<*>) return
        if (event.layer.toString() in SUPPRESSED) {
            event.isCanceled = true
        }
    }
}
