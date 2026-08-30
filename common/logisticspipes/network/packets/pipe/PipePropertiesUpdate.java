package logisticspipes.network.packets.pipe;

import java.util.Objects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.property.PropertyHolder;

@StaticResolve
public class PipePropertiesUpdate extends CoordinatesPacket {

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
		tile.pipe.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag));

		MainProxy.runOnServer(player.level(), () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder(pipePropertyHolder, player.registryAccess()).setPacketPos(this), player);
		});
	}

	public static PipePropertiesUpdate fromPropertyHolder(PropertyHolder holder, HolderLookup.Provider provider) {
		final PipePropertiesUpdate packet = PacketHandler.getPacket(PipePropertiesUpdate.class);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
		PropertyHolder.serialize(output, holder);
		packet.tag = output.buildResult();
		return packet;
	}

}
