package logisticspipes.pipes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;

import logisticspipes.interfaces.IPipeMenuProvider;
import logisticspipes.pipes.basic.fluid.FluidSinkPipe;
import logisticspipes.routing.sink.FluidSinkReply.FixedFluidPriority;
import logisticspipes.textures.PipeTexture;
import logisticspipes.world.inventory.FluidSinkMenu;

public class PipeFluidBasic extends FluidSinkPipe implements IPipeMenuProvider {

    public PipeFluidBasic(Item item) {
        super(item, "Fluids to sink", 1);
    }

    @Override
    public FixedFluidPriority getPriority() {
        return FixedFluidPriority.FLUID_SINK;
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.LIQUID_BASIC;
    }

    @Override
    public void onWrenchClicked(Player entityplayer) {
        if (entityplayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidSinkMenu(containerId, inventory, this);
    }
}
