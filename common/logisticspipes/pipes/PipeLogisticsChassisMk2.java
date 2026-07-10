package logisticspipes.pipes;

import logisticspipes.config.Configs;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import net.minecraft.world.item.Item;

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
		return Configs.CHASSIS_SLOTS_ARRAY[1];
	}

}
