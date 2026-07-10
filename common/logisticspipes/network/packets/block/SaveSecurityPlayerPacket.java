package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class SaveSecurityPlayerPacket extends NBTCoordinatesPacket {

	public SaveSecurityPlayerPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SaveSecurityPlayerPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.saveNewSecuritySettings(getTag());
		}
	}
}
