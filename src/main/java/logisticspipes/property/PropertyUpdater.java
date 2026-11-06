package logisticspipes.property;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.api.property.PropertyUtil;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.ModulePropertiesMessage;
import logisticspipes.utils.LPExecutors;

/**
 * Sends the changed properties of a module to the player who has its menu open, batched a few ticks apart.
 */
public class PropertyUpdater {

    private final WeakReference<ServerPlayer> weakPlayer;

    // One stable instance: a fresh method reference per call site would never compare equal
    // when removing the observer again.
    private final ObserverCallback<Object> observer = this::onPropertyChanged;

    private final List<Property<?>> properties;
    private final Set<Property<?>> changedProperties = new HashSet<>();
    private final LogisticsModule module;

    private boolean shouldUpdate = false;

    public PropertyUpdater(ServerPlayer player, LogisticsModule module, List<Property<?>> properties) {
        this.weakPlayer = new WeakReference<>(player);
        this.properties = properties;
        this.module = module;
        PropertyUtil.addObserver(properties, observer);
    }

    private void onPropertyChanged(Property<?> property) {
        changedProperties.add(property);
        if (!shouldUpdate) {
            shouldUpdate = true;
            LPExecutors.scheduleServerTask(5, this::sendPropertyUpdate);
        }
    }

    private void sendPropertyUpdate() {
        if (!shouldUpdate) {
            return;
        }
        ServerPlayer player = weakPlayer.get();
        if (player == null) {
            return;
        }
        TagValueOutput output =
            TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
        PropertyUtil.serialize(changedProperties, output);
        changedProperties.clear();
        PacketDistributor.sendToPlayer(player, new ModulePropertiesMessage(ModuleTarget.of(module), output.buildResult()));
        shouldUpdate = false;
    }

    boolean removeForPlayer(Player closingPlayer) {
        ServerPlayer player = weakPlayer.get();
        boolean shouldBeRemoved = player == null || player == closingPlayer;
        if (shouldBeRemoved) {
            PropertyUtil.removeObserver(properties, observer);
            shouldUpdate = false;
        }
        return shouldBeRemoved;
    }
}
