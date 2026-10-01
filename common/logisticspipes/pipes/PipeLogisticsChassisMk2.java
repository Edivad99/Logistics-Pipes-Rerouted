package logisticspipes.pipes;

import net.minecraft.world.item.Item;

import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;

public class PipeLogisticsChassisMk2 extends PipeLogisticsChassis {

	public PipeLogisticsChassisMk2(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_CHASSI2_TEXTURE;
	}

	@Override
	public int getChassisSize() {
		return 2;
	}

}
