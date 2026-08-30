package logisticspipes.network.packets.orderer;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LPConfigs;
import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.IResource.ColorCode;
import logisticspipes.request.resources.ResourceNetwork;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.string.ChatColor;

@StaticResolve
public class MissingItems extends ModernPacket {

	@Getter
	@Setter
	private Collection<IResource> items = new ArrayList<>();

	@Setter
	@Getter
	private boolean flag;

	public MissingItems(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new MissingItems(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			handleClient(player);
		}
	}

	private void handleClient(Player player) {
		if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && Minecraft.getInstance().screen instanceof GuiOrderer) {
			((GuiOrderer) Minecraft.getInstance().screen)
					.handleRequestAnswer(getItems(), isFlag(), (GuiOrderer) Minecraft.getInstance().screen, player);
		} else if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && Minecraft.getInstance().screen instanceof GuiRequestTable) {
			((GuiRequestTable) Minecraft.getInstance().screen)
					.handleRequestAnswer(getItems(), isFlag(), (GuiRequestTable) Minecraft.getInstance().screen, player);
		} else if (isFlag()) {
			for (IResource item : items) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "Missing: " + item.getDisplayText(ColorCode.MISSING)));
			}
		} else {
			for (IResource item : items) {
				player.sendSystemMessage(Component.literal(ChatColor.GREEN + "Requested: " + item.getDisplayText(ColorCode.SUCCESS)));
			}
			player.sendSystemMessage(Component.literal(ChatColor.GREEN + "Request successful!"));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(items);
		output.writeBoolean(isFlag());
	}

	@Override
	public void readData(LPDataInput input) {
		items = input.readArrayList(ResourceNetwork::readResource);
		setFlag(input.readBoolean());
	}
}
