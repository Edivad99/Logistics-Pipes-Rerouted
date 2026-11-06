package logisticspipes.interfaces;

import java.util.List;
import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A satellite pipe, item or fluid: a named place on the network that a crafter sends ingredients to.
 */
public interface SatellitePipe {

    @Nullable BlockEntity getContainer();

    List<ItemIdentifierStack> getItemList();

    /**
     * @return the named satellites of the same kind, this one included
     */
    Set<SatellitePipe> getSatellitesOfType();

    String getSatellitePipeName();

    void setSatellitePipeName(String satellitePipeName);

    /**
     * Tells the clients the name of the satellite.
     */
    void updateWatchers();

    /**
     * Lists the satellite among the named ones if it has a name, and takes it off if it has none.
     */
    void ensureAllSatelliteStatus();
}
