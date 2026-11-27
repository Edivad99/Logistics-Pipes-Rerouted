package logisticspipes.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import net.neoforged.neoforge.registries.DeferredHolder;

import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsSolidBlock;

public class LPBlockLootSubProvider extends BlockLootSubProvider {

    public LPBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(LPBlockLootSubProvider::new, LootContextParamSets.BLOCK)),
            registries);
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
