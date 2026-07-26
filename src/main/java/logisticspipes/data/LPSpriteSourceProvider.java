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

            .addSource(singleFile("items/broken_item"))
            .addSource(singleFile("items/chip/advanced"))
            .addSource(singleFile("items/chip/advanced_raw"))
            .addSource(singleFile("items/chip/basic"))
            .addSource(singleFile("items/chip/basic_raw"))
            .addSource(singleFile("items/chip/fpga"))
            .addSource(singleFile("items/chip/fpga_raw"))
            .addSource(singleFile("items/disk"))
            .addSource(singleFile("items/eastereggs/guipsp"))
            .addSource(singleFile("items/guide_book"))
            .addSource(singleFile("items/hud_glasses"))
            .addSource(singleFile("items/item_card"))
            .addSource(singleFile("items/liquids/empty"))
            .addSource(singleFile("items/liquids/stencil"))
            .addSource(singleFile("items/logistics_programmer"))

            .addSource(singleFile("items/module/active_supplier"))
            .addSource(singleFile("items/module/blank"))
            .addSource(singleFile("items/module/crafter"))
            .addSource(singleFile("items/module/crafter_mk2"))
            .addSource(singleFile("items/module/crafter_mk3"))
            .addSource(singleFile("items/module/enchantment_sink"))
            .addSource(singleFile("items/module/enchantment_sink_mk2"))
            .addSource(singleFile("items/module/extractor"))
            .addSource(singleFile("items/module/extractor_advanced"))
            .addSource(singleFile("items/module/extractor_advanced_mk2"))
            .addSource(singleFile("items/module/extractor_advanced_mk3"))
            .addSource(singleFile("items/module/extractor_mk2"))
            .addSource(singleFile("items/module/extractor_mk3"))
            .addSource(singleFile("items/module/item_sink"))
            .addSource(singleFile("items/module/item_sink_cc"))
            .addSource(singleFile("items/module/item_sink_creativetab"))
            .addSource(singleFile("items/module/item_sink_mod"))
            .addSource(singleFile("items/module/item_sink_oredict"))
            .addSource(singleFile("items/module/item_sink_polymorphic"))
            .addSource(singleFile("items/module/passive_supplier"))
            .addSource(singleFile("items/module/provider"))
            .addSource(singleFile("items/module/provider_mk2"))
            .addSource(singleFile("items/module/quick_sort"))
            .addSource(singleFile("items/module/quick_sort_cc"))
            .addSource(singleFile("items/module/terminus"))
            .addSource(singleFile("items/module/thaumic_aspect_sink"))

            .addSource(singleFile("items/parts/0"))
            .addSource(singleFile("items/parts/1"))
            .addSource(singleFile("items/parts/2"))
            .addSource(singleFile("items/parts/3"))

            .addSource(singleFile("items/pipe_controller"))
            .addSource(singleFile("items/pipe_manager"));

        for (int i = 0; i <= 16; i++) {
            atlas(SpriteSourceProvider.BLOCKS_ATLAS)
                .addSource(singleFile("items/remote_orderer/" + i));
        }

        atlas(SpriteSourceProvider.BLOCKS_ATLAS)
            .addSource(singleFile("items/sign_creator.0"))
            .addSource(singleFile("items/sign_creator.1"))

            .addSource(singleFile("items/upgrade/action_speed"))
            .addSource(singleFile("items/upgrade/cc_remote_control"))
            .addSource(singleFile("items/upgrade/crafting_byproduct"))
            .addSource(singleFile("items/upgrade/crafting_cleanup"))
            .addSource(singleFile("items/upgrade/crafting_monitoring"))
            .addSource(singleFile("items/upgrade/disconnection"))
            .addSource(singleFile("items/upgrade/fluid_crafting"))
            .addSource(singleFile("items/upgrade/fuzzy"))
            .addSource(singleFile("items/upgrade/item_extraction"))
            .addSource(singleFile("items/upgrade/item_stack_extraction"))
            .addSource(singleFile("items/upgrade/module_upgrade"))
            .addSource(singleFile("items/upgrade/opaque"))
            .addSource(singleFile("items/upgrade/pattern"))
            .addSource(singleFile("items/upgrade/power_supplier_eu"))
            .addSource(singleFile("items/upgrade/power_supplier_eu_ev"))
            .addSource(singleFile("items/upgrade/power_supplier_eu_hv"))
            .addSource(singleFile("items/upgrade/power_supplier_eu_lv"))
            .addSource(singleFile("items/upgrade/power_supplier_eu_mv"))
            .addSource(singleFile("items/upgrade/power_supplier_mj"))
            .addSource(singleFile("items/upgrade/power_supplier_rf"))
            .addSource(singleFile("items/upgrade/power_transportation"))
            .addSource(singleFile("items/upgrade/satellite_advanced"))
            .addSource(singleFile("items/upgrade/sneaky"))
            .addSource(singleFile("items/upgrade/sneaky_combination"))
            .addSource(singleFile("items/upgrade/speed"))

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
