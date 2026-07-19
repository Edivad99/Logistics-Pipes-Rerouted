package logisticspipes.world.level.block.entity;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsFrameTileEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsIC2PowerProviderTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.world.level.block.LPBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LPBlockEntityTypes {

  private static final DeferredRegister<BlockEntityType<?>> deferredRegister =
      DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LPConstants.ID);

  public static void register(IEventBus modEventBus) {
    deferredRegister.register(modEventBus);
  }

  // NOTE: BlockEntity constructors must be migrated to (BlockPos, BlockState) before
  // these suppliers will compile. Stubs use placeholder suppliers for now.

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsPowerJunctionTileEntity>> POWER_JUNCTION =
      deferredRegister.register("power_junction",
          () -> BlockEntityType.Builder.of(LogisticsPowerJunctionTileEntity::new,
              LPBlocks.POWER_JUNCTION.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsRFPowerProviderTileEntity>> POWER_PROVIDER_RF =
      deferredRegister.register("power_provider_rf",
          () -> BlockEntityType.Builder.of(LogisticsRFPowerProviderTileEntity::new,
              LPBlocks.POWER_PROVIDER_RF.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsIC2PowerProviderTileEntity>> POWER_PROVIDER_EU =
      deferredRegister.register("power_provider_ic2",
          () -> BlockEntityType.Builder.of(LogisticsIC2PowerProviderTileEntity::new,
              LPBlocks.POWER_PROVIDER_EU.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsSecurityTileEntity>> SECURITY_STATION =
      deferredRegister.register("security_station",
          () -> BlockEntityType.Builder.of(LogisticsSecurityTileEntity::new,
              LPBlocks.SECURITY_STATION.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsCraftingTableTileEntity>> CRAFTING_TABLE =
      deferredRegister.register("logistics_crafting_table",
          () -> BlockEntityType.Builder.of(LogisticsCraftingTableTileEntity::new,
              LPBlocks.CRAFTER.get(), LPBlocks.CRAFTER_FUZZY.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsTileGenericPipe>> PIPE =
      deferredRegister.register("pipe",
          () -> BlockEntityType.Builder.of(LogisticsTileGenericPipe::new,
              LPBlocks.PIPE.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsTileGenericSubMultiBlock>> SUB_PIPE =
      deferredRegister.register("sub_pipe",
          () -> BlockEntityType.Builder.of(LogisticsTileGenericSubMultiBlock::new,
              LPBlocks.SUB_MULTIBLOCK.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsStatisticsTileEntity>> STATISTICS_TABLE =
      deferredRegister.register("statistics_table",
          () -> BlockEntityType.Builder.of(LogisticsStatisticsTileEntity::new,
              LPBlocks.STATISTICS_TABLE.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsProgramCompilerBlockEntity>> PROGRAM_COMPILER =
      deferredRegister.register("program_compiler",
          () -> BlockEntityType.Builder.of(LogisticsProgramCompilerBlockEntity::new,
              LPBlocks.PROGRAM_COMPILER.get()).build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsFrameTileEntity>> FRAME =
      deferredRegister.register("frame",
          () -> BlockEntityType.Builder.of(LogisticsFrameTileEntity::new,
              LPBlocks.FRAME.get()).build(null));
}
