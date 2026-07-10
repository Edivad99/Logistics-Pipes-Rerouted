package logisticspipes;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Holds RegistryObject references to all registered LP blocks.
 * Access via .get() — e.g. LPBlocks.pipe.get()
 *
 * NOTE: Call sites previously used plain fields (e.g. LPBlocks.pipe) — all updated
 * to LPBlocks.pipe.get() as part of task #10.
 */
public class LPBlocks {

	public static final DeferredBlock<LogisticsSolidBlock> frame           = LPRegistries.FRAME;
	public static final DeferredBlock<LogisticsSolidBlock>           powerJunction   = LPRegistries.POWER_JUNCTION;
	public static final DeferredBlock<LogisticsSolidBlock>           securityStation = LPRegistries.SECURITY_STATION;
	public static final DeferredBlock<LogisticsSolidBlock>           crafter         = LPRegistries.CRAFTER;
	public static final DeferredBlock<LogisticsSolidBlock>           crafterFuzzy    = LPRegistries.CRAFTER_FUZZY;
	public static final DeferredBlock<LogisticsSolidBlock>           statisticsTable = LPRegistries.STATISTICS_TABLE;
	public static final DeferredBlock<LogisticsSolidBlock>           powerProviderRF = LPRegistries.POWER_PROVIDER_RF;
	public static final DeferredBlock<LogisticsSolidBlock>           powerProviderEU = LPRegistries.POWER_PROVIDER_EU;
	public static final DeferredBlock<LogisticsSolidBlock>           powerProviderMJ = LPRegistries.POWER_PROVIDER_MJ;
	public static final DeferredBlock<LogisticsSolidBlock>           programCompiler = LPRegistries.PROGRAM_COMPILER;
	public static final DeferredBlock<LogisticsBlockGenericPipe>          pipe           = LPRegistries.PIPE;
	public static final DeferredBlock<LogisticsBlockGenericSubMultiBlock> subMultiblock  = LPRegistries.SUB_MULTIBLOCK;

}
