package logisticspipes.transport;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsGenericSubMultiBlockBlockEntity;

/**
 * The transport of the high speed tubes. A tube spans several blocks: the pipe sits in one of them
 * and the others are sub blocks pointing back at it, so the length, the exits and the block behind
 * each exit all come from the tube itself. Items only ever go on into another tube.
 */
public class PipeMultiBlockTransportLogistics extends PipeTransportLogistics {

    private static final float SPEED = 0.8F;

    public PipeMultiBlockTransportLogistics() {
        super(false);
    }

    /**
     * Connects only to another tube, or to a sub block whose tubes all are.
     */
    @Override
    public boolean canPipeConnect(BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipe) {
            return pipe.pipe != null && pipe.pipe.isHSTube();
        }
        if (blockEntity instanceof LogisticsGenericSubMultiBlockBlockEntity subBlock) {
            List<LogisticsGenericPipeBlockEntity> mainPipes = subBlock.getMainPipe();
            return !mainPipes.isEmpty()
                && mainPipes.stream().allMatch(pipe -> pipe.pipe != null && pipe.pipe.isHSTube());
        }
        return false;
    }

    public @Nullable CoreMultiBlockPipe getMultiPipe() {
        return getPipe() instanceof CoreMultiBlockPipe multiPipe ? multiPipe : null;
    }

    @Override
    public float getPipeLength() {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        return multiPipe != null ? multiPipe.getPipeLength() : super.getPipeLength();
    }

    @Override
    public double getDistanceWeight() {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        return multiPipe != null ? multiPipe.getDistanceWeight() : super.getDistanceWeight();
    }

    @Override
    public float getYawDiff(LPTravelingItem item) {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        return multiPipe != null ? multiPipe.getYawDiff(item) : super.getYawDiff(item);
    }

    /**
     * A tube has no choice to make: what comes in on one end goes out the other.
     */
    @Override
    RoutingResult resolveDestination(LPTravelingItemServer data) {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        if (multiPipe == null) {
            return new RoutingResult(null, false);
        }
        return new RoutingResult(getFixedExit(data.input), true);
    }

    @Override
    public @Nullable Direction getFixedExit(Direction input) {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        return multiPipe != null ? multiPipe.getExitForInput(input.getOpposite()) : null;
    }

    @Override
    protected float getFixedSpeed() {
        return SPEED;
    }

    @Override
    protected @Nullable BlockEntity getBlockEntityAtExit(Direction output) {
        CoreMultiBlockPipe multiPipe = getMultiPipe();
        return multiPipe != null ? multiPipe.getBlockEntityAtExit(output) : null;
    }

    @Override
    protected void handleBlockEntityReachedServer(LPTravelingItemServer arrivingItem, @Nullable BlockEntity blockEntity,
        Direction dir) {
        markChunkModified(blockEntity);
        handOn(arrivingItem, blockEntity);
    }

    @Override
    protected void handleBlockEntityReachedClient(LPTravelingItemClient arrivingItem, @Nullable BlockEntity blockEntity,
        Direction dir) {
        handOn(arrivingItem, blockEntity);
    }

    /**
     * Passes the item on to the next tube, or marks it with an explosion if there is none: a tube
     * has nowhere else to put it.
     */
    private void handOn(LPTravelingItem item, @Nullable BlockEntity blockEntity) {
        BlockEntity next = null;
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipe && pipe.pipe instanceof CoreMultiBlockPipe) {
            next = blockEntity;
        } else if (blockEntity instanceof LogisticsGenericSubMultiBlockBlockEntity subBlock) {
            next = mainPipeOf(subBlock);
        }
        if (next != null) {
            passToNextPipe(item, next);
        } else {
            spawnMisroutedItemExplosionEffect();
        }
    }

    @Override
    public void readjustSpeed(LPTravelingItemServer item) {
        item.setSpeed(SPEED);
    }

    @Override
    public @Nullable CoreUnroutedPipe getNextPipe(Direction output) {
        if (getBlockEntityAtExit(output) instanceof LogisticsGenericSubMultiBlockBlockEntity subBlock) {
            LogisticsGenericPipeBlockEntity mainPipe = mainPipeOf(subBlock);
            return mainPipe != null ? mainPipe.pipe : null;
        }
        return super.getNextPipe(output);
    }

    /**
     * The tube a sub block belongs to, or null if it belongs to none.
     */
    private static @Nullable LogisticsGenericPipeBlockEntity mainPipeOf(
        LogisticsGenericSubMultiBlockBlockEntity subBlock) {
        List<LogisticsGenericPipeBlockEntity> mainPipes = subBlock.getMainPipe();
        if (mainPipes.size() > 1) {
            throw new UnsupportedOperationException(
                "Sub block at " + subBlock.getBlockPos() + " belongs to " + mainPipes.size() + " tubes");
        }
        return mainPipes.isEmpty() ? null : mainPipes.getFirst();
    }

    /**
     * Plays the sound and particle of an explosion at the pipe, marking an item that reached a block
     * it should never have reached.
     *
     * <p>This used to build an {@code Explosion} and call {@code finalizeExplosion(true)} on it
     * without ever calling {@code explode()}, so the block list stayed empty and the only thing that
     * ever happened was the sound and the particle -- no block or entity damage. 1.21.3 turned
     * Explosion into an interface implemented by the server-side ServerExplosion, which cannot be
     * driven that way, so the two effects are produced directly. The values match what the old
     * constructor defaulted to for a radius-4 DESTROY explosion.</p>
     */
    private void spawnMisroutedItemExplosionEffect() {
        Level level = getWorld();
        BlockPos pos = container.getBlockPos();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (level.isClientSide()) {
            level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F, false);
        }
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
    }
}
