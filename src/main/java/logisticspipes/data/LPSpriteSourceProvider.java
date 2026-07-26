package logisticspipes.data;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import logisticspipes.LPConstants;

public class LPSpriteSourceProvider extends SpriteSourceProvider {

    public LPSpriteSourceProvider(PackOutput output,
        CompletableFuture<HolderLookup.Provider> lookupProvider,
        ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, LPConstants.ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        atlas(SpriteSourceProvider.BLOCKS_ATLAS)
            .addSource(new DirectoryLister("blocks/pipes/new_texture", "blocks/pipes/new_texture/"))
            .addSource(new DirectoryLister("blocks/pipes/status_overlay", "blocks/pipes/status_overlay/"))
            .addSource(new DirectoryLister("blocks/pipes/overlay_gen", "blocks/pipes/overlay_gen/"))
            .addSource(new DirectoryLister("blocks/requesttable", "blocks/requesttable/"))
            .addSource(singleFile("blocks/pipes/liquid_connector"))
            .addSource(singleFile("blocks/pipes/pipemodel"))
            .addSource(singleFile("blocks/pipes/pipemodel-status"))
            .addSource(singleFile("blocks/pipes/pipemodel-status-bc"))
            .addSource(singleFile("blocks/pipes/pipemodel-inactive"))
            .addSource(singleFile("blocks/pipes/innerbox"))
            .addSource(singleFile("blocks/pipes/glass_texture_center"))
            .addSource(singleFile("blocks/blank_pipe"))

            .addSource(singleFile("items/eastereggs/guipsp"))
            .addSource(singleFile("items/liquids/empty"))
            .addSource(singleFile("items/liquids/stencil"))

            .addSource(singleFile("items/parts/0"))
            .addSource(singleFile("items/parts/1"))
            .addSource(singleFile("items/parts/2"))
            .addSource(singleFile("items/parts/3"));

        for (int i = 0; i <= 16; i++) {
            atlas(SpriteSourceProvider.BLOCKS_ATLAS)
                .addSource(singleFile("items/remote_orderer/" + i));
        }

        atlas(SpriteSourceProvider.BLOCKS_ATLAS)
            .addSource(singleFile("items/sign_creator.0"))
            .addSource(singleFile("items/sign_creator.1"))

            .addSource(singleFile("solid_block/crafting_table"))
            .addSource(singleFile("solid_block/crafting_table_fuzzy"))
            .addSource(singleFile("solid_block/frame"))
            .addSource(singleFile("solid_block/power_junction"))
            .addSource(singleFile("solid_block/power_provider_rf"))
            .addSource(singleFile("solid_block/program_compiler"))
            .addSource(singleFile("solid_block/security_station"))
            .addSource(singleFile("solid_block/soldering_station"))
            .addSource(singleFile("solid_block/soldering_station_active"))
            .addSource(singleFile("solid_block/statistics_table"));
    }

    private static SingleFile singleFile(String path) {
        return new SingleFile(LPConstants.rl(path), Optional.empty());
    }
}
