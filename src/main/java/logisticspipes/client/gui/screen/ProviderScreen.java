package logisticspipes.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.BooleanProperty;
import logisticspipes.api.property.EnumProperty;
import logisticspipes.api.property.layer.PropertyLayer;
import logisticspipes.api.property.layer.ValuePropertyOverlay;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_server.module.SetModulePropertiesMessage;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.inventory.ProviderMode;
import network.rs485.logisticspipes.inventory.container.ProviderContainer;
import network.rs485.logisticspipes.util.TextUtil;

public class ProviderScreen extends ModuleBaseScreen<ProviderContainer> {

    private static final String PREFIX = "gui.providerpipe.";

    private static final int PANEL_WIDTH = 175;

    /** The title wraps, so two lines are always reserved for it and the rest of the panel is fixed. */
    private static final int TITLE_Y = 5;
    private static final int TITLE_LINES = 2;

    /** Top-left of the 3x3 filter grid backgrounds; the slots themselves sit one pixel inside. */
    static final int FILTER_X = 60;
    static final int FILTER_Y = 28;
    private static final int GRID_SIZE = 3;
    private static final int SLOT_SIZE = 18;

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_Y = FILTER_Y + (GRID_SIZE * SLOT_SIZE - BUTTON_HEIGHT) / 2;
    private static final int SWITCH_BUTTON_X = 7;
    private static final int FILTER_MODE_BUTTON_X = 118;

    private static final int EXCESS_LABEL_Y = 86;
    private static final int MODE_LABEL_Y = 98;

    static final int PLAYER_INVENTORY_Y = 112;
    private static final int PANEL_HEIGHT = PLAYER_INVENTORY_Y + 58 + 18 + 6;

    private final ModuleProvider providerModule;
    private final PropertyLayer propertyLayer;
    private final ValuePropertyOverlay<ProviderMode, EnumProperty<ProviderMode>> providerModeOverlay;
    private final ValuePropertyOverlay<Boolean, BooleanProperty> isExclusionFilterOverlay;

    private @Nullable SmallGuiButton filterModeButton;

    public ProviderScreen(ProviderContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.getModule(), PANEL_WIDTH, PANEL_HEIGHT);
        providerModule = menu.getModule();
        propertyLayer = menu.getPropertyLayer();
        providerModeOverlay = propertyLayer.overlay(providerModule.providerMode);
        isExclusionFilterOverlay = propertyLayer.overlay(providerModule.isExclusionFilter);
        propertyLayer.addObserver(providerModule.isExclusionFilter, prop -> updateFilterModeButton());
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton switchButton = new SmallGuiButton(0, leftPos + SWITCH_BUTTON_X, topPos + BUTTON_Y,
            BUTTON_WIDTH, BUTTON_HEIGHT, TextUtil.translate(PREFIX + "Switch"));
        switchButton.setPressListener(button -> providerModeOverlay.write(EnumProperty::next));
        addRenderableWidget(switchButton);

        filterModeButton = new SmallGuiButton(1, leftPos + FILTER_MODE_BUTTON_X, topPos + BUTTON_Y,
            BUTTON_WIDTH, BUTTON_HEIGHT, "");
        filterModeButton.setPressListener(button -> {
            isExclusionFilterOverlay.write(BooleanProperty::toggle);
            updateFilterModeButton();
        });
        addRenderableWidget(filterModeButton);
        updateFilterModeButton();
    }

    private void updateFilterModeButton() {
        if (filterModeButton != null) {
            filterModeButton.setMessage(Component.literal(
                TextUtil.translate(PREFIX + (isExclusionFilterOverlay.get() ? "Exclude" : "Include"))));
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        propertyLayer.unregister();
        if (minecraft.player != null && minecraft.level != null && !propertyLayer.getProperties().isEmpty()) {
            // send update to server, when there are changed properties
            ClientPacketDistributor.sendToServer(SetModulePropertiesMessage.of(
                ModuleTarget.of(providerModule), propertyLayer, minecraft.level.registryAccess()));
        }
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + FILTER_X + column * SLOT_SIZE,
                    topPos + FILTER_Y + row * SLOT_SIZE);
            }
        }
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 7, topPos + PLAYER_INVENTORY_Y);
    }

    /**
     * The inventory name is longer than the panel, so it wraps and is centred line by line inside
     * the band reserved for it.
     */
    private void drawWrappedTitle(GuiGraphicsExtractor guiGraphics, Minecraft mc, String title, int color) {
        List<FormattedCharSequence> lines = mc.font.split(Component.literal(title), PANEL_WIDTH - 14);
        int lineHeight = mc.font.lineHeight;
        int top = TITLE_Y + (TITLE_LINES * lineHeight - lines.size() * lineHeight) / 2;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            guiGraphics.text(mc.font, line, (PANEL_WIDTH - mc.font.width(line)) / 2, top + i * lineHeight,
                color, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        Minecraft mc = Minecraft.getInstance();
        int textColor = Color.TEXT_DARK.getValue();

        drawWrappedTitle(guiGraphics, mc, providerModule.filterInventory.getName(), textColor);

        guiGraphics.text(mc.font, TextUtil.translate(PREFIX + "ExcessInventory"), 7, EXCESS_LABEL_Y, textColor, false);
        guiGraphics.text(mc.font, TextUtil.translate(providerModeOverlay.get().getModeTranslationKey()), 7,
            MODE_LABEL_Y, textColor, false);
    }
}
