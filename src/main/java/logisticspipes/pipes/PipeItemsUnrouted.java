package logisticspipes.pipes;

import net.minecraft.world.item.Item;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.textures.PipeTexture;
import logisticspipes.transport.PipeTransportLogistics;

public class PipeItemsUnrouted extends CoreUnroutedPipe {

	public PipeItemsUnrouted(Item item) {
		super(new PipeTransportLogistics(false), item);
	}

	@Override
	public int getTextureIndex() {
		return PipeTexture.BASIC_TRANSPORT.ordinal();
	}

}
