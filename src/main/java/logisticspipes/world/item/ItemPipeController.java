package logisticspipes.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import logisticspipes.proxy.MainProxy;
import logisticspipes.world.inventory.PlayerSettingsMenu;

public class ItemPipeController extends LogisticsItem {

    public ItemPipeController(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand handIn) {
        if (MainProxy.isClient(level)) {
            return InteractionResult.PASS;
        }
        useItem(player, level);
        // SUCCESS_SERVER: the early return above leaves only the server side reaching this.
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (MainProxy.isClient(level)) {
            return InteractionResult.PASS;
        }
        if (player != null) {
            useItem(player, level);
        }
        return InteractionResult.SUCCESS;
    }

    private void useItem(Player player, Level level) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, viewer) -> new PlayerSettingsMenu(containerId, inventory),
                    Component.empty()));
        }
    }
}
