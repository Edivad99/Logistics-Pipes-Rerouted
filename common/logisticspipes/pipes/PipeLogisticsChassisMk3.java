package logisticspipes.pipes;

import logisticspipes.LPConfigs;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import net.minecraft.world.item.Item;

public class PipeLogisticsChassisMk3 extends PipeLogisticsChassis {

	public PipeLogisticsChassisMk3(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_CHASSI3_TEXTURE;
	}

	@Override
	public int getChassisSize() {
		return LPConfigs.CHASSIS_SLOTS_ARRAY[2];
	}

}
