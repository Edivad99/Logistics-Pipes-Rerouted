package logisticspipes.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.client.gui.popup.SelectItemOutOfList;
import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;
import logisticspipes.network.to_server.gui.DummySlotClickMessage;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The fluid picker a dummy slot opens when it is clicked empty.
 *
 * <p>This used to be a method on the sided proxy interface, with the dedicated server carrying an
 * empty implementation of it. Only a client ever reaches this, so it lives on the client side now.
 */
public final class FluidSelection {

    private FluidSelection() {
    }

    /** Opens the picker over the current screen, and sends the choice back to {@code slotId}. */
    public static void open(int slotId) {
        if (!(Minecraft.getInstance().gui.screen() instanceof LogisticsBaseGuiScreen<?> gui)) {
            throw new UnsupportedOperationException(String.valueOf(Minecraft.getInstance().gui.screen()));
        }
        final List<ItemIdentifierStack> list = new ArrayList<>();
        for (FluidIdentifier fluid : FluidIdentifier.all()) {
            if (fluid == null) {
                continue;
            }
            list.add(fluid.getItemIdentifier().makeStack(1));
        }
        final SelectItemOutOfList subGui = new SelectItemOutOfList(list, slot -> {
            if (slot == -1) {
                return;
            }
            ClientPacketDistributor.sendToServer(
                new DummySlotClickMessage(slotId, list.get(slot).makeNormalStack(), 0));
        });
        SubGuiScreen deepest = null;
        if (gui.hasSubGui()) {
            deepest = gui.getSubGui();
            while (deepest.hasSubGui()) {
                deepest = deepest.getSubGui();
            }
        }
        if (deepest == null) {
            gui.setSubGui(subGui);
        } else {
            deepest.setSubGui(subGui);
        }
    }
}
