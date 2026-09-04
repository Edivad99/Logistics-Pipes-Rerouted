package logisticspipes.client.gui.popup;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.network.to_server.block.SaveDiskContentMessage;
import logisticspipes.network.to_server.block.SetDiskNameMessage;
import logisticspipes.network.to_server.orderer.RequestDiskMacroMessage;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;

public class GuiDiskPopup extends SubGuiScreen {

    private static final int SEARCH_WIDTH = 120;
    private final IDiskProvider diskProvider;
    private final TextListDisplay textList;
    private boolean editName = false;
    private boolean displayCursor = false;
    private long oldSystemTime = 0;
    private String name1;
    private String name2;

    public GuiDiskPopup(IDiskProvider diskProvider) {
        super(150, 200, 0, 0);
        this.diskProvider = diskProvider;
        name2 = "";
        if (diskProvider.getDisk().has(DataComponents.CUSTOM_NAME)) {
            name1 = diskProvider.getDisk().get(DataComponents.CUSTOM_NAME).getString();
        } else {
            name1 = "Disk";
        }
        textList = new TextListDisplay(this, 6, 46, 6, 30, 12, new TextListDisplay.List() {

            @Override
            public int getSize() {
                diskProvider.getDisk().update(
                    DataComponents.CUSTOM_DATA,
                    CustomData.EMPTY,
                    customData -> {
                        CompoundTag tag = customData.copyTag();
                        if (!tag.contains("macroList")) {
                            ListTag list = new ListTag();
                            tag.put("macroList", list);
                        }
                        return CustomData.of(tag);
                    }
                );

                var tag = Objects.requireNonNull(diskProvider.getDisk().get(DataComponents.CUSTOM_DATA)).copyTag();
                ListTag list = tag.getListOrEmpty("macroList");
                return list.size();
            }

            @Override
            public String getTextAt(int index) {
                diskProvider.getDisk().update(
                    DataComponents.CUSTOM_DATA,
                    CustomData.EMPTY,
                    customData -> {
                        CompoundTag tag = customData.copyTag();
                        if (!tag.contains("macroList")) {
                            ListTag list = new ListTag();
                            tag.put("macroList", list);
                        }
                        return CustomData.of(tag);
                    }
                );

                var tag = Objects.requireNonNull(diskProvider.getDisk().get(DataComponents.CUSTOM_DATA)).copyTag();
                ListTag list = tag.getListOrEmpty("macroList");
                if (index < list.size()) {
                    return list.getCompoundOrEmpty(index).getStringOr("name", "");
                }
                return "";
            }

            @Override
            public int getTextColor(int index) {
                return 0xFFFFFF;
            }
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double i = event.x();
        double j = event.y();
        int k = event.button();
        int x = (int) i - guiLeft;
        int y = (int) j - guiTop;
        textList.mouseClicked(i, j, k);
        if (k == 0) {
            if (10 < x && x < 138 && 29 < y && y < 44) {
                editName = true;
            } else if (editName) {
                writeDiskName();
            } else {
                return super.mouseClicked(event, doubleClick);
            }
        } else {
            return super.mouseClicked(event, doubleClick);
        }
        return true;
    }

    private void writeDiskName() {
        editName = false;
        ClientPacketDistributor.sendToServer(new SetDiskNameMessage(
            diskProvider.getBlockPos(), name1 + name2));
        diskProvider.getDisk().set(DataComponents.CUSTOM_NAME, Component.literal(name1 + name2));
        ClientPacketDistributor.sendToServer(new SaveDiskContentMessage(
            diskProvider.getBlockPos(), diskProvider.getDisk()));
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton req = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10, "Request");
        req.setPressListener(b -> handleRequest());
        addRenderableWidget(req);
        SmallGuiButton exit = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, "Exit");
        exit.setPressListener(b -> exitGui());
        addRenderableWidget(exit);
        SmallGuiButton addEdit = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, "Add/Edit");
        addEdit.setPressListener(b -> handleAddEdit());
        addRenderableWidget(addEdit);
        SmallGuiButton del = new SmallGuiButton(3, xCenter - 66, bottom - 15, 50, 10, "Delete");
        del.setPressListener(b -> handleDelete());
        addRenderableWidget(del);
        SmallGuiButton up = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
        up.setPressListener(b -> textList.scrollDown());
        addRenderableWidget(up);
        SmallGuiButton dn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
        dn.setPressListener(b -> textList.scrollUp());
        addRenderableWidget(dn);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        guiGraphics.text(minecraft.font, "Disk", xCenter - (minecraft.font.width("Disk") / 2), guiTop + 10, 0xFFFFFFFF,
            true);

