package logisticspipes.data.models;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import logisticspipes.LPConstants;
import logisticspipes.world.item.LPItems;

public class LPItemModelProvider extends ItemModelProvider {

    public LPItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LPConstants.ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.basicItem(LPItems.ITEM_CARD.get());
        this.basicItem(LPItems.HUD_GLASSES.get());
        this.basicItem(LPItems.DISK.get());
        this.basicItem(LPItems.PIPE_CONTROLLER.get());
        this.basicItem(LPItems.PIPE_MANAGER.get());
        this.basicItem(LPItems.LOGISTICS_PROGRAMMER.get());
        this.basicItem(LPItems.BROKEN_ITEM.get());
        this.basicItem(LPItems.GUIDE_BOOK.get());

        this.signCreator();

        //        this.basicItem(LPItems.REMOTE_ORDERER.get());
        //        this.basicItem(LPItems.PARTS.get());
        //        this.basicItem(LPItems.FLUID_CONTAINER.get());

        this.basicItem(LPItems.CHIP_ADVANCED.get());
        this.basicItem(LPItems.CHIP_ADVANCED_RAW.get());
        this.basicItem(LPItems.CHIP_BASIC.get());
        this.basicItem(LPItems.CHIP_BASIC_RAW.get());
        this.basicItem(LPItems.CHIP_FPGA.get());
        this.basicItem(LPItems.CHIP_FPGA_RAW.get());

        this.basicItem(LPItems.MODULE_BLANK.get());
        LPItems.modules.forEach((name, rl) -> this.basicItem(rl));
        LPItems.upgrades.forEach((name, rl) -> this.basicItem(rl));
    }

    private void signCreator() {
        final String[] SIGN_CREATOR_MODES = { "crafting", "item_amount" };
        String name = LPItems.SIGN_CREATOR.getId().getPath();

        ItemModelBuilder base =
            withExistingParent(name, mcLoc("item/generated"))
            .texture("layer0", LPConstants.rl("item/" + name + "_" + SIGN_CREATOR_MODES[0]));

        for (int mode = 0; mode < SIGN_CREATOR_MODES.length; mode++) {
            ItemModelBuilder variant =
                withExistingParent(name + "_" + SIGN_CREATOR_MODES[mode], mcLoc("item/generated"))
                .texture("layer0", LPConstants.rl("item/" + name + "_" + SIGN_CREATOR_MODES[mode]));

            if (mode > 0) {
                base.override().predicate(LPConstants.rl("creator_mode"), mode).model(variant).end();
            }
        }
    }
}
