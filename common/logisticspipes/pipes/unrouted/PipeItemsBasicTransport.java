package logisticspipes.pipes.unrouted;

import net.minecraft.world.item.Item;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.transport.PipeTransportLogistics;

public class PipeItemsBasicTransport extends CoreUnroutedPipe {

	public PipeItemsBasicTransport(Item item) {
		super(new PipeTransportLogistics(false), item);
	}

	@Override
	public int getTextureIndex() {
		return Textures.LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE.index();
	}

}
