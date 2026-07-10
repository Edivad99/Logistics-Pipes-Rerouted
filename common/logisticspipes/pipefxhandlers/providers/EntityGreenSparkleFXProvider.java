package logisticspipes.pipefxhandlers.providers;

import logisticspipes.pipefxhandlers.GenericSparkleFactory;
import logisticspipes.pipefxhandlers.ParticleProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public class EntityGreenSparkleFXProvider implements ParticleProvider {

	float red = 0F;
	float green = 1F;
	float blue = 0F;

	@Override
	public Particle createGenericParticle(ClientLevel world, double x, double y, double z, int amount) {

		return GenericSparkleFactory.getSparkleInstance(world, x, y, z, red, green, blue, amount);

	}

}
