package logisticspipes.network.packets.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;



import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.network.abstractpackets.IntegerPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestPipeDimension extends IntegerPacket {

	public RequestPipeDimension(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new RequestPipeDimension(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().screen instanceof GuiOrderer) {
			((GuiOrderer) Minecraft.getInstance().screen).dimension = getInteger();
			((GuiOrderer) Minecraft.getInstance().screen).refreshItems();
		} else {
			GuiOrderer.dimensioncache = getInteger();
			GuiOrderer.cachetime = System.currentTimeMillis();
		}
	}
}
