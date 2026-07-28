package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IScreenOpenController {

    void screenOpenedByPlayer(Player player);

    void screenClosedByPlayer(Player player);
}
