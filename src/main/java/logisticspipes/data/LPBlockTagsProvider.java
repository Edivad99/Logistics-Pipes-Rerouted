package logisticspipes.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;

import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

import logisticspipes.LPConstants;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsSolidBlock;

public class LPBlockTagsProvider extends BlockTagsProvider {

    public LPBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, LPConstants.ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        LPBlocks.entries().stream()
            .filter(holder -> holder.get() instanceof LogisticsSolidBlock)
            .map(DeferredHolder::key)
            .forEach(key -> {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(key);
                tag(BlockTags.NEEDS_IRON_TOOL).add(key);
            });
    }
}
