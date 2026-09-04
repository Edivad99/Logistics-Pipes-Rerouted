package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

/**
 * Something that wants to know who is looking at it.
 *
 * <p>Both menu bases call this: a block or pipe that pushes updates only to the players with its
 * screen open needs to be told when that list changes.
 */
public interface IScreenOpenController {

    void screenOpenedByPlayer(Player player);

    void screenClosedByPlayer(Player player);
}
