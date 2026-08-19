package logisticspipes.pipes;

import javax.annotation.Nullable;
import logisticspipes.world.item.LPItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.world.item.RemoteOrderer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
		if (!entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() &&
				entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).is(LPItems.REMOTE_ORDERER)) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openRequest) {
					ItemStack orderer = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
					RemoteOrderer.connectToPipe(orderer, this);
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.connectedtopipe"));
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
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
