package logisticspipes.routing.channels;

import java.lang.ref.WeakReference;

import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.interfaces.routing.IChannelManagerProvider;

public class ChannelManagerProvider implements IChannelManagerProvider {

    @Nullable
    private WeakReference<Level> worldWeakReference = null;
    @Nullable
    private ChannelManager channelManager = null;

    public ChannelManagerProvider() {
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public IChannelManager getChannelManager(Level level) {
        if (worldWeakReference == null || worldWeakReference.get() == null || channelManager == null) {
            worldWeakReference = new WeakReference<>(level);
            if (channelManager != null) {
                channelManager.setChanged();
            }
            channelManager = new ChannelManager(level);
        }
        return channelManager;
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload worldEvent) {
        if (worldWeakReference != null) {
            if (worldWeakReference.get() == null || worldWeakReference.get() == worldEvent.getLevel()) {
                channelManager = null;
                worldWeakReference = null;
            }
        }
    }
}
