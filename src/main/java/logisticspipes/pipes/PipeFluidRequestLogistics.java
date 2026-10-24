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
import logisticspipes.security.SecurityPermission;
import logisticspipes.security.SecurityPermissions;
import logisticspipes.textures.PipeTexture;
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
    public boolean handleClick(Player entityplayer, @Nullable SecurityPermissions permissions) {
        if (!getLevel().isClientSide()) {
            if (permissions == null || permissions.allows(SecurityPermission.OPEN_REQUEST)) {
                openGui(entityplayer);
            } else {
                entityplayer.sendSystemMessage(Component.literal("Permission denied"));
            }
        }
        return true;
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.LIQUID_REQUEST;
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
