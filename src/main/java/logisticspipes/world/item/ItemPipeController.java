package logisticspipes.world.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

public class ItemPipeController extends LogisticsItem {

    public ItemPipeController(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} is holding a pipe controller, which unlocks the remote pipe GUIs. */
    public static boolean isHeldBy(@Nullable Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.PIPE_CONTROLLER.get());
    }
}
