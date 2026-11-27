package logisticspipes.data;

import java.util.Set;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import net.neoforged.neoforge.registries.DeferredHolder;

import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsSolidBlock;

public class LPBlockLootSubProvider extends BlockLootSubProvider {

    public LPBlockLootSubProvider(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    public static LootTableProvider.SubProviderEntry entry() {
        return new LootTableProvider.SubProviderEntry(LPBlockLootSubProvider::new, LootContextParamSets.BLOCK);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return LPBlocks.entries().stream()
            .<Block>map(DeferredHolder::get)
            .filter(block -> block instanceof LogisticsSolidBlock)
            .toList();
    }
}
