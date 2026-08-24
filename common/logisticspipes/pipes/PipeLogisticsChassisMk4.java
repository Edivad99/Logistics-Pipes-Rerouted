package logisticspipes.pipes;

import net.minecraft.world.item.Item;

import logisticspipes.LPConfigs;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;

public class PipeLogisticsChassisMk4 extends PipeLogisticsChassis {

	public PipeLogisticsChassisMk4(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_CHASSI4_TEXTURE;
	}

	@Override
	public int getChassisSize() {
		return LPConfigs.CHASSIS_SLOTS_ARRAY[3];
	}

}
