/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.FuzzySlotSettingsPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.extension.GuiExtensionController;
import logisticspipes.utils.gui.extension.GuiExtensionController.GuiSide;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.util.FuzzyFlag;
import network.rs485.logisticspipes.util.FuzzyUtil;
import network.rs485.logisticspipes.util.TextUtil;

public abstract class LogisticsBaseGuiScreen extends AbstractContainerScreen implements ISubGuiController, IGuiAccess {

    protected static final Identifier ITEMSINK = LPConstants.rl("textures/gui/itemsink.png");
    protected final int xCenterOffset;
    protected final int yCenterOffset;
    private final List<EditBox> textFields = new ArrayList<>();
    @Getter
    protected int right;
    @Getter
    protected int bottom;
    protected int xCenter;
    protected int yCenter;
    protected List<IRenderSlot> slots = new ArrayList<>();
    protected GuiExtensionController extensionControllerLeft = new GuiExtensionController(GuiSide.LEFT);
    protected GuiExtensionController extensionControllerRight = new GuiExtensionController(GuiSide.RIGHT);
    /**
     * Compatibility bridge: mirrors widgets added via addRenderableWidget so old buttonList.get(i) still works.
     */
    protected List<AbstractWidget> buttonList = new ArrayList<>();
    /**
     * Panel size, standing in for {@code imageWidth} / {@code imageHeight}.
     *
     * <p>26.1.2 made those final: they are constructor arguments of
     * {@code AbstractContainerScreen} now. LP cannot use them, because several of its screens
     * resize themselves after construction -- the crafting pipe grows for its satellite rows, the
     * request table widens and narrows as the recipe panel is toggled -- so the size is kept here
     * instead. Nothing is lost: LP already computed its own {@code leftPos}/{@code topPos} in
     * {@link #init()} rather than relying on vanilla's centring.</p>
     */
    protected int panelWidth;
    protected int panelHeight;
    @Nullable
    private SubGuiScreen subGui;
    @Nullable
    private AbstractWidget selectedButton;
    private int currentDrawScreenMouseX;
    private int currentDrawScreenMouseY;
    /**
     * Stored during rendering so non-render methods (drawPoint, fillRect, etc.) can use it.
     */
    @Deprecated(forRemoval = true)
    private GuiGraphicsExtractor guiGraphics;
    private IFuzzySlot fuzzySlot;
    private boolean fuzzySlotActiveGui;
    private int fuzzySlotGuiHoverTime;
    private final Queue<Runnable> renderAtTheEnd = new LinkedList<>();

    public LogisticsBaseGuiScreen(int panelWidth, int panelHeight, int xCenterOffset, int yCenterOffset) {
        this(new DummyContainer(null, null), panelWidth, panelHeight, xCenterOffset, yCenterOffset);
    }

    public LogisticsBaseGuiScreen(AbstractContainerMenu container) {
        super(container, Minecraft.getInstance().player.getInventory(), Component.empty());
        xCenterOffset = 0;
        yCenterOffset = 0;
    }

