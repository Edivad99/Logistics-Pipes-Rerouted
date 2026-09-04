package logisticspipes.client.gui.screen;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.to_server.module.OpenChassisGuiMessage;

public abstract class ModuleBaseScreen<T extends AbstractContainerMenu>
    extends LogisticsBaseGuiScreen<T> {

    @Getter
    protected LogisticsModule module;

    public ModuleBaseScreen(T par1Container, LogisticsModule module) {
        super(par1Container);
        this.module = module;
    }

    public ModuleBaseScreen(T menu, Inventory inventory, Component title, LogisticsModule module,
        int panelWidth, int panelHeight) {
        super(menu, inventory, title, panelWidth, panelHeight, 0, 0);
        this.module = module;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char typedChar = (char) event.codepoint();
        int keyCode = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
        if (module == null) {
            return super.charTyped(event);
        }
        if (keyCode == 1 || typedChar == 'e') {
            if (module.getSlot() == ModulePositionType.SLOT) {
                ClientPacketDistributor.sendToServer(new OpenChassisGuiMessage(module.getBlockPos()));
            }
            return super.charTyped(event);
        }
        return super.charTyped(event);
    }
}
