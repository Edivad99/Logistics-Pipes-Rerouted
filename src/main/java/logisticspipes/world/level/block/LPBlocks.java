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
      deferredRegister.register("frame",
          () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_JUNCTION =
      deferredRegister.register("power_junction",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_POWER_JUNCTION));

  public static final DeferredBlock<LogisticsSolidBlock> SECURITY_STATION =
      deferredRegister.register("security_station",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_SECURITY_STATION));

  public static final DeferredBlock<LogisticsSolidBlock> CRAFTER =
      deferredRegister.register("crafting_table",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_AUTOCRAFTING_TABLE));

  public static final DeferredBlock<LogisticsSolidBlock> CRAFTER_FUZZY =
      deferredRegister.register("crafting_table_fuzzy",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_FUZZYCRAFTING_TABLE));

  public static final DeferredBlock<LogisticsSolidBlock> STATISTICS_TABLE =
      deferredRegister.register("statistics_table",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_STATISTICS_TABLE));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_PROVIDER_RF =
      deferredRegister.register("power_provider_rf",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_RF_POWERPROVIDER));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_PROVIDER_EU =
      deferredRegister.register("power_provider_eu",
          () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_IC2_POWERPROVIDER));

  public static final DeferredBlock<LogisticsSolidBlock> POWER_PROVIDER_MJ =
      deferredRegister.register("power_provider_mj",
      () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_BC_POWERPROVIDER));

  public static final DeferredBlock<LogisticsSolidBlock> PROGRAM_COMPILER =
      deferredRegister.register("program_compiler",
          () -> new LogisticsSolidBlock(LogisticsSolidBlock.Type.LOGISTICS_PROGRAM_COMPILER));

  public static final DeferredBlock<LogisticsBlockGenericPipe> PIPE =
      deferredRegister.register("pipe", LogisticsBlockGenericPipe::new);

  public static final DeferredBlock<LogisticsBlockGenericSubMultiBlock> SUB_MULTIBLOCK =
      deferredRegister.register("sub_multiblock", LogisticsBlockGenericSubMultiBlock::new);
}
