package logisticspipes.data.models;

import net.minecraft.data.PackOutput;

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

//        this.basicItem(LPItems.REMOTE_ORDERER.get());
//        this.basicItem(LPItems.SIGN_CREATOR.get());
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
}