        //NameInput
        if (editName) {
            guiGraphics.fill(guiLeft + 10, guiTop + 28, right - 10, guiTop + 45, Color.getValue(Color.BLACK));
            guiGraphics.fill(guiLeft + 11, guiTop + 29, right - 11, guiTop + 44, Color.getValue(Color.WHITE));
        } else {
            guiGraphics.fill(guiLeft + 11, guiTop + 29, right - 11, guiTop + 44, Color.getValue(Color.BLACK));
        }
        guiGraphics.fill(guiLeft + 12, guiTop + 30, right - 12, guiTop + 43, Color.getValue(Color.DARKER_GREY));

        guiGraphics.text(minecraft.font, name1 + name2, guiLeft + 15, guiTop + 33, 0xFFFFFFFF, false);

        //guiGraphics.fill(guiLeft + 6, guiTop + 46, right - 6, bottom - 30, Color.getValue(Color.GREY));

        textList.extractGuiBackground(guiGraphics, mouseX, mouseY);

        if (editName) {
            int lineX = guiLeft + 15 + minecraft.font.width(name1);
            if (System.currentTimeMillis() - oldSystemTime > 500) {
                displayCursor = !displayCursor;
                oldSystemTime = System.currentTimeMillis();
            }
            if (displayCursor) {
                guiGraphics.fill(lineX, guiTop + 31, lineX + 1, guiTop + 42, Color.getValue(Color.WHITE));
            }
        }
    }

    // Deferred: scroll wheel handling not wired

    private void handleRequest() {
        ClientPacketDistributor.sendToServer(new RequestDiskMacroMessage(
            diskProvider.getBlockPos(),
            textList.getSelected()));
    }

    private void handleDelete() {
        diskProvider.getDisk().update(
            DataComponents.CUSTOM_DATA,
            CustomData.EMPTY,
            customData -> {
                CompoundTag tag = customData.copyTag();
                if (!tag.contains("macroList")) {
                    ListTag list = new ListTag();
                    tag.put("macroList", list);
                }

                ListTag list = tag.getListOrEmpty("macroList");
                ListTag newList = new ListTag();

                for (int i = 0; i < list.size(); i++) {
                    if (i != textList.getSelected()) {
                        newList.add(list.getCompoundOrEmpty(i));
                    }
                }

                textList.setSelected(-1);
                tag.put("macroList", newList);
                return CustomData.of(tag);
            }
        );

        ClientPacketDistributor.sendToServer(new SaveDiskContentMessage(
            diskProvider.getBlockPos(), diskProvider.getDisk()));
    }

    private void handleAddEdit() {
        String macroName = "";
        if (diskProvider.getDisk().has(DataComponents.CUSTOM_DATA)) {
            CompoundTag nbt = Objects.requireNonNull(diskProvider.getDisk().get(DataComponents.CUSTOM_DATA)).copyTag();
            if (nbt.contains("macroList")) {
                ListTag list = nbt.getListOrEmpty("macroList");
                if (textList.getSelected() != -1 && textList.getSelected() < list.size()) {
                    CompoundTag entry = list.getCompoundOrEmpty(textList.getSelected());
                    macroName = entry.getStringOr("name", "");
                }
            }
        }
        setSubGui(new GuiAddMacro(diskProvider, macroName));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
        if (editName) {
            if (c == 13) {
                writeDiskName();
                return true;
            } else if (i == 47 && Minecraft.getInstance().hasControlDown()) {
                name1 = name1 + Minecraft.getInstance().keyboardHandler.getClipboard();
            } else if (c == 8) {
                if (name1.length() > 0) {
                    name1 = name1.substring(0, name1.length() - 1);
                }
                return true;
            } else if (Character.isLetterOrDigit(c) || c == ' ') {
                if (minecraft.font.width(name1 + c + name2) <= SEARCH_WIDTH) {
                    name1 += c;
                }
                return true;
            } else if (i == 203) { //Left
                if (name1.length() > 0) {
                    name2 = name1.substring(name1.length() - 1) + name2;
                    name1 = name1.substring(0, name1.length() - 1);
                }
            } else if (i == 205) { //Right
                if (name2.length() > 0) {
                    name1 += name2.substring(0, 1);
                    name2 = name2.substring(1);
                }
            } else if (i == 1) { //ESC
                writeDiskName();
            } else if (i == 28) { //Enter
                writeDiskName();
            } else if (i == 199) { //Pos
                name2 = name1 + name2;
                name1 = "";
            } else if (i == 207) { //Ende
                name1 = name1 + name2;
                name2 = "";
            } else if (i == 211) { //Entf
                if (name2.length() > 0) {
                    name2 = name2.substring(1);
                }
            }
            //		} else if (Minecraft.getInstance().hasShiftDown()){
            //			return super.charTyped(event);
            //		} else if (Minecraft.getInstance().hasControlDown()){
            //			return super.charTyped(event);
        } else {
            return super.charTyped(event);
        }
        return false;
    }
}
