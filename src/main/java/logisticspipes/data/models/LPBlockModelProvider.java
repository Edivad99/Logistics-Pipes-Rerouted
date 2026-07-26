package logisticspipes.data.models;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import logisticspipes.LPConstants;

public class LPBlockModelProvider extends BlockStateProvider {

    public LPBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LPConstants.ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        //this.cubeAll(LPBlocks.CRAFTER.get());
    }
}
