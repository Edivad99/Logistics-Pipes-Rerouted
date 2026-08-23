package logisticspipes.network.packets.block;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.StaticResolve;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class SecurityStationOpenPlayer extends NBTCoordinatesPacket {

	public SecurityStationOpenPlayer(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationOpenPlayer(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (MainProxy.isClient(player.level())) {
			handleClientSide(player);
		} else {

		}
	}

	private void handleClientSide(Player player) {
		if (Minecraft.getInstance().screen instanceof GuiSecurityStation) {
			SecuritySettings setting = new SecuritySettings(null);
			setting.deserialize(TagValueInput.create(ProblemReporter.DISCARDING,
				player.level().registryAccess(), getTag()));
			((GuiSecurityStation) Minecraft.getInstance().screen).handlePlayerSecurityOpen(setting);
		}
	}
}
