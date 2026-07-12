package logisticspipes.network.packets.pipe;

import java.util.Objects;
import javax.annotation.Nonnull;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipePropertiesUpdate extends CoordinatesPacket {

	@Nonnull
	public CompoundTag tag = new CompoundTag();

	public PipePropertiesUpdate(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundTag(tag);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		tag = Objects.requireNonNull(input.readCompoundTag(), "read null NBT in PipePropertiesUpdate");
	}

	@Override
	public ModernPacket template() {
		return new PipePropertiesUpdate(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe tile = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (!(tile.pipe instanceof PropertyHolder pipePropertyHolder)) {
			return;
		}

		// sync updated properties
		tile.pipe.readFromNBT(tag, player.registryAccess());

		MainProxy.runOnServer(player.level(), () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder(pipePropertyHolder, player.registryAccess()).setPacketPos(this), player);
		});
	}

	@Nonnull
	public static PipePropertiesUpdate fromPropertyHolder(PropertyHolder holder, HolderLookup.Provider provider) {
		final PipePropertiesUpdate packet = PacketHandler.getPacket(PipePropertiesUpdate.class);
		PropertyHolder.writeToNBT(packet.tag, provider, holder);
		return packet;
	}

}
