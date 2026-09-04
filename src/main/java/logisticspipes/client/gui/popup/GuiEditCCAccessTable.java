package logisticspipes.client.gui.popup;

import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.to_server.security.SetSecurityStationCCIdMessage;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiEditCCAccessTable extends SubGuiScreen {

    private static final String PREFIX = "gui.securitystation.popup.ccAccess.";
    private static final int searchWidth = 55;
    private final LogisticsSecurityTileEntity tile;
    private String searchInput1 = "0";
    private String searchInput2 = "";
    private boolean editSearch = false;
    private boolean editSearchB = false;
    private boolean displayCursor = true;
    private long oldSystemTime = 0;
    private int lastClickedX = 0;
    private int lastClickedY = 0;
    private int lastClickedK = 0;
    private boolean clickWasButton = false;
    private int page = 0;

    public GuiEditCCAccessTable(LogisticsSecurityTileEntity tile) {
        super(150, 150, 0, 0);
        this.tile = tile;
    }

    @Override
    public void init() {
        super.init();
        SmallGuiButton minus = new SmallGuiButton(0, guiLeft + 10, guiTop + 119, 30, 20, "-");
        minus.setPressListener(b -> handleBtn(0));
        addRenderableWidget(minus);
        SmallGuiButton plus = new SmallGuiButton(1, guiLeft + 110, guiTop + 119, 30, 20, "+");
        plus.setPressListener(b -> handleBtn(1));
        addRenderableWidget(plus);
        SmallGuiButton rm = new SmallGuiButton(2, guiLeft + 30, guiTop + 107, 40, 10,
            TextUtil.translate(GuiEditCCAccessTable.PREFIX + "Remove"));
        rm.setPressListener(b -> handleBtn(2));
        addRenderableWidget(rm);
        SmallGuiButton add = new SmallGuiButton(3, guiLeft + 80, guiTop + 107, 40, 10,
            TextUtil.translate(GuiEditCCAccessTable.PREFIX + "Add"));
        add.setPressListener(b -> handleBtn(3));
        addRenderableWidget(add);
        SmallGuiButton prev = new SmallGuiButton(4, guiLeft + 87, guiTop + 4, 10, 10, "<");
        prev.setPressListener(b -> handleBtn(4));
        addRenderableWidget(prev);
        SmallGuiButton next = new SmallGuiButton(5, guiLeft + 130, guiTop + 4, 10, 10, ">");
        next.setPressListener(b -> handleBtn(5));
        addRenderableWidget(next);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        guiGraphics.text(minecraft.font, "(" + (page + 1) + "/" + ((int) ((tile.excludedCC.size() / 9D) + 1 - (
                tile.excludedCC.size() % 9 == 0 && tile.excludedCC.size() != 0 ? 1 : 0))) + ")", guiLeft + 100, guiTop + 5,
            0xFF4F4F4F, false);

        boolean dark = true;
        for (int i = 0; i < 9; i++) {
            guiGraphics.fill(guiLeft + 10, guiTop + 15 + (i * 10), right - 10, guiTop + 25 + (i * 10),
                dark ? Color.getValue(Color.DARKER_GREY) : Color.getValue(Color.LIGHTER_GREY));
            dark = !dark;
        }
        dark = true;
        for (int i = 0; i < 9 && i + (page * 9) < tile.excludedCC.size(); i++) {
            Integer id = tile.excludedCC.get(i + (page * 9));
            guiGraphics.text(minecraft.font, Integer.toString(id),
                guiLeft + 75 - (minecraft.font.width(Integer.toString(id)) / 2), guiTop + 16 + (i * 10),
                dark ? 0xFFFFFFFF : 0xFF000000, false);
            dark = !dark;
            if (lastClickedX >= guiLeft + 10 && lastClickedX < right - 10 && lastClickedY >= guiTop + 15 + (i * 10)
                && lastClickedY < guiTop + 25 + (i * 10)) {
                lastClickedX = -10000000;
                lastClickedY = -10000000;
                searchInput1 = Integer.toString(id);
                searchInput2 = "";
            }
        }

        //SearchInput
        if (editSearch) {
            guiGraphics.fill(guiLeft + 40, bottom - 30, right - 40, bottom - 13, Color.getValue(Color.BLACK));
            guiGraphics.fill(guiLeft + 41, bottom - 29, right - 41, bottom - 14, Color.getValue(Color.WHITE));
        } else {
            guiGraphics.fill(guiLeft + 41, bottom - 29, right - 41, bottom - 14, Color.getValue(Color.BLACK));
        }
        guiGraphics.fill(guiLeft + 42, bottom - 28, right - 42, bottom - 15, Color.getValue(Color.DARKER_GREY));

        guiGraphics.text(minecraft.font, searchInput1 + searchInput2,
            guiLeft + 75 - (minecraft.font.width(searchInput1 + searchInput2) / 2), bottom - 25, 0xFFFFFFFF, false);
        if (editSearch) {
            int lineX =
                guiLeft + 75 + minecraft.font.width(searchInput1) - (minecraft.font.width(searchInput1 + searchInput2)
                    / 2);
            if (System.currentTimeMillis() - oldSystemTime > 500) {
                displayCursor = !displayCursor;
                oldSystemTime = System.currentTimeMillis();
            }
            if (displayCursor) {
                guiGraphics.fill(lineX, bottom - 27, lineX + 1, bottom - 16, Color.getValue(Color.WHITE));
            }
        }

        //Click into search
        if (lastClickedX != -10000000 && lastClickedY != -10000000) {
            if (lastClickedX >= guiLeft + 42 && lastClickedX < right - 42 && lastClickedY >= bottom - 30
                && lastClickedY < bottom - 13) {
                editSearch = true;
                if (searchInput1.equals("0") && searchInput2.isEmpty()) {
                    searchInput1 = "";
                }
                lastClickedX = -10000000;
                lastClickedY = -10000000;
                if (lastClickedK == 1) {
                    searchInput1 = "0";
                    searchInput2 = "";
                }
            } else {
                editSearch = false;
                if (searchInput1.length() == 0 && searchInput2.length() == 0) {
                    searchInput1 = "0";
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double i = event.x();
        double j = event.y();
        int k = event.button();
        clickWasButton = false;
        editSearchB = true;
        boolean result = super.mouseClicked(event, doubleClick);
        if ((!clickWasButton && i >= guiLeft + 10 && i < right - 10 && j >= guiTop + 18 && j < bottom - 10)
            || editSearch) {
            if (!editSearchB) {
                editSearch = false;
            }
            lastClickedX = (int) i;
            lastClickedY = (int) j;
            lastClickedK = k;
        }
        return result;
    }

    private void handleBtn(int id) {
        if (editSearch) {
            editSearchB = false;
        }
        clickWasButton = true;
        switch (id) {
            case 0:
                if ((searchInput1 + searchInput2).equals("")) {
                    searchInput1 = "0";
                    break;
                }
                try {
                    int number = Integer.valueOf(searchInput1 + searchInput2);
                    number--;
                    if (number < 0) {
                        number = 0;
                    }
                    searchInput1 = Integer.toString(number);
                    searchInput2 = "";
                } catch (Exception e) {
                    e.printStackTrace();
                    searchInput1 = "0";
                    searchInput2 = "";
                }
                break;
            case 1:
                if ((searchInput1 + searchInput2).equals("")) {
                    searchInput1 = "1";
                    break;
                }
                try {
                    int number = Integer.valueOf(searchInput1 + searchInput2);
                    number++;
                    if (minecraft.font.width(Integer.toString(number)) <= GuiEditCCAccessTable.searchWidth) {
                        searchInput1 = Integer.toString(number);
                        searchInput2 = "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    searchInput1 = "0";
                    searchInput2 = "";
                }
                break;
            case 2: {
                Integer id1 = Integer.valueOf(searchInput1 + searchInput2);
                tile.excludedCC.remove(id1);
                ClientPacketDistributor.sendToServer(
                    new SetSecurityStationCCIdMessage(tile.getBlockPos(), id1, false));
            }
            break;
            case 3: {
                Integer id2 = Integer.valueOf(searchInput1 + searchInput2);
                if (!tile.excludedCC.contains(id2)) {
                    tile.excludedCC.add(id2);
                    Collections.sort(tile.excludedCC);
                }
                ClientPacketDistributor.sendToServer(
                    new SetSecurityStationCCIdMessage(tile.getBlockPos(), id2, true));
            }
            break;
            case 4:
                page--;
                if (page < 0) {
                    page = 0;
                }
                break;
            case 5:
                page++;
                if (page > (tile.excludedCC.size() / 9) - (
                    tile.excludedCC.size() % 9 == 0 && tile.excludedCC.size() != 0 ? 1 : 0)) {
                    page =
                        (tile.excludedCC.size() / 9) - (tile.excludedCC.size() % 9 == 0 && tile.excludedCC.size() != 0 ?
                            1 :
                            0);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
        if (editSearch) {
            if (c == 13) {
                editSearch = false;
                return true;
            } else if (i == 47 && Minecraft.getInstance().hasControlDown()) {
                try {
                    String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    Integer.valueOf(clip);
                    searchInput1 = searchInput1 + clip;
                } catch (Exception e) {
                    setSubGui(new GuiMessagePopup("Clipboard doesn't", "contain a number."));
                }
            } else if (c == 8) {
                if (searchInput1.length() > 0) {
                    searchInput1 = searchInput1.substring(0, searchInput1.length() - 1);
                }
                return true;
            } else if (Character.isDigit(c)) {
                if (minecraft.font.width(searchInput1 + c + searchInput2) <= GuiEditCCAccessTable.searchWidth) {
                    searchInput1 += c;
                }
                return true;
            } else if (i == 203) { //Left
                if (searchInput1.length() > 0) {
                    searchInput2 = searchInput1.substring(searchInput1.length() - 1) + searchInput2;
                    searchInput1 = searchInput1.substring(0, searchInput1.length() - 1);
                }
            } else if (i == 205) { //Right
                if (searchInput2.length() > 0) {
                    searchInput1 += searchInput2.substring(0, 1);
                    searchInput2 = searchInput2.substring(1);
                }
            } else if (i == 1) { //ESC
                editSearch = false;
            } else if (i == 28) { //Enter
                editSearch = false;
            } else if (i == 199) { //Pos
                searchInput2 = searchInput1 + searchInput2;
                searchInput1 = "";
            } else if (i == 207) { //Ende
                searchInput1 = searchInput1 + searchInput2;
                searchInput2 = "";
            } else if (i == 211) { //Entf
                if (searchInput2.length() > 0) {
                    searchInput2 = searchInput2.substring(1);
                }
            }
        } else {
            return super.charTyped(event);
        }
        return false;
    }

    public void fillColor(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, Color color) {
        guiGraphics.fill(x1, y1, x2, y2, Color.getValue(color));
    }
}
