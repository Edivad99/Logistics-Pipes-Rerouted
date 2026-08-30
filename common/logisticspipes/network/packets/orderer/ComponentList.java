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

@StaticResolve
public class ComponentList extends ModernPacket {

	@Getter
	@Setter
	private Collection<IResource> used = new ArrayList<>();

	@Getter
	@Setter
	private Collection<IResource> missing = new ArrayList<>();

	public ComponentList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ComponentList(getId());
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
					.handleSimulateAnswer(used, missing, (GuiOrderer) Minecraft.getInstance().screen, player);
		} else if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && Minecraft.getInstance().screen instanceof GuiRequestTable) {
			((GuiRequestTable) Minecraft.getInstance().screen)
					.handleSimulateAnswer(used, missing, (GuiRequestTable) Minecraft.getInstance().screen, player);
		} else {
			for (IResource item : used) {
				player.sendSystemMessage(Component.literal("Component: " + item.getDisplayText(ColorCode.SUCCESS)));
			}
			for (IResource item : missing) {
				player.sendSystemMessage(Component.literal("Missing: " + item.getDisplayText(ColorCode.MISSING)));
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(used);
		output.writeCollection(missing);
	}

	@Override
	public void readData(LPDataInput input) {
		used = input.readArrayList(ResourceNetwork::readResource);
		missing = input.readArrayList(ResourceNetwork::readResource);
	}
}
