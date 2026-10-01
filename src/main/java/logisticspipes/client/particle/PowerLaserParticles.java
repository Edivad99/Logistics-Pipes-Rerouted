package logisticspipes.client.particle;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;

/**
 * The power-laser particles on screen, kept by pipe, side and colour so that the server's message
 * to take a laser away can find the particle it spawned.
 *
 * <p>The particles do not expire on their own: the server sends a removal once a laser stops being
 * refreshed. Switching level drops every particle without removing it, so the maps are cleared
 * whenever a different level shows up.</p>
 */
public final class PowerLaserParticles {

    private record BeamKey(BlockPos pos, Direction dir, int color) {
    }

    private record BallKey(BlockPos pos, int color) {
    }

    private static final Map<BeamKey, PowerLaserBeamParticle> BEAMS = new HashMap<>();
    private static final Map<BallKey, PowerLaserBallParticle> BALLS = new HashMap<>();
    private static @Nullable ClientLevel currentLevel;

    private PowerLaserParticles() {
    }

    public static void add(ClientLevel level, BlockPos pos, Direction dir, int color, float length, boolean reverse,
        boolean renderBall) {
        if (!LPConfigs.COMMON.ENABLE_PARTICLE_FX.getAsBoolean()) {
            return;
        }
        if (level != currentLevel) {
            BEAMS.clear();
            BALLS.clear();
            currentLevel = level;
        }

        BeamKey beamKey = new BeamKey(pos, dir, color);
        PowerLaserBeamParticle beam = BEAMS.get(beamKey);
        if (beam == null || !beam.isAlive()) {
            beam = new PowerLaserBeamParticle(level, pos, length, dir, color).setReverse(reverse);
            Minecraft.getInstance().particleEngine.add(beam);
            BEAMS.put(beamKey, beam);
        }
        if (renderBall) {
            BallKey ballKey = new BallKey(pos, color);
            PowerLaserBallParticle ball = BALLS.get(ballKey);
            if (ball == null || !ball.isAlive()) {
                ball = new PowerLaserBallParticle(level, pos, color);
                Minecraft.getInstance().particleEngine.add(ball);
                BALLS.put(ballKey, ball);
            }
        }
    }

    public static void remove(BlockPos pos, @Nullable Direction dir, int color, boolean isBall) {
        if (isBall) {
            PowerLaserBallParticle ball = BALLS.remove(new BallKey(pos, color));
            if (ball != null) {
                ball.remove();
            }
        } else if (dir != null) {
            PowerLaserBeamParticle beam = BEAMS.remove(new BeamKey(pos, dir, color));
            if (beam != null) {
                beam.remove();
            }
        }
    }
}
