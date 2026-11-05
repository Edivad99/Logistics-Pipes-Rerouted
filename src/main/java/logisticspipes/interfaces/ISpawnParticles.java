package logisticspipes.interfaces;

import logisticspipes.particle.Particles;

/**
 * Something that can show particles at its position.
 */
public interface ISpawnParticles {

    void spawnParticle(Particles particle, int amount);
}
