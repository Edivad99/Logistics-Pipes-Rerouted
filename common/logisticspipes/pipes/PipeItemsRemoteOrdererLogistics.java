package logisticspipes.pipes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.RemoteOrderer;

public class PipeItemsRemoteOrdererLogistics extends CoreRoutedPipe implements IRequestItems {

	public PipeItemsRemoteOrdererLogistics(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_REMOTE_ORDERER_TEXTURE;
	}

	@Override
	public boolean handleClick(Player entityplayer, @Nullable SecuritySettings settings) {
		if (entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.REMOTE_ORDERER)) {
			if (!entityplayer.level().isClientSide()) {
                MutableComponent resp;
				if (settings == null || settings.openRequest) {
					ItemStack orderer = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
					RemoteOrderer.connectToPipe(orderer, this);
                    resp = Component.translatable("lp.chat.connectedtopipe").withStyle(ChatFormatting.GREEN);
				} else {
                    resp = Component.translatable("lp.chat.permissiondenied").withStyle(ChatFormatting.RED);
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
		return ItemSendMode.Normal;
	}

}
