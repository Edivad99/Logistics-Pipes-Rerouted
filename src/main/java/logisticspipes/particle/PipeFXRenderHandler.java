package logisticspipes.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;

public class PipeFXRenderHandler {

    public static void spawnGenericParticle(Particles particle,
        double x, double y, double z, int amount) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        boolean isAll = mc.options.particles().get().equals(ParticleStatus.ALL);
        double distance = 16.0D;
        if (mc.getCameraEntity().distanceToSqr(x, y, z) > distance * distance) {
            return;
        } else if (!isAll) {
            return;
        }

        for (int i = 0; i < Math.sqrt(amount); i++) {
            level.addParticle(particle.getSparkleFXParticleOptions(amount), x, y, z, 0, 0, 0);
        }
    }
}
