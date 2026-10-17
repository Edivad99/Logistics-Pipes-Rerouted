package logisticspipes.pipes.upgrades;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import lombok.AllArgsConstructor;
import lombok.Getter;

import logisticspipes.network.to_client.pipe.UpgradeConfigPopupMessage;

public class ConnectionUpgradeConfig implements IConfigPipeUpgrade {

    public static String getName() {
        return "disconnection";
    }

    public static Stream<Direction> getSides(ItemStack stack) {
        if (stack.isEmpty()) {
            return Stream.empty();
        }
        final CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Arrays.stream(Sides.values()).filter(side -> tag.getBooleanOr(side.getLpName(), false))
            .map(Sides::getDir);
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public List<UpgradeTarget> getAllowedPipes() {
        return List.of(UpgradeTarget.ALL);
    }

    @Override
    public List<UpgradeTarget> getAllowedModules() {
        return List.of();
    }

    @Override
    public UpgradeConfigPopupMessage.Kind getConfigPopup() {
        return UpgradeConfigPopupMessage.Kind.DISCONNECTION;
    }

    @AllArgsConstructor
    public enum Sides {
        UP(Direction.UP, "LPDIS-UP"),
        DOWN(Direction.DOWN, "LPDIS-DOWN"),
        NORTH(Direction.NORTH, "LPDIS-NORTH"),
        SOUTH(Direction.SOUTH, "LPDIS-SOUTH"),
        EAST(Direction.EAST, "LPDIS-EAST"),
        WEST(Direction.WEST, "LPDIS-WEST");
        @Getter
        private Direction dir;
        @Getter
        private String lpName;

        public static String getNameForDirection(Direction fd) {
            Optional<Sides> opt = Arrays.stream(values()).filter(side -> side.getDir() == fd).findFirst();
            if (opt.isPresent()) {
                return opt.get().getLpName();
            }
            return "LPDIS-UNKNWON";
        }
    }
}
