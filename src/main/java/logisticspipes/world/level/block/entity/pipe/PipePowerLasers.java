package logisticspipes.world.level.block.entity.pipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.Direction;

import logisticspipes.LPConfigs;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PowerLaserMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The power lasers a pipe shows. The server keeps each one alive for as long as power keeps flowing
 * through it, and tells the clients watching the chunk when one appears or goes away; drawing them is
 * {@code PowerLaserParticles}' job on the client.
 */
public class PipePowerLasers {

    private static final int LASER_TIMEOUT_TICKS = 4;

    private final LogisticsTileGenericPipe pipe;
    private final Map<LaserKey, Beam> beams = new HashMap<>();
    /** Ticks left for the ball of each colour. */
    private final Map<Integer, Integer> balls = new HashMap<>();

    private record LaserKey(Direction dir, int color) {}

    private static final class Beam {

        private final float length;
        private final boolean reverse;
        private int timeout = LASER_TIMEOUT_TICKS;

        private Beam(float length, boolean reverse) {
            this.length = length;
            this.reverse = reverse;
        }
    }

    public PipePowerLasers(LogisticsTileGenericPipe pipe) {
        this.pipe = pipe;
    }

    public void onUpdate() {
        Iterator<Map.Entry<LaserKey, Beam>> beamIter = beams.entrySet().iterator();
        while (beamIter.hasNext()) {
            Map.Entry<LaserKey, Beam> entry = beamIter.next();
            if (--entry.getValue().timeout < 0) {
                LaserKey key = entry.getKey();
                TargetLookup.sendToChunkWatchers(pipe,
                    PowerLaserMessage.remove(pipe.getBlockPos(), key.dir(), key.color(), false));
                beamIter.remove();
            }
        }
        Iterator<Map.Entry<Integer, Integer>> ballIter = balls.entrySet().iterator();
        while (ballIter.hasNext()) {
            Map.Entry<Integer, Integer> entry = ballIter.next();
            int timeout = entry.getValue() - 1;
            if (timeout < 0) {
                TargetLookup.sendToChunkWatchers(pipe,
                    PowerLaserMessage.remove(pipe.getBlockPos(), null, entry.getKey(), true));
                ballIter.remove();
            } else {
                entry.setValue(timeout);
            }
        }
    }

    public void addLaser(Direction dir, float length, int color, boolean reverse, boolean renderBall) {
        if (pipe.getLevel().isClientSide() || !LPConfigs.COMMON.ENABLE_PARTICLE_FX.getAsBoolean()) {
            return;
        }
        boolean sendPacket = false;
        Beam beam = beams.get(new LaserKey(dir, color));
        if (beam != null) {
            beam.timeout = LASER_TIMEOUT_TICKS;
        } else {
            beams.put(new LaserKey(dir, color), new Beam(length, reverse));
            sendPacket = true;
        }
        if (renderBall) {
            if (balls.put(color, LASER_TIMEOUT_TICKS) == null) {
                sendPacket = true;
            }
        }
        if (sendPacket) {
            TargetLookup.sendToChunkWatchers(pipe,
                PowerLaserMessage.add(pipe.getBlockPos(), dir, color, length, reverse, renderBall));
        }
    }

    public void sendInit() {
        beams.forEach((key, beam) -> TargetLookup.sendToChunkWatchers(pipe, PowerLaserMessage.add(
            pipe.getBlockPos(), key.dir(), key.color(), beam.length, beam.reverse, balls.containsKey(key.color()))));
    }
}
