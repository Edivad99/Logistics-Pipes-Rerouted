package logisticspipes.network.packets.block;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityStationId extends CoordinatesPacket {

	@Getter
	@Setter
	private UUID uuid;

	public SecurityStationId(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationId(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.setClientUUID(getUuid());
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeLong(uuid.getMostSignificantBits());
		output.writeLong(uuid.getLeastSignificantBits());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		uuid = new UUID(input.readLong(), input.readLong());
	}
}
