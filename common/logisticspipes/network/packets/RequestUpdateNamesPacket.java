package logisticspipes.network.packets;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraftforge.registries.ForgeRegistries;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RequestUpdateNamesPacket extends ModernPacket {

	public RequestUpdateNamesPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		// fillItemCategory/getCreativeTabs removed in 1.20.1 — iterate registry directly
		List<ItemIdentifier> identList = new LinkedList<>();
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (item != null) {
				identList.add(ItemIdentifier.get(item, 0, null));
			}
		}
		SimpleServiceLocator.clientBufferHandler.setPause(true);
		for (ItemIdentifier item : identList) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(UpdateName.class).setIdent(item).setName(item.getFriendlyName()));
		}
		SimpleServiceLocator.clientBufferHandler.setPause(false);
		Minecraft.getInstance().player.sendSystemMessage(Component.literal("Names in send Queue"));
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new RequestUpdateNamesPacket(getId());
	}
}
