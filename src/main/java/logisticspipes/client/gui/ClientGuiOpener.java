package logisticspipes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.network.packets.gui.OpenGUIPacket;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.LPDataIOWrapper;

public final class ClientGuiOpener {

    private ClientGuiOpener() {
    }

    public static void openGui(OpenGUIPacket packet, Player player) {
        int guiID = packet.getGuiID();
        GuiProvider provider = NewGuiHandler.guilist.get(guiID).template();
        LPDataIOWrapper.provideData(packet.getGuiData(), provider::readData);

        if (provider instanceof PopupGuiProvider && packet.getWindowID() == -2) {
            if (Minecraft.getInstance().screen instanceof LogisticsBaseGuiScreen baseGUI) {
                SubGuiScreen newSub;
                try {
                    newSub = (SubGuiScreen) provider.getClientGui(player);
                } catch (TargetNotFoundException e) {
                    throw e;
                } catch (Exception e) {
                    LogisticsPipes.LOG.error(packet.getClass().getName());
                    LogisticsPipes.LOG.error(packet.toString());
                    throw new RuntimeException(e);
                }
                if (newSub != null) {
                    if (!baseGUI.hasSubGui()) {
                        baseGUI.setSubGui(newSub);
                    } else {
                        SubGuiScreen canidate = baseGUI.getSubGui();
                        while (canidate != null && canidate.hasSubGui()) {
                            canidate = canidate.getSubGui();
                        }
                        canidate.setSubGui(newSub);
                    }
                }
            }
        } else {
            AbstractContainerScreen screen;
            try {
                screen = (AbstractContainerScreen) provider.getClientGui(player);
            } catch (TargetNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LogisticsPipes.LOG.error("getClientGui failed for provider {}", provider.getClass().getName(), e);
                return;
            }
            // Mirror the server-side windowId onto the client-side menu so vanilla
            // slot-sync packets (ClientboundContainerSetContent etc.) reach this menu.
            if (screen != null && screen.getMenu() != null) {
                try {
                    NewGuiHandler.CONTAINER_ID_FIELD.setInt(screen.getMenu(), packet.getWindowID());
                } catch (ReflectiveOperationException ex) {
                    LogisticsPipes.LOG.error("Failed to set client menu containerId", ex);
                }
                player.containerMenu = screen.getMenu();
            }
            if (screen == null) {
                LogisticsPipes.LOG.warn(
                    "getClientGui returned null for provider {} (guiID={}) — closing current screen instead of opening a GUI",
                    provider.getClass().getName(), guiID);
            }
            Minecraft.getInstance().setScreen(screen);
        }
    }
}
