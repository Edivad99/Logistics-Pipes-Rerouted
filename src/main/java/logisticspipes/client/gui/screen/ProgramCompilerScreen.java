package logisticspipes.client.gui.screen;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.CompilerTriggerTaskPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.TextListDisplay;
import logisticspipes.world.inventory.ProgramCompilerMenu;
import logisticspipes.world.item.ItemLogisticsPipe;
import logisticspipes.world.item.ItemModule;
import logisticspipes.world.item.ItemUpgrade;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import network.rs485.logisticspipes.util.TextUtil;

//TODO: Config Option for disabling program compilation
public class ProgramCompilerScreen extends LogisticsBaseGuiScreen {

    private final LogisticsProgramCompilerBlockEntity compiler;
    private final TextListDisplay.List categoryTextList;
    private final TextListDisplay.List programTextList;
    private final TextListDisplay categoryList;
    private final TextListDisplay programList;
    private final TextListDisplay programListLarge;
    private SmallGuiButton catUp;
    private SmallGuiButton catDn;
    private SmallGuiButton unlock;
    private SmallGuiButton progUp;
    private SmallGuiButton progDn;
    private SmallGuiButton programmerButton;
    private InputBar search;

    public ProgramCompilerScreen(ProgramCompilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 180, 190, 0, 0);
        this.compiler = menu.getBlockEntity();

        categoryTextList = new TextListDisplay.List() {

            @Override
            public int getSize() {
                if (compiler.getInventory().getItem(0).isEmpty()) {
                    return 0;
                }
                ListTag list = compiler.getListTagForKey("compilerCategories");
                return (int) LogisticsProgramCompilerBlockEntity.programByCategory.keySet().stream()
                    .filter(it -> list.stream().noneMatch(nbtBase -> nbtBase.asString().orElse("").equals(it.toString())))
                    .count();
            }

            @Override
            public String getTextAt(int index) {
                if (compiler.getInventory().getItem(0).isEmpty()) {
                    return "";
                }
                ListTag list = compiler.getListTagForKey("compilerCategories");
                return TextUtil.translate(
                    "gui.compiler." + LogisticsProgramCompilerBlockEntity.programByCategory.keySet().stream()
                        .filter(it -> list.stream().noneMatch(nbtBase -> nbtBase.asString().orElse("").equals(it.toString())))
                        .skip(index)
                        .findFirst()
                        .map(it -> String.format("%s.%s", it.getNamespace(), it.getPath()))
                        .orElse(null));
            }

            @Override
            public int getTextColor(int index) {
                return 0xFFFFFF;
            }
        };
        categoryList = new TextListDisplay(this, 8, 30, 110, 104, 5, categoryTextList);

        programTextList = new TextListDisplay.List() {

            @Override
            public int getSize() {
                if (compiler.getInventory().getItem(0).isEmpty()) {
                    return 0;
                }
                ListTag list = compiler.getListTagForKey("compilerCategories");
                return getProgramListForSelectionIndex(list).size();
            }

            @Override
            public String getTextAt(int index) {
                if (compiler.getInventory().getItem(0).isEmpty()) {
                    return "";
                }
                ListTag list = compiler.getListTagForKey("compilerCategories");
                ResourceLocation sel = getProgramListForSelectionIndex(list).get(index);

                Item selItem = BuiltInRegistries.ITEM.getValue(sel);
                return TextUtil.translate(selItem.getDescriptionId());
            }

            @Override
            public int getTextColor(int index) {
                if (compiler.getInventory().getItem(0).isEmpty()) {
                    return 0xFFFFFF;
                }
                ListTag list = compiler.getListTagForKey("compilerCategories");
                ResourceLocation sel = getProgramListForSelectionIndex(list).get(index);

                ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
                return listPrograms.stream()
                    .anyMatch(it -> ResourceLocation.parse(it.asString().orElse("")).equals(sel))
                    ? 0xAAFFAA : 0xFFAAAA;
            }
        };

