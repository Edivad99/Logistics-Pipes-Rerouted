package logisticspipes.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.world.inventory.FirewallMenu;
import network.rs485.logisticspipes.util.TextUtil;

public class FirewallScreen extends LogisticsBaseGuiScreen<FirewallMenu> {

    private static final String PREFIX = "gui.firewall.";

    private final PipeItemsFirewall pipe;

    public FirewallScreen(FirewallMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 260, 0, 0);
        this.pipe = menu.getPipe();
    }

    @Override
    public void init() {
        super.init();
        final String blocked = TextUtil.translate(FirewallScreen.PREFIX + "Blocked");
        final String allowed = TextUtil.translate(FirewallScreen.PREFIX + "Allowed");
        addRenderableWidget(new GuiStringHandlerButton(0, width / 2 + 23, height / 2 + 27 - 139, 60, 20,
            () -> pipe.isBlocking() ? blocked : allowed, () -> pipe.setBlocking(!pipe.isBlocking())));
        addRenderableWidget(new GuiStringHandlerButton(1, width / 2 + 23, height / 2 + 60 - 139, 60, 20,
            () -> pipe.isBlockProvider() ? blocked : allowed, () -> pipe.setBlockProvider(!pipe.isBlockProvider())));
        addRenderableWidget(new GuiStringHandlerButton(2, width / 2 + 23, height / 2 + 93 - 139, 60, 20,
            () -> pipe.isBlockCrafter() ? blocked : allowed, () -> pipe.setBlockCrafter(!pipe.isBlockCrafter())));
        addRenderableWidget(new GuiStringHandlerButton(3, width / 2 + 23, height / 2 + 126 - 139, 60, 20,
            () -> pipe.isBlockSorting() ? blocked : allowed, () -> pipe.setBlockSorting(!pipe.isBlockSorting())));
        addRenderableWidget(new GuiStringHandlerButton(4, width / 2 + 23, height / 2 + 160 - 139, 60, 20,
            () -> pipe.isBlockPower() ? blocked : allowed, () -> pipe.setBlockPower(!pipe.isBlockPower())));
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 33, topPos + 175);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + x * 18 + 16, topPos + y * 18 + 40);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Firewall"), 45, 8, 0xFF404040,
            false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Filter") + ":", 14, 28, 0xFF404040,
            false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Filtereditemsare") + ":", 125, 8,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Providing") + ":", 144, 41,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Crafting") + ":", 146, 74,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Sorting") + ":", 150, 107,
            0xFF404040, false);
        guiGraphics.text(minecraft.font, TextUtil.translate(FirewallScreen.PREFIX + "Powerflow") + ":", 142, 141,
            0xFF404040, false);
    }
}
