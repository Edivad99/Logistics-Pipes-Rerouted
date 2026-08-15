package logisticspipes.integrations.refinedstorage;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

/**
 * Finds the Refined Storage network behind a block, shared by the item and fluid handlers.
 *
 * <p>Every class here touches {@code com.refinedmods} types, so it must only be loaded once RS is
 * known to be present -- see {@code SpecialInventoryHandlerManager}.</p>
 */
public final class RSNetworks {

    /**
     * Named rather than {@link Actor#EMPTY} so RS attributes the change to us in its resource
     * tracking, which is what the "last modified by" column in a Grid shows.
     */
    public static final Actor ACTOR = () -> "Logistics Pipes";

    private RSNetworks() {
    }

    /**
     * The storage of the network {@code blockEntity} belongs to, or null when there is none to talk to.
     */
    @Nullable
    public static StorageNetworkComponent findStorage(BlockEntity blockEntity, @Nullable Direction dir) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }
        NetworkNodeContainerProvider provider = RSNetworks.findProvider(blockEntity, level, dir);
        if (provider == null) {
            return null;
        }
        for (NetworkNodeContainer container : provider.getContainers()) {
            NetworkNode node = container.getNode();
            if (node == null) {
                continue;
            }
            Network network = node.getNetwork();
            if (network == null) {
                // Not connected to anything yet; nothing to expose rather than an empty network.
                continue;
            }
            StorageNetworkComponent component = network.getComponent(StorageNetworkComponent.class);
            if (component != null) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkNodeContainerProvider findProvider(BlockEntity blockEntity, Level level,
        @Nullable Direction dir) {
        // The five-argument overload: the short one looks the state and block entity up through
        // getChunk(), a blocking chunk load, and this runs from the pipe adjacency scan.
        NetworkNodeContainerProvider provider = level.getCapability(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, dir);
        if (provider != null || dir == null) {
            return provider;
        }
        // Capability providers frequently register for the null context only, in which case the
        // side-specific lookup above finds nothing even though the block does host a node.
        return level.getCapability(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
    }
}
