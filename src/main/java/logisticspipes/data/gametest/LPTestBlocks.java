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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

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

    /**
     * An energy store that takes FE and gives none back, for what the FE power supplier upgrade
     * hands out: vanilla has no block that holds energy.
     */
    public static final DeferredBlock<BatteryBlock> BATTERY =
        BLOCKS.registerBlock("test_battery", properties -> new BatteryBlock(properties, false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_ENTITY =
        BLOCK_ENTITY_TYPES.register("test_battery",
            () -> new BlockEntityType<>((pos, state) -> new BatteryBlockEntity(pos, state, false), BATTERY.get()));
    /**
     * A battery with an item slot too, like a machine that processes items: the FE power
     * supplier upgrade only powers what its pipe connects to, and a pipe connects to inventories.
     */
    public static final DeferredBlock<BatteryBlock> MACHINE =
        BLOCKS.registerBlock("test_machine", properties -> new BatteryBlock(properties, true));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> MACHINE_ENTITY =
        BLOCK_ENTITY_TYPES.register("test_machine",
            () -> new BlockEntityType<>((pos, state) -> new BatteryBlockEntity(pos, state, true), MACHINE.get()));

    private LPTestBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, TANK_ENTITY.get(), (tank, side) -> tank.tank);
            event.registerBlockEntity(Capabilities.Energy.BLOCK, BATTERY_ENTITY.get(),
                (battery, side) -> battery.energy);
            event.registerBlockEntity(Capabilities.Energy.BLOCK, MACHINE_ENTITY.get(),
                (machine, side) -> machine.energy);
            event.registerBlockEntity(Capabilities.Item.BLOCK, MACHINE_ENTITY.get(),
                (machine, side) -> machine.items);
        });
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

    public static class BatteryBlock extends Block implements EntityBlock {

        private final boolean machine;

        public BatteryBlock(Properties properties, boolean machine) {
            super(properties);
            this.machine = machine;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BatteryBlockEntity(pos, state, machine);
        }
    }

    public static class BatteryBlockEntity extends BlockEntity {

        public static final int CAPACITY = 1_000_000;

        public final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, CAPACITY, 0);
        public final ItemStacksResourceHandler items = new ItemStacksResourceHandler(1);

        public BatteryBlockEntity(BlockPos pos, BlockState state, boolean machine) {
            super(machine ? MACHINE_ENTITY.get() : BATTERY_ENTITY.get(), pos, state);
        }
    }
}
