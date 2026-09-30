package logisticspipes.world.level.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.world.level.block.LPBlocks;

public class LPBlockEntityTypes {

    private static final DeferredRegister<BlockEntityType<?>> deferredRegister =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LPConstants.ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsPowerJunctionBlockEntity>> POWER_JUNCTION =
        deferredRegister.register("power_junction",
            () -> new BlockEntityType<>(LogisticsPowerJunctionBlockEntity::new, LPBlocks.POWER_JUNCTION.get()));

    // NOTE: BlockEntity constructors must be migrated to (BlockPos, BlockState) before
    // these suppliers will compile. Stubs use placeholder suppliers for now.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsFEPowerProviderBlockEntity>> POWER_PROVIDER_FE =
        deferredRegister.register("power_provider_fe",
            () -> new BlockEntityType<>(LogisticsFEPowerProviderBlockEntity::new, LPBlocks.POWER_PROVIDER_FE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsSecurityBlockEntity>> SECURITY_STATION =
        deferredRegister.register("security_station",
            () -> new BlockEntityType<>(LogisticsSecurityBlockEntity::new, LPBlocks.SECURITY_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsCraftingTableBlockEntity>> CRAFTING_TABLE =
        deferredRegister.register("logistics_crafting_table",
            () -> new BlockEntityType<>(LogisticsCraftingTableBlockEntity::new,
                LPBlocks.CRAFTER.get(), LPBlocks.CRAFTER_FUZZY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsTileGenericPipe>> PIPE =
        deferredRegister.register("pipe",
            () -> new BlockEntityType<>(LogisticsTileGenericPipe::new, LPBlocks.PIPE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsTileGenericSubMultiBlock>> SUB_PIPE =
        deferredRegister.register("sub_pipe",
            () -> new BlockEntityType<>(LogisticsTileGenericSubMultiBlock::new, LPBlocks.SUB_MULTIBLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsStatisticsBlockEntity>> STATISTICS_TABLE =
        deferredRegister.register("statistics_table",
            () -> new BlockEntityType<>(LogisticsStatisticsBlockEntity::new, LPBlocks.STATISTICS_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsProgramCompilerBlockEntity>> PROGRAM_COMPILER =
        deferredRegister.register("program_compiler",
            () -> new BlockEntityType<>(LogisticsProgramCompilerBlockEntity::new, LPBlocks.PROGRAM_COMPILER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsFrameBlockEntity>> FRAME =
        deferredRegister.register("frame",
            () -> new BlockEntityType<>(LogisticsFrameBlockEntity::new, LPBlocks.FRAME.get()));

    public static void register(IEventBus modEventBus) {
        // Renamed from RF when the Thermal energy API gave way to NeoForge's FE; keeps old worlds loading.
        deferredRegister.addAlias(LPConstants.rl("power_provider_rf"), LPConstants.rl("power_provider_fe"));
        deferredRegister.register(modEventBus);
    }
}
