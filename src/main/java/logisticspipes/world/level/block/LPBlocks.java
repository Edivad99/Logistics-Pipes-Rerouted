package logisticspipes.world.level.block;

import java.util.Collection;
import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LPBlocks {

  private static final DeferredRegister.Blocks deferredRegister =
      DeferredRegister.createBlocks(LPConstants.ID);

  public static void register(IEventBus modEventBus) {
    deferredRegister.register(modEventBus);
  }

  public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
    return deferredRegister.getEntries();
  }

  public static final DeferredBlock<LogisticsSolidBlock> FRAME =
      deferredRegister.registerBlock("frame",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME, properties));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_JUNCTION =
      deferredRegister.registerBlock("power_junction",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_POWER_JUNCTION, properties));

  public static final DeferredBlock<LogisticsSolidBlock> SECURITY_STATION =
      deferredRegister.registerBlock("security_station",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_SECURITY_STATION, properties));

  public static final DeferredBlock<LogisticsSolidBlock> CRAFTER =
      deferredRegister.registerBlock("crafting_table",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_AUTOCRAFTING_TABLE, properties));

  public static final DeferredBlock<LogisticsSolidBlock> CRAFTER_FUZZY =
      deferredRegister.registerBlock("crafting_table_fuzzy",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_FUZZYCRAFTING_TABLE, properties));

  public static final DeferredBlock<LogisticsSolidBlock> STATISTICS_TABLE =
      deferredRegister.registerBlock("statistics_table",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_STATISTICS_TABLE, properties));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_PROVIDER_RF =
      deferredRegister.registerBlock("power_provider_rf",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_RF_POWERPROVIDER, properties));

  public static final DeferredBlock<LogisticsSolidBlock> PROGRAM_COMPILER =
      deferredRegister.registerBlock("program_compiler",
          properties -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_PROGRAM_COMPILER, properties));

  public static final DeferredBlock<LogisticsBlockGenericPipe> PIPE =
      deferredRegister.registerBlock("pipe", LogisticsBlockGenericPipe::new);

  public static final DeferredBlock<LogisticsBlockGenericSubMultiBlock> SUB_MULTIBLOCK =
      deferredRegister.registerBlock("sub_multiblock", LogisticsBlockGenericSubMultiBlock::new);
}