    public LogisticsBaseGuiScreen(AbstractContainerMenu container, Inventory inventory, Component title,
        int panelWidth, int panelHeight, int xCenterOffset, int yCenterOffset) {
        super(container, inventory, title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.xCenterOffset = xCenterOffset;
        this.yCenterOffset = yCenterOffset;
    }

    public LogisticsBaseGuiScreen(AbstractContainerMenu container, int panelWidth, int panelHeight, int xCenterOffset,
        int yCenterOffset) {
        super(container, Minecraft.getInstance().player.getInventory(), Component.empty());
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.xCenterOffset = xCenterOffset;
        this.yCenterOffset = yCenterOffset;
    }

    public int getCurrentMouseX() {
        return currentDrawScreenMouseX;
    }

    public int getCurrentMouseY() {
        return currentDrawScreenMouseY;
    }

    /**
     * The panel size as everyone outside LP sees it.
     *
     * <p>The fields behind these are final and were filled in by the three-argument
     * {@code AbstractContainerScreen} constructor, which defaults them to 176x166 -- so without
     * this override every LP screen, whatever its real size, claims to be a vanilla-sized chest
     * panel. Vanilla itself only uses them for centring, which LP does not rely on, but other mods
     * read them to keep clear of the window: JEI asks {@code getImageWidth}/{@code getImageHeight}
     * to place its ingredient list, and drew it straight over the request table.</p>
     */
    @Override
    public int getImageWidth() {
        return panelWidth;
    }

    @Override
    public int getImageHeight() {
        return panelHeight;
    }

    /**
     * Vanilla decides whether a click counts as outside the GUI -- which is what throws the carried
     * stack away -- from the fields rather than from the getters above, so it needs its own
     * override.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        return mouseX < guiLeft || mouseY < guiTop
            || mouseX >= guiLeft + panelWidth || mouseY >= guiTop + panelHeight;
    }

    @Override
    public void init() {
        super.init();
        buttonList.clear();
        leftPos = width / 2 - panelWidth / 2 + xCenterOffset;
        topPos = height / 2 - panelHeight / 2 + yCenterOffset;

        right = width / 2 + panelWidth / 2 + xCenterOffset;
        bottom = height / 2 + panelHeight / 2 + yCenterOffset;

        xCenter = (right + leftPos) / 2;
        yCenter = (bottom + topPos) / 2;
        extensionControllerLeft.setMaxBottom(bottom);
        extensionControllerRight.setMaxBottom(bottom);
    }

    @Override
    public boolean hasSubGui() {
        return subGui != null;
    }

    @Override
    public @Nullable SubGuiScreen getSubGui() {
        return subGui;
    }

    @Override
    public void setSubGui(SubGuiScreen gui) {
        if (subGui == null) {
            subGui = gui;
            subGui.register(this);
            subGui.init(width, height);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (subGui != null) {
            subGui.resize(width, height);
        }
    }

    @Override
    public void resetSubGui() {
        subGui = null;
    }

    /**
     * Vanilla calls this once per frame, from {@code extractRenderStateWithTooltipAndSubtitles},
     * in its own stratum ahead of the one the widgets go into -- so everything drawn here lands
     * underneath them without any ordering care of LP's own.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        extractGuiBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * The panel chrome, drawn behind the widgets.
     *
     * <p>This used to be {@code AbstractContainerScreen#renderBg}, which 26.1.2 removed: a container
     * screen now paints its own background by overriding {@link #extractBackground}, the way
     * {@code AbstractFurnaceScreen} does. LP keeps a hook of its own only because dozens of screens
     * override it and because the argument order differs.</p>
     *
     * <p>Same name and argument order as {@link SubGuiScreen#extractGuiBackground}, which is the
     * matching hook on the popup side.</p>
     */
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderExtensions(guiGraphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.guiGraphics = guiGraphics;
        currentDrawScreenMouseX = mouseX;
        currentDrawScreenMouseY = mouseY;
        checkButtons();
        if (subGui != null) {
            // The mouse is reported as (0,0) so nothing underneath the popup highlights or shows a
            // tooltip while the popup has the pointer.
            super.extractRenderState(guiGraphics, 0, 0, partialTicks);
            subGui.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
            for (IRenderSlot slot : slots) {
                int localMouseX = mouseX - leftPos;
                int localMouseY = mouseY - topPos;
                int mouseXMax = localMouseX - slot.getSize();
                int mouseYMax = localMouseY - slot.getSize();
                if (slot.getXPos() < localMouseX && slot.getXPos() > mouseXMax && slot.getYPos() < localMouseY
                    && slot.getYPos() > mouseYMax) {
                    if (slot.displayToolTip()) {
                        if (slot.getToolTipText() != null && !slot.getToolTipText().isEmpty()) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(slot.getToolTipText());
                            LPGuiGraphics.drawToolTip(guiGraphics, mouseX, mouseY, list, ChatFormatting.WHITE);
                        }
                    }
                }
            }
            this.extractTooltip(guiGraphics, mouseX, mouseY);
            renderToolTips(guiGraphics, mouseX, mouseY, partialTicks);
        }
        Runnable run = renderAtTheEnd.poll();
        while (run != null) {
            run.run();
            run = renderAtTheEnd.poll();
        }
    }

    protected void renderExtensions(GuiGraphicsExtractor guiGraphics) {
        extensionControllerLeft.render(guiGraphics, leftPos, topPos);
        extensionControllerRight.render(guiGraphics, leftPos + panelWidth, topPos);
    }

    /**
     * Vanilla's per-slot entry point. LP1 hooked the equivalent {@code drawSlot(Slot)} to skip slots that are
     * covered by an open GUI extension and to stamp the fuzzy-flag markers on top; the 1.20.1 port left that
     * method behind as dead code, so neither happened and every slot of the container got drawn.
     */
    @Override
    protected void extractSlot(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY) {
        if (!shouldRenderSlot(slot)) {
            return;
        }
        super.extractSlot(guiGraphics, slot, mouseX, mouseY);
        // The fuzzy markers and their hover panel belong to the screen underneath, so they stay hidden while a
        // popup is up -- otherwise they would draw over it.
        if (subGui == null) {
            onRenderSlot(slot);
        }
    }

