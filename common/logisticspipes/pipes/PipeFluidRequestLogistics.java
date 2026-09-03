package logisticspipes.pipes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IPipeMenuProvider;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.world.inventory.FluidOrdererMenu;

public class PipeFluidRequestLogistics extends FluidRoutedPipe implements IRequestFluid, IPipeMenuProvider {

	public PipeFluidRequestLogistics(Item item) {
		super(item);
	}

	public void openGui(Player entityplayer) {
		if (entityplayer instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(this);
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FluidOrdererMenu(containerId, inventory, this);
	}

	@Override
	public boolean handleClick(Player entityplayer, @Nullable SecuritySettings settings) {
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
