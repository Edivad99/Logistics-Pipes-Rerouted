package logisticspipes.pipefxhandlers;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

public class GenericSparkleFactory {

	public static EntitySparkleFX getSparkleInstance(Level world, double x, double y, double z, float red, float green, float blue, int amount) {

		EntitySparkleFX effect;

		float boundry = 0.4F;
		int pipeWidth = 3;

		float width = boundry + world.getRandom().nextInt(pipeWidth) / 10.0F;
		float length = boundry + world.getRandom().nextInt(pipeWidth) / 10.0F;
		float height = world.getRandom().nextInt(7) / 10.0F + 0.2F;

		float scalemult = 1f + (float) Math.log10(amount);

		effect = new EntitySparkleFX((ClientLevel) world, x + length, y + height, z + width, scalemult, red, green, blue, 6 + world.getRandom().nextInt(3));

		return effect;
	}

}
