package logisticspipes.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;

import logisticspipes.LPConstants;
import logisticspipes.particle.LPParticleTypes;

public class LPParticleProvider extends ParticleDescriptionProvider {

    public LPParticleProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(LPParticleTypes.SPARKLE.get(), LPConstants.rl("sparkle"), 4, false);
    }
}

