package logisticspipes.pipes.unrouted;

import javax.annotation.Nullable;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.transport.PipeTransportLogistics;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class PipeItemsBasicTransport extends CoreUnroutedPipe {

	public PipeItemsBasicTransport(Item item) {
		super(new PipeTransportLogistics(false), item);
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public int getIconIndex(@Nullable Direction direction) {
		return Textures.LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE.normal;
	}

	@Override
	public int getTextureIndex() {
		return Textures.LOGISTICSPIPE_BASIC_TRANSPORT_TEXTURE.newTexture;
	}

}
