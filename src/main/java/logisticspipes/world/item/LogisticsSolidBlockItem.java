package logisticspipes.world.item;

import net.minecraft.world.item.BlockItem;

import lombok.Getter;

import logisticspipes.blocks.LogisticsSolidBlock;

public class LogisticsSolidBlockItem extends BlockItem {

    @Getter
    private final LogisticsSolidBlock.Type type;

    public LogisticsSolidBlockItem(LogisticsSolidBlock block, Properties properties) {
        super(block, properties.useBlockDescriptionPrefix());
        type = block.getType();
    }
}