    /**
     * Whether {@code slot} is present in the current state of the screen -- a slot that fails here is neither
     * drawn nor interactive. Subclasses narrow this.
     */
    protected boolean shouldRenderSlot(Slot slot) {
        return extensionControllerLeft.renderSlot(slot) && extensionControllerRight.renderSlot(slot);
    }

    /**
     * Vanilla routes every slot hit-test through here: the hover highlight in {@code render}, and
     * {@code findSlot}, which backs slot clicks, releases and drags. Filtering it in one place is what keeps a
     * hidden slot from staying hoverable and clickable where it is not drawn.
     */
    @Override
    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return shouldRenderSlot(slot) && super.isHovering(slot, mouseX, mouseY);
    }

    private void onRenderSlot(Slot slot) {
        if (slot instanceof IFuzzySlot) {
            final IBitSet set = ((IFuzzySlot) slot).getFuzzyFlags();
            int x1 = slot.x;
            int y1 = slot.y;
            // GL_LIGHTING removed — use shaders
            final boolean useOreDict = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.USE_ORE_DICT);
            if (useOreDict) {
                guiGraphics.fill(x1 + 8, y1 - 1, x1 + 17, y1, 0xFFFF4040);
                guiGraphics.fill(x1 + 16, y1, x1 + 17, y1 + 8, 0xFFFF4040);
            }
            final boolean ignoreDamage = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.IGNORE_DAMAGE);
            if (ignoreDamage) {
                guiGraphics.fill(x1 - 1, y1 - 1, x1 + 8, y1, 0xFF40FF40);
                guiGraphics.fill(x1 - 1, y1, x1, y1 + 8, 0xFF40FF40);
            }
            final boolean ignoreNBT = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.IGNORE_NBT);
            if (ignoreNBT) {
                guiGraphics.fill(x1 - 1, y1 + 16, x1 + 8, y1 + 17, 0xFF4040FF);
                guiGraphics.fill(x1 - 1, y1 + 8, x1, y1 + 17, 0xFF4040FF);
            }
            final boolean useOreCategory = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.USE_ORE_CATEGORY);
            if (useOreCategory) {
                guiGraphics.fill(x1 + 8, y1 + 16, x1 + 17, y1 + 17, 0xFF7F7F40);
                guiGraphics.fill(x1 + 16, y1 + 8, x1 + 17, y1 + 17, 0xFF7F7F40);
            }
            final boolean mouseOver = this.isMouseOverSlot(slot, currentDrawScreenMouseX, currentDrawScreenMouseY);
            if (mouseOver) {
                if (fuzzySlot == slot) {
                    fuzzySlotGuiHoverTime++;
                    if (fuzzySlotGuiHoverTime >= 10) {
                        fuzzySlotActiveGui = true;
                    }
                } else {
                    fuzzySlot = (IFuzzySlot) slot;
                    fuzzySlotGuiHoverTime = 0;
                    fuzzySlotActiveGui = false;
                }
            }
            if (fuzzySlotActiveGui && fuzzySlot == slot) {
                if (!mouseOver) {
                    //Check within FuzzyGui
                    if (!isHovering(slot.x, slot.y + 16, 60, 52, currentDrawScreenMouseX, currentDrawScreenMouseY)) {
                        fuzzySlotActiveGui = false;
                        fuzzySlot = null;
                    }
                }
                final int posX = slot.x + leftPos;
                final int posY = slot.y + 17 + topPos;
                renderAtTheEnd.add(() -> {
                    LPGuiGraphics.drawGuiBackGround(guiGraphics, posX, posY, posX + 61, posY + 47, 0.0f, true, true,
                        true, true, true);
                    final String PREFIX = "gui.crafting.";
                    guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "OreDict"), posX + 5, posY + 5,
                        (useOreDict ? 0xFFFF4040 : 0x404040), false);
                    guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "IgnDamage"), posX + 5, posY + 15,
                        (ignoreDamage ? 0xFF40FF40 : 0x404040), false);
                    guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "IgnNBT"), posX + 5, posY + 25,
                        (ignoreNBT ? 0xFF4040FF : 0x404040), false);
                    guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "OrePrefix"), posX + 5, posY + 35,
                        (useOreCategory ? 0xFF7F7F40 : 0x404040), false);
                });
            }
        }
    }

    /**
     * LP's own "is the cursor on this slot" test, used by the fuzzy-slot markers and the slot-finder flows.
     * <p>
     * Presence and bounds come from {@link #isHovering(Slot, double, double)}, so the extension and per-tab
     * filters live in {@link #shouldRenderSlot} only. Two conditions remain on top, and neither is about
     * whether the slot is drawn: an extension may keep a slot visible but inert (the crafting pipe's liquid
     * extension does exactly that until a satellite is chosen), and the open fuzzy panel swallows the cursor
     * from the slots it covers.
     */
    protected boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        if (!isHovering(slot, mouseX, mouseY)) {
            return false;
        }
        if (!extensionControllerLeft.renderSelectSlot(slot) || !extensionControllerRight.renderSelectSlot(slot)) {
            return false;
        }
        return !isMouseInFuzzyPanel(currentDrawScreenMouseX, currentDrawScreenMouseY);
    }

    private boolean isMouseInFuzzyPanel(int x, int y) {
        if (!fuzzySlotActiveGui || fuzzySlot == null) {
            return false;
        }
        return isHovering(fuzzySlot.getX(), fuzzySlot.getY() + 16, 60, 52, x, y);
    }

    protected void checkButtons() {
        for (AbstractWidget button : buttonList) {
            if (extensionControllerLeft.renderButtonControlled(button)) {
                button.visible = extensionControllerLeft.renderButton(button);
            }
            if (extensionControllerRight.renderButtonControlled(button)) {
                button.visible = extensionControllerRight.renderButton(button);
            }
        }
    }

    public <T extends AbstractWidget> T addRenderableWidget(T button) {
        buttonList.add(button);
        return super.addRenderableWidget(button);
    }

    public void addRenderSlot(IRenderSlot slot) {
        slots.add(slot);
    }

    /**
     * Draw tooltips here rather than from {@link #extractLabels}, and in <b>screen</b> coordinates.
     * <p>
     * This runs after {@code AbstractContainerScreen#render} has popped its pose, so nothing is
     * translated: what you pass to {@code GuiGraphicsExtractor#renderTooltip} is what the player sees.
     * {@code extractLabels}, by contrast, runs inside a pose already translated by
     * (leftPos, topPos), so drawing a tooltip there applies the gui origin twice. Mirrors
     * {@code SubGuiScreen#renderToolTips}, so both kinds of screen work the same way.
     * <p>
     * Only called when no popup is open; a popup draws its own tooltips.
     */
    protected void renderToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (mouseX < leftPos) {
            extensionControllerLeft.mouseOver(mouseX, mouseY);
        }
        if (mouseX > leftPos + panelWidth) {
            extensionControllerRight.mouseOver(mouseX, mouseY);
        }
        for (IRenderSlot slot : slots) {
            if (slot instanceof IItemTextureRenderSlot itemTextureRenderSlot) {
                if (slot.drawSlotBackground()) {
                    LPGuiGraphics.drawSlotBackground(guiGraphics, slot.getXPos(), slot.getYPos());
                }
                if (itemTextureRenderSlot.drawSlotIcon() && !itemTextureRenderSlot.customRender(
                    minecraft, 0.0f)) {
                    LPGuiGraphics.renderIconAt(guiGraphics, slot.getXPos() + 1, slot.getYPos() + 1, 0.0f,
                        itemTextureRenderSlot.getTextureIcon());
                }
            } else if (slot instanceof ISmallColorRenderSlot smallColorRenderSlot) {
                if (slot.drawSlotBackground()) {
                    LPGuiGraphics.drawSmallSlotBackground(guiGraphics, slot.getXPos(), slot.getYPos());
                }
                if (smallColorRenderSlot.drawColor()) {
                    guiGraphics.fill(slot.getXPos() + 1, slot.getYPos() + 1, slot.getXPos() + 7, slot.getYPos() + 7,
                        smallColorRenderSlot.getColor());
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double par1 = event.x();
        double par2 = event.y();
        int par3 = event.button();
        // Popups are modal: route all input to the innermost sub-GUI (LP1 parity)
        if (subGui != null) {
            return subGui.mouseClicked(event, doubleClick);
        }
        for (IRenderSlot slot : slots) {
            int mouseX = (int) par1 - leftPos;
            int mouseY = (int) par2 - topPos;
            int mouseXMax = mouseX - slot.getSize();
            int mouseYMax = mouseY - slot.getSize();
            if (slot.getXPos() < mouseX && slot.getXPos() > mouseXMax && slot.getYPos() < mouseY
                && slot.getYPos() > mouseYMax) {
                slot.mouseClicked(par3);
                return true;
            }
        }
        if (isMouseInFuzzyPanel((int) par1, (int) par2)) {
            final int posX = fuzzySlot.getX() + leftPos;
            final int posY = fuzzySlot.getY() + 17 + topPos;
            int sel = -1;
            if (par1 >= posX + 5 && par1 <= posX + 56) {
                if (par2 >= posY + 5 && par2 <= posY + 45) {
                    sel = (int) (par2 - posY - 4) / 10;
                }
            }
            IBitSet set = fuzzySlot.getFuzzyFlags();
            FuzzyFlag flag = switch (sel) {
                case 0 -> FuzzyFlag.USE_ORE_DICT;
                case 1 -> FuzzyFlag.IGNORE_DAMAGE;
                case 2 -> FuzzyFlag.IGNORE_NBT;
                case 3 -> FuzzyFlag.USE_ORE_CATEGORY;
                default -> null;
            };
            if (flag == null) {
                return false;
            }
            set.flip(flag.getBit());
            MainProxy.sendPacketToServer(
                PacketHandler.getPacket(FuzzySlotSettingsPacket.class)
                    .setSlotNumber(fuzzySlot.getSlotId())
                    .setFlags(set.copyValue()));
            return true;
        }
        // Button presses are handled by AbstractContainerScreen/Screen's own widget dispatch
        // (addRenderableWidget wires SmallGuiButton/GuiCheckBox press listeners in this class).
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (subGui != null) {
            return subGui.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (subGui != null) {
            return subGui.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (subGui != null) {
            return subGui.keyPressed(event);
        }
        // While a text field has the focus, swallow the key so the inventory hotkey ('e' by default)
        // types into the field instead of closing the GUI. ESC still closes, like vanilla does.
        if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
            EditBox editing = getEditingTextField();
            if (editing != null) {
                editing.keyPressed(event);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Nullable
    private EditBox getEditingTextField() {
        if (getFocused() instanceof EditBox focused && focused.canConsumeInput()) {
            return focused;
        }
        // Text fields that are not registered as screen children still track their own focus state.
        return InputBar.focusedAmong(textFields);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (subGui != null) {
            return subGui.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (subGui != null) {
            return subGui.mouseReleased(event);
        }
        if (selectedButton != null && event.button() == 0) {
            selectedButton.mouseReleased(event);
            selectedButton = null;
            return true;
        } else if (isMouseInFuzzyPanel((int) (event.x() - leftPos), (int) (event.y() - topPos))) {
            return false;
        } else {
            return super.mouseReleased(event);
        }
    }

    private boolean mouseCanPressButton(int par1, int par2) {
        for (AbstractWidget b : buttonList) {
            if (b.visible && b.isMouseOver(par1, par2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverSlot(int par1, int par2) {
        for (int k = 0; k < menu.slots.size(); ++k) {
            Slot slot = menu.slots.get(k);
            if (isMouseOverSlot(slot, par1, par2)) {
                return true;
            }
        }
        return false;
    }

    public void drawPoint(int x, int y, int color) {
        guiGraphics.fill(x, y, x + 1, y + 1, color);
    }

    public void drawPoint(int x, int y, Color color) {
        guiGraphics.fill(x, y, x + 1, y + 1, Color.getValue(color));
    }

    public void fillRect(int x1, int y1, int x2, int y2, Color color) {
        guiGraphics.fill(x1, y1, x2, y2, Color.getValue(color));
    }

    public void drawLine(int x1, int y1, int x2, int y2, Color color) {
        int lasty = y1;
        for (int dx = 0; x1 + dx < x2; dx++) {
            int plotx = x1 + dx;
            int ploty;

            if (x2 - x1 == 1) {
                ploty = y1 + (y2 - y1) / (x2 - x1) * dx;
            } else {
                ploty = y1 + (y2 - y1) / (x2 - x1 - 1) * dx;
            }

            drawPoint(plotx, ploty, color);
            while (lasty < ploty) {
                drawPoint(plotx, ++lasty, color);
            }
            while (lasty > ploty) {
                drawPoint(plotx, --lasty, color);
            }
        }
        while (lasty < y2) {
            drawPoint(x2, ++lasty, color);
        }
        while (lasty > y2) {
            drawPoint(x2, --lasty, color);
        }
    }

    public void closeGui() throws IOException {
        onClose();
    }

    @Override
    public LogisticsBaseGuiScreen getBaseScreen() {
        return this;
    }

    public void registerTextField(EditBox textField) {
        textFields.add(textField);
    }

    public void drawCenteredString(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color) {
        int actualX = x - minecraft.font.width(text) / 2;
        guiGraphics.text(minecraft.font, text, actualX, y, color, false);
    }
}
