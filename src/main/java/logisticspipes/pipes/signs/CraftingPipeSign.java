package logisticspipes.pipes.signs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.crafting.CraftingDummyInventoryMessage;
import logisticspipes.pipes.PipeItemsCraftingLogistics;

/**
 * Shows what a crafting pipe makes, and through which satellite.
 */
public final class CraftingPipeSign implements IPipeSign {

    @Getter
    private final PipeItemsCraftingLogistics pipe;

    CraftingPipeSign(PipeItemsCraftingLogistics pipe) {
        this.pipe = pipe;
    }

    @Override
    public PipeSignType type() {
        return PipeSignType.CRAFTING;
    }

    @Override
    public CustomPacketPayload getPacket() {
        final Container inventory = pipe.getDummyInventory();
        final List<ItemStack> slots = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            slots.add(inventory.getItem(slot));
        }
        return new CraftingDummyInventoryMessage(
            new ModuleTarget(pipe.getPos(), Optional.of(ModulePositionType.IN_PIPE), 0), slots);
    }
}
