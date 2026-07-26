package logisticspipes.world.level.block.entity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

import logisticspipes.utils.PlayerIdentifier;

public class AutoCraftingContainer extends TransientCraftingContainer {

    public final PlayerIdentifier placedByPlayer;

    public AutoCraftingContainer(PlayerIdentifier playerID) {
        super(new AbstractContainerMenu(null, 0) {

            @Override
            public boolean stillValid(Player entityplayer) {
                return false;
            }

            @Override
            public ItemStack quickMoveStack(Player player, int i) {
                return ItemStack.EMPTY;
            }
        }, 3, 3);
        placedByPlayer = playerID;
    }
}
