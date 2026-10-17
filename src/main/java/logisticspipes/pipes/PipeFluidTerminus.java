package logisticspipes.pipes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.interfaces.IPipeMenuProvider;
import logisticspipes.network.to_client.pipe.PipePropertiesMessage;
import logisticspipes.pipes.basic.fluid.FluidSinkPipe;
import logisticspipes.textures.Textures;
import logisticspipes.utils.FluidSinkReply.FixedFluidPriority;
import logisticspipes.world.inventory.FluidTerminusMenu;

public class PipeFluidTerminus extends FluidSinkPipe implements IPipeMenuProvider {

    public PipeFluidTerminus(Item item) {
        super(item, "Fluids to terminate", 9);
    }

    @Override
    public FixedFluidPriority getPriority() {
        return FixedFluidPriority.TERMINUS;
    }

    @Override
    public Textures.TextureType getCenterTexture() {
        return Textures.LOGISTICSPIPE_LIQUID_TERMINUS;
    }

    @Override
    public void onWrenchClicked(Player entityplayer) {
        if (entityplayer instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                PipePropertiesMessage.of(getPos(), this, entityplayer.registryAccess()));
        }
        if (entityplayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidTerminusMenu(containerId, inventory, this);
    }
}
