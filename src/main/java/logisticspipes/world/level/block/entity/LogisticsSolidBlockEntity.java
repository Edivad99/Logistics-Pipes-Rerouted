package logisticspipes.world.level.block.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.RequestRotationPacket;
import logisticspipes.proxy.MainProxy;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsSolidBlockEntity extends BlockEntity implements ITickable, IRotationProvider {

    private boolean init = false;
    public int rotation = 0;

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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rotation = tag.getInt("rotation");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("rotation", rotation);
    }

    // onChunkUnload() removed in 1.20.1 — handled by level unload events if needed

    @Override
    public void update() {
        if (MainProxy.isClient(getWorld())) {
            if (!init) {
                MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRotationPacket.class)
                    .setBlockPos(getBlockPos()));
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

    @Override
    public int getRotation() {
        return rotation;
    }

    public boolean isActive() {
        return false;
    }

    @Override
    public void setRotation(int rotation) {
        this.rotation = rotation;
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
