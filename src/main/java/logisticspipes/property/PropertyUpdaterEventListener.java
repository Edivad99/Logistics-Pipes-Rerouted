package logisticspipes.property;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.world.inventory.ModuleMenu;

public final class PropertyUpdaterEventListener {

    public static final PropertyUpdaterEventListener INSTANCE = new PropertyUpdaterEventListener();

    private final List<PropertyUpdater> propertyUpdaters = new ArrayList<>();

    private PropertyUpdaterEventListener() {
    }

    @SubscribeEvent
    public void openContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof ModuleMenu menu) {
            LogisticsModule module = menu.getModule();
            propertyUpdaters.add(new PropertyUpdater(player, module, module.getProperties()));
        }
    }

    @SubscribeEvent
    public void closeContainer(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            propertyUpdaters.removeIf(propertyUpdater -> propertyUpdater.removeForPlayer(player));
        }
    }
}
