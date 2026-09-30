package logisticspipes.world.level.block.entity.pipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import logisticspipes.LPConfigs;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PowerLaserMessage;
import logisticspipes.pipefxhandlers.PipeFXLaserPowerBall;
import logisticspipes.pipefxhandlers.PipeFXLaserPowerBeam;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The power lasers a pipe shows. The server tracks them and tells nearby clients, which draw them as particles.
 */
public class PipePowerLasers {

    private static final int LASER_TIMEOUT_TICKS = 4;

    private final LogisticsTileGenericPipe pipe;
    private final Map<LaserKey, LaserBeamData> powerLasersBeam = new HashMap<>();
    private final Map<Integer, LaserBallData> powerLasersBall = new HashMap<>();

    private record LaserKey(Direction dir, int color) {}

    @Data
    @AllArgsConstructor
    private static class LaserBeamData {

        final float length;
        int timeout;
        final boolean reverse;

        boolean isDeadEntity() {
            return false;
        }

        void setDead() {}

        boolean sendPacket() {
            return true;
        }

        void tick() {
            timeout--;
        }
    }

    private class LaserBeamDataClient extends LaserBeamData {

        @Getter
        final PipeFXLaserPowerBeam entity;

        public LaserBeamDataClient(float length, int timeout, boolean reverse, Direction dir, int color) {
            super(length, timeout, reverse);
            entity = new PipeFXLaserPowerBeam((ClientLevel) pipe.getLevel(), pipe.getBlockPos(), length,
                dir, color, pipe).setReverse(reverse);
            Minecraft.getInstance().particleEngine.add(entity);
        }

        @Override
        boolean isDeadEntity() {
            return !entity.isAlive();
        }

        @Override
        void setDead() {
            entity.remove();
        }

        @Override
        boolean sendPacket() {
            return false;
        }

        @Override
        void tick() {}
    }

    @Data
    @AllArgsConstructor
    private static class LaserBallData {

        final float length;
        int timeout;

        boolean isDeadEntity() {
            return false;
        }

        void setDead() {}

        boolean sendPacket() {
            return true;
        }

        void tick() {
            timeout--;
        }
    }

    private class LaserBallDataClient extends LaserBallData {

        @Getter
        final PipeFXLaserPowerBall entity;

        public LaserBallDataClient(float length, int timeout, int color) {
            super(length, timeout);
            entity = new PipeFXLaserPowerBall((ClientLevel) pipe.getLevel(), pipe.getBlockPos(), color, pipe);
            Minecraft.getInstance().particleEngine.add(entity);
        }

        @Override
        boolean isDeadEntity() {
            return !entity.isAlive();
        }

        @Override
        void setDead() {
            entity.remove();
        }

        @Override
        boolean sendPacket() {
            return false;
        }

        @Override
        void tick() {}
    }

    public PipePowerLasers(LogisticsTileGenericPipe pipe) {
        this.pipe = pipe;
    }

    public void onUpdate() {
        {
            Iterator<LaserKey> iter = powerLasersBeam.keySet().iterator();
            while (iter.hasNext()) {
                LaserKey key = iter.next();
                LaserBeamData data = powerLasersBeam.get(key);
                data.tick();
                if (data.timeout < 0 || data.isDeadEntity()) {
                    data.setDead();
                    if (data.sendPacket()) {
                        TargetLookup.sendToChunkWatchers(pipe,
                                PowerLaserMessage.remove(pipe.getBlockPos(), key.dir, key.color, false));
                    }
                    iter.remove();
                }
            }
        }
        {
            Iterator<Integer> iter = powerLasersBall.keySet().iterator();
            while (iter.hasNext()) {
                Integer key = iter.next();
                LaserBallData data = powerLasersBall.get(key);
                data.tick();
                if (data.timeout < 0 || data.isDeadEntity()) {
                    data.setDead();
                    if (data.sendPacket()) {
                        TargetLookup.sendToChunkWatchers(pipe,
                                PowerLaserMessage.remove(pipe.getBlockPos(), null, key, true));
                    }
                    iter.remove();
                }
            }
        }
    }

    public void addLaser(Direction dir, float length, int color, boolean reverse, boolean renderBall) {
        if (!LPConfigs.COMMON.ENABLE_PARTICLE_FX.getAsBoolean()) {
            return;
        }
        boolean sendPacket = false;
        if (powerLasersBeam.containsKey(new LaserKey(dir, color))) {
            powerLasersBeam.get(new LaserKey(dir, color)).timeout = LASER_TIMEOUT_TICKS;
        } else {
            if (pipe.getLevel().isClientSide()) {
                powerLasersBeam.put(new LaserKey(dir, color),
                    new LaserBeamDataClient(length, LASER_TIMEOUT_TICKS, reverse, dir, color));
            } else {
                powerLasersBeam.put(new LaserKey(dir, color), new LaserBeamData(length, LASER_TIMEOUT_TICKS, reverse));
                sendPacket = true;
            }
        }
        if (renderBall) {
            if (powerLasersBall.containsKey(color)) {
                powerLasersBall.get(color).timeout = LASER_TIMEOUT_TICKS;
            } else {
                if (pipe.getLevel().isClientSide()) {
                    powerLasersBall.put(color, new LaserBallDataClient(length, LASER_TIMEOUT_TICKS, color));
                } else {
                    powerLasersBall.put(color, new LaserBallData(length, LASER_TIMEOUT_TICKS));
                    sendPacket = true;
                }
            }
        }
        if (sendPacket) {
            TargetLookup.sendToChunkWatchers(pipe,
                    PowerLaserMessage.add(pipe.getBlockPos(), dir, color, length, reverse, renderBall));
        }
    }

    public void removeLaser(Direction dir, int color, boolean isBall) {
        if (!pipe.getLevel().isClientSide()) {
            return;
        }
        if (!isBall) {
            LaserKey key = new LaserKey(dir, color);
            LaserBeamData beam = powerLasersBeam.get(key);
            if (beam != null) {
                beam.timeout = -1;
                if (pipe.getLevel().isClientSide()) {
                    ((LaserBeamDataClient) beam).entity.remove();
                }
                powerLasersBeam.remove(key);
            }
        } else {
            LaserBallData ball = powerLasersBall.get(color);
            if (ball != null) {
                ball.timeout = -1;
                if (pipe.getLevel().isClientSide()) {
                    ((LaserBallDataClient) ball).entity.remove();
                }
                powerLasersBall.remove(color);
            }
        }
    }

    public void sendInit() {
        for (LaserKey key : powerLasersBeam.keySet()) {
            LaserBeamData data = powerLasersBeam.get(key);
            boolean isBall = powerLasersBall.containsKey(key.color);
            TargetLookup.sendToChunkWatchers(pipe, PowerLaserMessage.add(
                    pipe.getBlockPos(), key.dir, key.color, data.length, data.reverse, isBall));
        }
    }
}
