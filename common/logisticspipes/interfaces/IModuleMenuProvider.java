package logisticspipes.interfaces;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;

/**
 * A module that opens its own settings screen.
 *
 * <p>Not a {@link MenuProvider} itself: a module is not addressed by position -- it may be held in
 * hand -- so the target travels with the menu and the provider is made around it.
 */
public interface IModuleMenuProvider {

    AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target);

    /**
     * Anything else the client needs before the menu can be built. The target is already written.
     *
     * <p>For state the module keeps server side and the screen draws: the client's copy of a
     * module in a pipe is not the one being configured.
     */
    default void writeMenuData(RegistryFriendlyByteBuf buffer) {
    }

    /** Opens the module's screen, telling the client where to look for the module. */
    static void open(ServerPlayer player, LogisticsModule module) {
        if (!(module instanceof IModuleMenuProvider provider)) {
            return;
        }
        final ModuleTarget target = ModuleTarget.of(module);
        player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, viewer) -> provider.createMenu(containerId, inventory, target),
                        Component.empty()),
                buffer -> {
                    ModuleTarget.STREAM_CODEC.encode(buffer, target);
                    provider.writeMenuData(buffer);
                });
    }
}
