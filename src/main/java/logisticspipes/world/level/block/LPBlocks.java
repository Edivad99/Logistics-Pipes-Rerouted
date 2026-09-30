package logisticspipes.world.level.block;

import java.util.Collection;

import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;

public class LPBlocks {

    private static final DeferredRegister.Blocks deferredRegister =
        DeferredRegister.createBlocks(LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        // Renamed from RF when the Thermal energy API gave way to NeoForge's FE; keeps old worlds loading.
        deferredRegister.addAlias(LPConstants.rl("power_provider_rf"), LPConstants.rl("power_provider_fe"));
        deferredRegister.register(modEventBus);
    }

    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return deferredRegister.getEntries();
    }

    public static final DeferredBlock<LogisticsFrameBlock> FRAME =
        deferredRegister.registerBlock("frame", LogisticsFrameBlock::new);

    public static final DeferredBlock<LogisticsPowerJunctionBlock> POWER_JUNCTION =
        deferredRegister.registerBlock("power_junction", LogisticsPowerJunctionBlock::new);

    public static final DeferredBlock<LogisticsSecurityStationBlock> SECURITY_STATION =
        deferredRegister.registerBlock("security_station", LogisticsSecurityStationBlock::new);

    public static final DeferredBlock<LogisticsCraftingTableBlock> CRAFTER =
        deferredRegister.registerBlock("crafting_table",
            properties -> new LogisticsCraftingTableBlock(false, properties));

    public static final DeferredBlock<LogisticsCraftingTableBlock> CRAFTER_FUZZY =
        deferredRegister.registerBlock("crafting_table_fuzzy",
            properties -> new LogisticsCraftingTableBlock(true, properties));

    public static final DeferredBlock<LogisticsStatisticsTableBlock> STATISTICS_TABLE =
        deferredRegister.registerBlock("statistics_table", LogisticsStatisticsTableBlock::new);

    public static final DeferredBlock<LogisticsFEPowerProviderBlock> POWER_PROVIDER_FE =
        deferredRegister.registerBlock("power_provider_fe", LogisticsFEPowerProviderBlock::new);

    public static final DeferredBlock<LogisticsProgramCompilerBlock> PROGRAM_COMPILER =
        deferredRegister.registerBlock("program_compiler", LogisticsProgramCompilerBlock::new);

    public static final DeferredBlock<LogisticsBlockGenericPipe> PIPE =
        deferredRegister.registerBlock("pipe", LogisticsBlockGenericPipe::new);

    public static final DeferredBlock<LogisticsBlockGenericSubMultiBlock> SUB_MULTIBLOCK =
        deferredRegister.registerBlock("sub_multiblock", LogisticsBlockGenericSubMultiBlock::new);
}
