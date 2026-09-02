package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;

/**
 * The satellite naming screen has no slots at all, but still needs a menu to hang on.
 */
public class SatelliteMenu extends DummyMenu {

    @Getter
    private final SatellitePipe pipe;

    public SatelliteMenu(int containerId, Inventory inventory, SatellitePipe pipe) {
        super(LPMenuTypes.SATELLITE.get(), containerId, inventory.player, ((CoreUnroutedPipe) pipe).container);
        this.pipe = pipe;
    }
}