        programList = new TextListDisplay(this, 80, 30, 8, 104, 5, programTextList);
        programListLarge = new TextListDisplay(this, 8, 30, 8, 104, 5, programTextList);
    }

    @Override
    public void init() {
        super.init();
        catUp = new SmallGuiButton(0, leftPos + 8, topPos + 90, 15, 10, "/\\");
        catUp.setPressListener(b -> categoryList.scrollDown());
        addRenderableWidget(catUp);
        catDn = new SmallGuiButton(1, leftPos + 24, topPos + 90, 15, 10, "\\/");
        catDn.setPressListener(b -> categoryList.scrollUp());
        addRenderableWidget(catDn);
        unlock = new SmallGuiButton(2, leftPos + 40, topPos + 90, 40, 10, "Unlock");
        unlock.setPressListener(b -> {
            if (categoryList.getSelected() != -1) {
                ListTag list = compiler.getListTagForKey("compilerCategories");
                LogisticsProgramCompilerBlockEntity.programByCategory.keySet().stream()
                    .filter(it -> list.stream().noneMatch(nbtBase -> nbtBase.asString().orElse("").equals(it.toString())))
                    .skip(categoryList.getSelected())
                    .findFirst()
                    .ifPresent(it -> MainProxy.sendPacketToServer(
                        PacketHandler.getPacket(CompilerTriggerTaskPacket.class)
                            .setCategory(it)
                            .setType("category")
                            .setTilePos(compiler))
                    );
            }
        });
        addRenderableWidget(unlock);
        progUp = new SmallGuiButton(3, leftPos + 100, topPos + 90, 15, 10, "/\\");
        progUp.setPressListener(b -> {
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                programListLarge.scrollDown();
            } else {
                programList.scrollDown();
            }
        });
        addRenderableWidget(progUp);
        progDn = new SmallGuiButton(4, leftPos + 116, topPos + 90, 15, 10, "\\/");
        progDn.setPressListener(b -> {
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                programListLarge.scrollUp();
            } else {
                programList.scrollUp();
            }
        });
        addRenderableWidget(progDn);
        programmerButton = new SmallGuiButton(5, leftPos + 132, topPos + 90, 40, 10, "Compile");
        programmerButton.setPressListener(b -> {
            int selIndex = programList.getSelected();
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                selIndex = programListLarge.getSelected();
            }
            if (selIndex != -1) {
                ListTag list = compiler.getListTagForKey("compilerCategories");
                ResourceLocation sel = getProgramListForSelectionIndex(list).get(selIndex);
                ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
                boolean flag = listPrograms.stream()
                    .anyMatch(it -> ResourceLocation.parse(it.asString().orElse("")).equals(sel));
                MainProxy.sendPacketToServer(PacketHandler.getPacket(CompilerTriggerTaskPacket.class).setCategory(sel)
                    .setType(flag ? "flash" : "program").setTilePos(compiler));
            }
        });
        addRenderableWidget(programmerButton);

        search = new InputBar(font, this, leftPos + 30, topPos + 11, 120, 16);
        addRenderableWidget(search);
    }

    @Override
    public void closeGui() throws IOException {
        super.closeGui();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
        LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 105);
        LPGuiGraphics.drawSlotDiskBackground(guiGraphics, leftPos + 9, topPos + 9);
        LPGuiGraphics.drawSlotProgrammerBackground(guiGraphics, leftPos + 153, topPos + 9);

        if (compiler.getCurrentTask() != null) {
            guiGraphics.fill(leftPos + 9, topPos + 50, leftPos + 171, topPos + 66, Color.getValue(Color.BLACK));
            guiGraphics.fill(leftPos + 10, topPos + 51, leftPos + 170, topPos + 65, 0xFFFFFFFF);
            guiGraphics.fill(leftPos + 11, topPos + 52, leftPos + 11 + (int) (158 * compiler.getTaskProgress()),
                topPos + 64, Color.getValue(Color.GREEN));

            catUp.visible = false;
            catDn.visible = false;
            unlock.visible = false;
            progUp.visible = false;
            progDn.visible = false;
            programmerButton.visible = false;
        } else {
            catUp.visible = true;
            catDn.visible = true;
            unlock.visible = true;
            progUp.visible = true;
            progDn.visible = true;
            programmerButton.visible = true;
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                catUp.visible = false;
                catDn.visible = false;
                unlock.visible = false;
                programListLarge.renderGuiBackground(guiGraphics, var2, var3);
            } else {
                catUp.visible = true;
                catDn.visible = true;
                unlock.visible = true;
                categoryList.renderGuiBackground(guiGraphics, var2, var3);
                programList.renderGuiBackground(guiGraphics, var2, var3);
            }

            int selIndex = programList.getSelected();
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                selIndex = programListLarge.getSelected();
            }

            if (selIndex != -1) {
                ListTag list = compiler.getListTagForKey("compilerCategories");
                ResourceLocation sel = getProgramListForSelectionIndex(list).get(selIndex);

                ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
                if (listPrograms.stream().anyMatch(it -> ResourceLocation.parse(it.asString().orElse("")).equals(sel))) {
                    programmerButton.setMessage(Component.literal("Flash"));
                    programmerButton.active = !compiler.getInventory().getItem(1).isEmpty();
                } else {
                    programmerButton.setMessage(Component.literal("Compile"));
                    programmerButton.active = true;
                }
            }
        }
    }

    private List<ResourceLocation> getProgramListForSelectionIndex(ListTag list) {
        return list.stream()
            .flatMap(
                nbtBase -> LogisticsProgramCompilerBlockEntity.programByCategory.get(
                        ResourceLocation.parse(nbtBase.asString().orElse("")))
                    .stream())
            .filter(it -> TextUtil.translate(BuiltInRegistries.ITEM.getValue(it).getDescriptionId()).toLowerCase()
                .contains(search.getValue().toLowerCase()))
            .sorted(Comparator.<ResourceLocation, Integer>comparing(o -> getSortingClass(BuiltInRegistries.ITEM.getValue(o)))
                .thenComparing(o -> TextUtil.translate(BuiltInRegistries.ITEM.getValue(o).getDescriptionId()).toLowerCase())
            )
            .collect(Collectors.toList());
    }

    private int getSortingClass(Item object) {
        return switch (object) {
            case ItemLogisticsPipe __ -> 0;
            case ItemModule __ -> 1;
            case ItemUpgrade __ -> 2;
            default -> 10;
        };
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        if (compiler.getCurrentTask() == null) {
            if (!search.handleKey(typedChar, keyCode)) {
                return super.charTyped(typedChar, keyCode);
            }
            return true;
        } else {
            return super.charTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean mouseClicked(double par1, double par2, int par3) {
        if (compiler.getCurrentTask() == null) {
            search.handleClick(par1, par2, par3);
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                programListLarge.mouseClicked(par1, par2, par3);
            } else {
                categoryList.mouseClicked(par1, par2, par3);
                programList.mouseClicked(par1, par2, par3);
            }
        }
        return super.mouseClicked(par1, par2, par3);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (compiler.getCurrentTask() == null) {
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                if (scrollY < 0) {
                    programListLarge.mouseScrollUp();
                } else if (scrollY > 0) {
                    programListLarge.mouseScrollDown();
                }
            } else {
                if (scrollY < 0) {
                    categoryList.mouseScrollUp();
                    programList.mouseScrollUp();
                } else if (scrollY > 0) {
                    categoryList.mouseScrollDown();
                    programList.mouseScrollDown();
                }
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (compiler.getCurrentTask() != null) {
            guiGraphics.drawString(font, TextUtil.translate("gui.compiler.processing"), 10, 39, 0xFF000000, false);
            Item item = BuiltInRegistries.ITEM.getValue(compiler.getCurrentTask());
            String name;
            if (!item.equals(Items.AIR)) {
                name = item.getDescriptionId();
            } else {
                name = "gui.compiler." + compiler.getCurrentTask().toString().replace(':', '.');
            }
            String text = TextUtil.getTrimmedString(TextUtil.translate(name), 160, font, "...");
            guiGraphics.drawString(font, text, 10, 70, 0xFF000000, false);
            if (!compiler.isWasAbleToConsumePower()) {
                guiGraphics.drawString(font, TextUtil.translate("gui.compiler.nopower.1"), 68, 10, 0xFF000000, false);
                guiGraphics.drawString(font, TextUtil.translate("gui.compiler.nopower.2"), 35, 20, 0xFF000000, false);
            }
        } else {
            if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
                programListLarge.renderGuiForeground(guiGraphics);
            } else {
                categoryList.renderGuiForeground(guiGraphics);
                programList.renderGuiForeground(guiGraphics);
            }
        }
    }
}
