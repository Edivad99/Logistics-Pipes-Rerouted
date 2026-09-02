package logisticspipes.network.abstractpackets;

import java.util.Collections;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.packetcontent.IPacketContent;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class ModernPacket {

	/*
	@Getter
	protected String channel;
	 */
	@Getter
	private final int id;
	protected int leftRetries = 5;
	@Getter
	@Setter
	private boolean isChunkDataPacket;
	//@Getter
	//private byte[] data = null;
	@Getter
	@Setter
	private int debugId = 0;
	@Getter
	private Identifier dimension = Identifier.withDefaultNamespace("overworld");

	public List<IPacketContent<?>> content = Collections.emptyList();

	public ModernPacket(int id) {
		this.id = id;
	}

	public ModernPacket setDimension(Identifier dimension) {
		this.dimension = dimension;
		return this;
	}

	public ModernPacket setDimension(Level level) {
		this.dimension = level.dimension().identifier();
		return this;
	}

	public void readData(LPDataInput input) {
		Identifier rl = input.readIdentifier();
		dimension = rl != null ? rl : Identifier.withDefaultNamespace("overworld");
		content.forEach(it -> it.readData(input));
	}

	public abstract void processPacket(Player player);

	public void writeData(LPDataOutput output) {
		output.writeIdentifier(dimension);
		content.forEach(it -> it.writeData(output));
	}

	public abstract ModernPacket template();

	public boolean retry() {
		return leftRetries-- > 0;
	}
}
