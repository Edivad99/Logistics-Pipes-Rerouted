package logisticspipes.network.packets.pipe;

import java.util.ArrayList;
import java.util.Collection;
import logisticspipes.interfaces.ISpawnParticles.ParticleCount;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipefxhandlers.PipeFXRenderHandler;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ParticleFX extends CoordinatesPacket {

	@Getter
	@Setter
	@NonNull
	private Collection<ParticleCount> particles;

	public ParticleFX(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ParticleFX(getId());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		int nparticles = input.readInt();
		particles = new ArrayList<>(nparticles);
		for (int i = 0; i < nparticles; i++) {
			int particle = input.readByte();
			int amount = input.readInt();
			particles.add(new ParticleCount(Particles.values()[particle], amount));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(particles.size());
		for (ParticleCount pc : particles) {
			output.writeByte(pc.getParticle().ordinal());
			output.writeInt(pc.getAmount());
		}
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().options.graphicsMode().get().getId() < 1) { // isFancyGraphicsEnabled removed — check graphicsMode >= 1 (Fancy)
			return;
		}
		for (ParticleCount pc : particles) {
			PipeFXRenderHandler.spawnGenericParticle(pc.getParticle(), getPosX(), getPosY(), getPosZ(), pc.getAmount());
		}
	}
}
