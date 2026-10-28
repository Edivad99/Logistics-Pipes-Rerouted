package logisticspipes.data.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;
import logisticspipes.utils.transfer.LPFluidTank;

/**
 * Blocks that only exist on the game test server, for what vanilla has no block for. They are
 * registered only when {@code logisticspipes.test} is set, so they never reach a player's world.
 */
public final class LPTestBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LPConstants.ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LPConstants.ID);

    /**
     * A fluid tank, which the fluid pipes need: vanilla's cauldron has no block entity to connect
     * to.
     */
    public static final DeferredBlock<TankBlock> TANK = BLOCKS.registerBlock("test_tank", TankBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_ENTITY =
        BLOCK_ENTITY_TYPES.register("test_tank", () -> new BlockEntityType<>(TankBlockEntity::new, TANK.get()));

    private LPTestBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(
            Capabilities.Fluid.BLOCK, TANK_ENTITY.get(), (tank, side) -> tank.tank));
    }

    public static class TankBlock extends Block implements EntityBlock {

        public TankBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TankBlockEntity(pos, state);
        }
    }

    public static class TankBlockEntity extends BlockEntity {

        public static final int CAPACITY = 16_000;

        public final LPFluidTank tank = new LPFluidTank(CAPACITY);

        public TankBlockEntity(BlockPos pos, BlockState state) {
            super(TANK_ENTITY.get(), pos, state);
        }
    }
}
