package logisticspipes.world.item;

import net.minecraft.world.item.BlockItem;

import logisticspipes.world.level.block.LogisticsSolidBlock;

public class LogisticsSolidBlockItem extends BlockItem {

    public LogisticsSolidBlockItem(LogisticsSolidBlock block, Properties properties) {
        super(block, properties.useBlockDescriptionPrefix());
    }

    /** The block this item places, for the renderer that draws its 3D model in the inventory. */
    public LogisticsSolidBlock getSolidBlock() {
        return (LogisticsSolidBlock) getBlock();
    }
}
