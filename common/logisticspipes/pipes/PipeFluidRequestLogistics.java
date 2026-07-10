package logisticspipes.pipes;

import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class PipeFluidRequestLogistics extends FluidRoutedPipe implements IRequestFluid {

	public PipeFluidRequestLogistics(Item item) {
		super(item);
	}

	public void openGui(Player entityplayer) {
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FluidOrdererGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}

	@Override
	public boolean handleClick(Player entityplayer, SecuritySettings settings) {
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openRequest) {
				openGui(entityplayer);
			} else {
				entityplayer.sendSystemMessage(Component.literal("Permission denied"));
			}
		}
		return true;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_REQUEST;
	}

	@Override
	public void sendFailed(FluidIdentifier value1, Integer value2) {
		//Request Pipe doesn't handle this.
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
