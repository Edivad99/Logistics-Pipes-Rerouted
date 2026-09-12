package logisticspipes.client.gui.screen;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.BooleanProperty;
import logisticspipes.api.property.layer.PropertyLayer;
import logisticspipes.api.property.layer.PropertyOverlay;
import logisticspipes.api.property.layer.ValuePropertyOverlay;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.module.ItemSinkImportRequestMessage;
import logisticspipes.network.to_server.module.SetModulePropertiesMessage;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.container.ItemSinkContainer;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemSinkScreen extends ModuleBaseScreen<ItemSinkContainer> {

    private static final String PREFIX = "gui.itemsink.";

    private static final int PANEL_WIDTH = 175;

    /** Top-left of the filter slot backgrounds; the slots themselves sit one pixel inside. */
    private static final int FILTER_X = 7;
    private static final int FILTER_Y = 18;

    private static final int BUTTON_ROW_Y = 40;
    private static final int BUTTON_ROW_HEIGHT = 20;
    private static final int PLAYER_INVENTORY_Y = 66;
    private static final int PANEL_HEIGHT = PLAYER_INVENTORY_Y + 58 + 18 + 6;

    /** The import button, the label and the toggle share one row; the label wraps into what is left. */
    private static final int IMPORT_BUTTON_X = 7;
    private static final int IMPORT_BUTTON_WIDTH = 50;
    private static final int TOGGLE_BUTTON_X = 118;
    private static final int TOGGLE_BUTTON_WIDTH = 50;
    private static final int LABEL_LEFT = IMPORT_BUTTON_X + IMPORT_BUTTON_WIDTH + 3;
    private static final int LABEL_RIGHT = TOGGLE_BUTTON_X - 3;

    private final ModuleItemSink itemSinkModule;
    private final PropertyLayer propertyLayer;
    private final ValuePropertyOverlay<Boolean, BooleanProperty> defaultRouteOverlay;
    private final PropertyOverlay<ItemIdentifierInventory, ItemIdentifierInventoryProperty> filterInventoryOverlay;

    /** The import button only makes sense next to a pipe, which a module in hand has none of. */
    private final boolean inHand;

    private @Nullable SmallGuiButton defaultRouteButton;

    public ItemSinkScreen(ItemSinkContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.getModule(), PANEL_WIDTH, PANEL_HEIGHT);
        itemSinkModule = menu.getModule();
        propertyLayer = menu.getPropertyLayer();
        defaultRouteOverlay = propertyLayer.overlay(itemSinkModule.defaultRoute);
        filterInventoryOverlay = propertyLayer.overlayOf(itemSinkModule.filterInventory);
        inHand = isInHand(menu);
        propertyLayer.addObserver(itemSinkModule.defaultRoute, prop -> updateDefaultRouteButton());
    }

    static boolean isInHand(ItemSinkContainer menu) {
        return menu.getTarget().slot().orElse(null) == ModulePositionType.IN_HAND;
    }

    @Override
    public void init() {
        super.init();
        // Importing pulls from the inventory the pipe is attached to, which a module in hand has none of.
        if (!inHand) {
            SmallGuiButton importButton = new SmallGuiButton(0, leftPos + IMPORT_BUTTON_X, topPos + BUTTON_ROW_Y,
                IMPORT_BUTTON_WIDTH, BUTTON_ROW_HEIGHT, TextUtil.translate(PREFIX + "import"));
            importButton.setPressListener(button -> ClientPacketDistributor.sendToServer(
                new ItemSinkImportRequestMessage(ModuleTarget.of(itemSinkModule))));
            addRenderableWidget(importButton);
        }
        defaultRouteButton = new SmallGuiButton(1, leftPos + TOGGLE_BUTTON_X, topPos + BUTTON_ROW_Y,
            TOGGLE_BUTTON_WIDTH, BUTTON_ROW_HEIGHT, "");
        defaultRouteButton.setPressListener(button -> {
            defaultRouteOverlay.write(BooleanProperty::toggle);
            updateDefaultRouteButton();
        });
        addRenderableWidget(defaultRouteButton);
        updateDefaultRouteButton();
    }

    private void updateDefaultRouteButton() {
        if (defaultRouteButton != null) {
            defaultRouteButton.setMessage(Component.literal(
                TextUtil.translate(PREFIX + (defaultRouteOverlay.get() ? "Yes" : "No"))));
        }
    }

    /** Fills the filter from the attached inventory, in response to the import button. */
    public void importFromInventory(List<ItemIdentifier> importedItems) {
        if (importedItems.isEmpty()) {
            return;
        }
        filterInventoryOverlay.writeVoid(filterInventory -> {
            for (int i = 0; i < filterInventory.getSize(); i++) {
                if (i < importedItems.size()) {
                    filterInventory.setItem(i, importedItems.get(i).makeStack(1));
                } else {
                    filterInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        });
    }

    @Override
    public void onClose() {
        super.onClose();
        propertyLayer.unregister();
        if (minecraft != null && minecraft.player != null && minecraft.level != null
            && !propertyLayer.getProperties().isEmpty()) {
            // send update to server, when there are changed properties
            ClientPacketDistributor.sendToServer(SetModulePropertiesMessage.of(
                ModuleTarget.of(itemSinkModule), propertyLayer, minecraft.level.registryAccess()));
        }
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        for (int column = 0; column < 9; column++) {
            LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + FILTER_X + column * 18, topPos + FILTER_Y);
        }
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + FILTER_X, topPos + PLAYER_INVENTORY_Y);
    }








    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        Minecraft mc = Minecraft.getInstance();

        String title = itemSinkModule.filterInventory.getName();
        guiGraphics.text(mc.font, title, (PANEL_WIDTH - mc.font.width(title)) / 2, 6,
            Color.TEXT_DARK.getValue(), false);

        drawWrappedLabel(guiGraphics, mc, TextUtil.translate(PREFIX + "Defaultroute") + ":");

    }

    /**
     * The label shares its row with two buttons, so it wraps into the gap between them and is drawn
     * right-aligned and vertically centred on the row.
     */
    private void drawWrappedLabel(GuiGraphicsExtractor guiGraphics, Minecraft mc, String text) {
        List<FormattedCharSequence> lines = mc.font.split(Component.literal(text), LABEL_RIGHT - LABEL_LEFT);
        int lineHeight = mc.font.lineHeight;
        int top = BUTTON_ROW_Y + (BUTTON_ROW_HEIGHT - lines.size() * lineHeight) / 2;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            guiGraphics.text(mc.font, line, LABEL_RIGHT - mc.font.width(line), top + i * lineHeight,
                Color.TEXT_DARK.getValue(), false);
        }
    }


}
