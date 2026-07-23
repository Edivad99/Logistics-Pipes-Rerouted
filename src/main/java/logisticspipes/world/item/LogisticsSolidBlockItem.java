package logisticspipes.world.item;

import net.minecraft.world.item.BlockItem;

import lombok.Getter;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.interfaces.ILogisticsItem;

public class LogisticsSolidBlockItem extends BlockItem implements ILogisticsItem {

    @Getter
    private final LogisticsSolidBlock.Type type;

    public LogisticsSolidBlockItem(LogisticsSolidBlock block, Properties properties) {
        super(block, properties);
        type = block.getType();
    }
}
