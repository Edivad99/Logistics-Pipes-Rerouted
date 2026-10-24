package logisticspipes.pipes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.Translations;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.security.SecurityPermission;
import logisticspipes.security.SecurityPermissions;
import logisticspipes.textures.PipeTexture;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.RemoteOrderer;

public class PipeItemsRemoteOrdererLogistics extends CoreRoutedPipe implements IRequestItems {

    public PipeItemsRemoteOrdererLogistics(Item item) {
        super(item);
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.REMOTE_ORDERER;
    }

    @Override
    public boolean handleClick(Player entityplayer, @Nullable SecurityPermissions permissions) {
        if (entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.REMOTE_ORDERER)) {
            if (!entityplayer.level().isClientSide()) {
                MutableComponent resp;
                if (permissions == null || permissions.allows(SecurityPermission.OPEN_REQUEST)) {
                    ItemStack orderer = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
                    RemoteOrderer.connectToPipe(orderer, this);
                    resp = Component.translatable(Translations.Chat.CONNECTED_TO_PIPE).withStyle(ChatFormatting.GREEN);
                } else {
                    resp = Component.translatable(Translations.Chat.PERMISSION_DENIED).withStyle(ChatFormatting.RED);
                }
                entityplayer.sendOverlayMessage(resp);
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable LogisticsModule getLogisticsModule() {
        return null;
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
    }

}
