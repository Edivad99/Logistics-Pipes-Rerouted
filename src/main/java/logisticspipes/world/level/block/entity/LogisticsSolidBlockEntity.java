package logisticspipes.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.network.to_server.RequestBlockRotationMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.DoubleCoordinates;

public class LogisticsSolidBlockEntity extends BlockEntity implements ITickable, IRotationProvider {

    public int rotation = 0;
    private boolean init = false;

    public LogisticsSolidBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Returns the level this block entity is in. Replaces removed getWorld() from 1.12.2.
     */
    @Nullable
    protected Level getWorld() {
        return this.level;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rotation = input.getIntOr("rotation", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("rotation", rotation);
    }

    @Override
    public void update() {
        if (MainProxy.isClient(getWorld())) {
            if (!init) {
                ClientPacketDistributor.sendToServer(new RequestBlockRotationMessage(getBlockPos()));
                init = true;
            }
        }
    }

    // shouldRefresh() removed in 1.20.1 — block entities are replaced on block change by default

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public void onBlockBreak() {
    }

    /**
     * 1.21.5 split block removal in two: {@code BlockBehaviour#onRemove} became
     * {@code affectNeighborsAfterRemoval}, which runs only to notify neighbours and — crucially —
     * after the block entity has already been detached, so it can no longer reach it. Everything
     * that has to touch the block entity moved here, which runs while it is still attached.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        onBlockBreak();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public int getRotation() {
        return rotation;
    }

    @Override
    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isActive() {
        return false;
    }

    public void notifyOfBlockChange() {
    }

    public DoubleCoordinates getLPPosition() {
        return new DoubleCoordinates(this);
    }

    public Level getLevelForHUD() {
        return getWorld();
    }
}
