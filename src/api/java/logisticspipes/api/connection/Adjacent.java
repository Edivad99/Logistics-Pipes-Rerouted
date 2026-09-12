package logisticspipes.api.connection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface Adjacent {

    Map<BlockPos, ConnectionType> connectedPos();

    @Nullable ConnectionType get(Direction direction);

    Optional<ConnectionType> optionalGet(Direction direction);

    Map<NeighborBlockEntity<BlockEntity>, ConnectionType> neighbors();

    List<NeighborBlockEntity<BlockEntity>> inventories();

    List<NeighborBlockEntity<BlockEntity>> fluidTanks();
}
