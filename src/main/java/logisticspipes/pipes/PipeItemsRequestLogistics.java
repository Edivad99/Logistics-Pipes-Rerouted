package logisticspipes.pipes;

import java.util.LinkedList;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.inventory.OrdererMenu;

public class PipeItemsRequestLogistics extends CoreRoutedPipe implements IRequestItems {

    private final LinkedList<Map<ItemIdentifier, Integer>> history = new LinkedList<>();

    public PipeItemsRequestLogistics(Item item) {
        super(item);
    }

    /**
     * The disk this requester holds, or an empty stack when it has no slot for one.
     *
     * <p>Only the Mk2 pipe and the request table have a disk; the plain requester answers empty so
     * that anything reading a disk can ask any requester.
     */
    public ItemStack getDisk() {
        return ItemStack.EMPTY;
    }

    @Override
    public TextureType getCenterTexture() {
        return Textures.LOGISTICSPIPE_REQUESTER_TEXTURE;
    }

    @Override
    public @Nullable LogisticsModule getLogisticsModule() {
        return null;
    }

    public void openGui(Player entityplayer) {
        if (entityplayer instanceof ServerPlayer serverPlayer) {
            OrdererMenu.open(serverPlayer, this);
        }
    }

    @Override
    public boolean handleClick(Player entityplayer, @Nullable SecuritySettings settings) {
        if (!getLevel().isClientSide()) {
            if (settings == null || settings.openRequest) {
                openGui(entityplayer);
            } else {
                entityplayer.sendSystemMessage(Component.literal("Permission denied"));
            }
        }
        return true;
    }

    @Override
    public void enabledUpdateEntity() {
        super.enabledUpdateEntity();
        if (getLevel().getGameTime() % 1200 == 0) {
            history.addLast(LogisticsManager.getAvailableItems(getRouter().getIRoutersByCost()));
            if (history.size() > 20) {
                history.removeFirst();
            }
        }
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
    }
}
