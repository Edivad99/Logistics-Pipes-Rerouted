package logisticspipes.client.gui.screen;

import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.gui.popup.GuiAddTracking;
import logisticspipes.network.to_server.block.RequestRunningCraftingTasksMessage;
import logisticspipes.network.to_server.block.RequestTrackableItemsMessage;
import logisticspipes.network.to_server.block.TrackItemMessage;
import logisticspipes.util.TrackingTask;
import logisticspipes.utils.Color;
import logisticspipes.utils.TextUtil;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.StringUtils;
import logisticspipes.world.inventory.StatisticsMenu;
import logisticspipes.world.level.block.entity.LogisticsStatisticsBlockEntity;

public class StatisticsScreen extends LogisticsBaseGuiScreen<StatisticsMenu> {

    private final String PREFIX = "gui.networkstatistics.";
    private final TabTracker tabTracker = new TabTracker();
    private final TabCrafting tabCrafting = new TabCrafting();
    private final List<StatisticsTab> tabs = Arrays.asList(tabTracker, tabCrafting);
    private final LogisticsStatisticsBlockEntity tile;
    private int currentTab;
    private int prevMouseDragX;
    private int prevMouseDragY;

    public StatisticsScreen(StatisticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 180, 220, 0, 0);
        this.tile = menu.getBlockEntity();
    }

    @Override
    public void init() {

        super.init();

        tabs.forEach(StatisticsTab::init);

        tabTracker.updateItemList();
    }

    @Override
    public void closeGui() throws IOException {
        super.closeGui();

    }

    @Override
    public void resetSubGui() {
        super.resetSubGui();
        tabTracker.updateItemList();
    }

    private StatisticsTab getActiveTab() {
        return tabs.get(currentTab);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int clickedMouseButton = event.button();
        getActiveTab().onMouseDrag((int) mouseX, (int) mouseY, (int) (mouseX - prevMouseDragX),
            (int) (mouseY - prevMouseDragY));
        prevMouseDragX = (int) mouseX;
        prevMouseDragY = (int) mouseY;
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        getActiveTab().onMouseScroll((int) scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float f) {
        drawBG(guiGraphics);
        getActiveTab().draw(guiGraphics, mouseX, mouseY);

        super.extractGuiBackground(guiGraphics, mouseX, mouseY, f);
    }

    private void drawBG(GuiGraphicsExtractor guiGraphics) {
        // background
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos + 20, right, bottom, 0.0f, true);
        LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos + (25 * currentTab) + 2, topPos - 2,
            leftPos + 27 + (25 * currentTab), topPos + 38, 0.0f, true, true, true, false, true);

        // tab selector panes
        for (int i = 0; i < tabs.size(); i++) {
            LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos + (25 * i) + 2, topPos - 2, leftPos + 27 + (25 * i),
                topPos + 35, 0.0f, false, true, true, false, true);
        }

        // Tab icons, one per selector pane: the stats sheet, then the crafting table the second
        // tab reports on.
        LPGuiGraphics.drawStatsBackground(guiGraphics, leftPos + 6, topPos + 3);
        guiGraphics.item(new ItemStack(Items.CRAFTING_TABLE), leftPos + 31, topPos + 3);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
        getActiveTab().charTyped(c, i);
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();
        prevMouseDragX = (int) mouseX;
        prevMouseDragY = (int) mouseY;

        if (mouseButton == 0 && mouseX > leftPos && mouseX < leftPos + 220 && mouseY > topPos && mouseY < topPos + 20) {
            double tabX = mouseX - leftPos - 3;
            currentTab = max(0, min((int) (tabX / 25), tabs.size() - 1));
        } else {
            getActiveTab().handleClick((int) mouseX, (int) mouseY, mouseButton);
            return super.mouseClicked(event, doubleClick);
        }
        return true;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        getActiveTab().drawForegroundLayer(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void checkButtons() {
        super.checkButtons();
        tabs.forEach(StatisticsTab::checkButtons);
    }

    public void handleTrackableItems(List<ItemIdentifierStack> items) {
        tabTracker.handlePacket(items);
    }

    /** The tracked items or their recorded history changed while the screen was open. */
    public void handleTrackingTasks() {
        tabTracker.updateItemList();
    }

    public void handleRunningCraftingTasks(List<ItemIdentifierStack> tasks) {
        tabCrafting.handlePacket(tasks);
    }

    private interface StatisticsTab {

        void init();

        void draw(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY);

        default void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        }

        default void checkButtons() {
        }

        default void charTyped(char c, int i) {
        }

        default void handleClick(int mouseX, int mouseY, int mouseButton) {
        }

        default void onMouseDrag(int x, int y, int dx, int dy) {
        }

        default void onMouseScroll(int dw) {
        }

    }

    private class TabTracker implements StatisticsTab {

        private static final int GRAPH_WIDTH = 150;
        private static final int GRAPH_HEIGHT = 80;
        /** Where the value zero sits before panning, as an offset from the horizontal axis. */
        private static final float Y_VIEWPORT_CENTER = 40;
        private static final float X_VIEWPORT_CENTER = 75;

        private final List<AbstractButton> BUTTONS = new ArrayList<>();
        private final List<String> graphTexts = new ArrayList<>();
        private final List<int[]> graphTextPos = new ArrayList<>();
        private @Nullable ItemDisplay itemDisplay;
        private float xViewportOffset = -1434;
        private float yViewportOffset;
        private float xViewportScale = 15;
        private float yViewportScale = 15;
        private boolean isDraggingGraph = false;
        private boolean isDraggingXBar = false;
        private boolean isDraggingYBar = false;
        // Buffered text labels populated in draw(), drawn in drawForegroundLayer()
        private @Nullable String taskNameLabel = null;
        // What the vertical scale was fitted to, so panning and zooming survive a redraw
        private @Nullable ItemIdentifier fittedItem;
        private long fittedMax = -1;

        @Override
        public void init() {
            SmallGuiButton b0 = new SmallGuiButton(0, leftPos + 10, topPos + 70, 20, 20, "<");
            b0.setPressListener(b -> itemDisplay.prevPage());
            BUTTONS.add(addRenderableWidget(b0));
            SmallGuiButton b1 = new SmallGuiButton(1, leftPos + 150, topPos + 70, 20, 20, ">");
            b1.setPressListener(b -> itemDisplay.nextPage());
            BUTTONS.add(addRenderableWidget(b1));
            SmallGuiButton b2 = new SmallGuiButton(2, leftPos + 37, topPos + 70, 40, 20, "Add");
            b2.setPressListener(b -> ClientPacketDistributor.sendToServer(
                new RequestTrackableItemsMessage(tile.getBlockPos())));
            BUTTONS.add(addRenderableWidget(b2));
            SmallGuiButton b3 = new SmallGuiButton(3, leftPos + 83, topPos + 70, 60, 20, "Remove");
            b3.setPressListener(b -> {
                if (itemDisplay.getSelectedItem() != null) {
                    ClientPacketDistributor.sendToServer(new TrackItemMessage(
                        tile.getBlockPos(), itemDisplay.getSelectedItem().getItem(), false));
                    Iterator<TrackingTask> iter = tile.tasks.iterator();
                    while (iter.hasNext()) {
                        TrackingTask task = iter.next();
                        if (task.item == itemDisplay.getSelectedItem().getItem()) {
                            iter.remove();
                            break;
                        }
                    }
                    updateItemList();
                }
            });
            BUTTONS.add(addRenderableWidget(b3));

            if (itemDisplay == null) {
                itemDisplay = new ItemDisplay(null, font, StatisticsScreen.this, null, leftPos + 10, topPos + 18,
                    panelWidth - 20, panelHeight - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
            }
            itemDisplay.reposition(leftPos + 10, topPos + 40, panelWidth - 20, 20, 0, 0);
        }

        @Override
        public void draw(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
            taskNameLabel = null;
            graphTexts.clear();
            graphTextPos.clear();
            itemDisplay.renderItemArea(guiGraphics, 0.0f);
            itemDisplay.renderPageNumber(guiGraphics, right - 40, topPos + 28);
            if (itemDisplay.getSelectedItem() != null) {
                TrackingTask task = getSelectedTask();

                if (task != null) {
                    long recordedMax = maxRecorded(task);
                    if (task.item != fittedItem || recordedMax > fittedMax) {
                        fitVertically(recordedMax);
                        fittedItem = task.item;
                        fittedMax = recordedMax;
                    }
                    LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 10, topPos + 99);
                    guiGraphics.item(task.item.makeNormalStack(1), leftPos + 12, topPos + 101);
                    taskNameLabel = StringUtils.getWithMaxWidth(task.item.getFriendlyName(), 136, font);

                    int xOrigo = xCenter - 72;
                    int yOrigo = yCenter + 90;

                    drawLine(guiGraphics, xOrigo, yOrigo, xOrigo + GRAPH_WIDTH, yOrigo, Color.DARKER_GREY);
                    drawLine(guiGraphics, xOrigo, yOrigo, xOrigo, yOrigo - GRAPH_HEIGHT, Color.DARKER_GREY);

                    drawLine(guiGraphics, xOrigo - 4, yOrigo - GRAPH_HEIGHT, xOrigo, yOrigo - GRAPH_HEIGHT, Color.DARKER_GREY);

                    drawLine(guiGraphics, xOrigo + GRAPH_WIDTH, yOrigo - 1, xOrigo + GRAPH_WIDTH, yOrigo + 4, Color.DARKER_GREY);

                    long[] data = getTaskData(task);

                    Set<Integer> labeledYPixels = new HashSet<>();
                    int rightLimit = 2; // we want to draw one more graph part past the right edge
                    for (int i = 0; i < data.length; i++) {
                        rightLimit--;
                        if (rightLimit == 0) {
                            break;
                        }

                        float x = i;
                        float y = data[i];
                        float prevX = x;
                        float prevY = y;
                        if (i > 0) {
                            prevX = x - 1;
                            prevY = data[i - 1];
                        }

                        x += xViewportOffset;
                        x *= xViewportScale;
                        x += X_VIEWPORT_CENTER;
                        prevX += xViewportOffset;
                        prevX *= xViewportScale;
                        prevX += X_VIEWPORT_CENTER;

                        y -= yViewportOffset;
                        y *= yViewportScale;
                        y += Y_VIEWPORT_CENTER;
                        prevY -= yViewportOffset;
                        prevY *= yViewportScale;
                        prevY += Y_VIEWPORT_CENTER;

                        if (x <= GRAPH_WIDTH) {
                            rightLimit = 2;
                        }
                        if (x < 0) {
                            continue;
                        }

                        if (x <= GRAPH_WIDTH) {
                            int interval = max(1, (int) (40 / xViewportScale) + 1);
                            if (i % interval == 0) {
                                String s = formatTime(data.length - i - 1);
                                int w = minecraft.font.width(s);
                                drawLine(guiGraphics, xOrigo + (int) x, yOrigo - 1, xOrigo + (int) x, yOrigo + 4,
                                    Color.DARKER_GREY);
                                graphTexts.add(s);
                                graphTextPos.add(
                                    new int[] { (int) (xOrigo - leftPos + (int) x - w / 2f), yOrigo - topPos + 6,
                                        Color.DARKER_GREY.getValue() });
                            }
                        }

                        if (y >= 0 && y < GRAPH_HEIGHT) {
                            drawLine(guiGraphics, xOrigo - 4, yOrigo - (int) y, xOrigo, yOrigo - (int) y,
                                Color.DARKER_GREY);
                            int yPixel = (int) y;
                            boolean tooClose = false;
                            for (int labeled : labeledYPixels) {
                                if (Math.abs(labeled - yPixel) < 10) {
                                    tooClose = true;
                                    break;
                                }
                            }
                            if (!tooClose) {
                                labeledYPixels.add(yPixel);
                                String label = Long.toString(data[i]);
                                int lw = minecraft.font.width(label);
                                graphTexts.add(label);
                                graphTextPos.add(new int[] { xOrigo - leftPos - 5 - lw, yOrigo - topPos - yPixel - 4,
                                    Color.DARKER_GREY.getValue() });
                            }
                        }

                        drawGraphPart(guiGraphics, xOrigo, yOrigo, (int) prevX, (int) prevY, (int) x, (int) y);
                    }
                }
            }
        }

        @Nullable
        private TrackingTask getSelectedTask() {
            for (TrackingTask taskLoop : tile.tasks) {
                if (taskLoop.item == itemDisplay.getSelectedItem().getItem()) {
                    return taskLoop;
                }
            }
            return null;
        }

        /**
         * Scales the graph so the largest amount recorded sits just below the top of the frame.
         *
         * <p>A fixed scale puts anything past five items off screen, which leaves the line where
         * it cannot be seen until the view is dragged back to it by hand. Refitting happens when
         * another item is picked and when a new high arrives, so a zoom set by hand survives the
         * samples in between.
         */
        private long maxRecorded(TrackingTask task) {
            long max = 0;
            for (long amount : task.amountRecorded) {
                max = max(max, amount);
            }
            return max;
        }

        private void fitVertically(long max) {
            yViewportScale = max > 0 ? GRAPH_HEIGHT * 0.875f / max : 15;
            // Places the zero line on the horizontal axis rather than halfway up the frame.
            yViewportOffset = Y_VIEWPORT_CENTER / yViewportScale;
        }

        private long[] getTaskData(TrackingTask task) {
            long[] data = new long[task.amountRecorded.length];
            System.arraycopy(task.amountRecorded, task.arrayPos, data, 0, task.amountRecorded.length - task.arrayPos);
            System.arraycopy(task.amountRecorded, 0, data, task.amountRecorded.length - task.arrayPos, task.arrayPos);
            return data;
        }

        @Override
        public void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
            guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "amount"), 10, 28,
                Color.getValue(Color.DARKER_GREY), false);
            if (taskNameLabel != null) {
                guiGraphics.text(minecraft.font, taskNameLabel, 32, 104, Color.getValue(Color.DARKER_GREY), false);
            }
            for (int i = 0; i < graphTexts.size(); i++) {
                int[] pos = graphTextPos.get(i);
                guiGraphics.text(minecraft.font, graphTexts.get(i), pos[0], pos[1], pos[2], false);
            }
        }

        @Override
        public void charTyped(char c, int i) {
            if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { //PgUp
                itemDisplay.prevPage();
            } else if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { //PgDn
                itemDisplay.nextPage();
            }
        }

        @Override
        public void handleClick(int mouseX, int mouseY, int mouseButton) {
            if (itemDisplay.handleClick(mouseX, mouseY, mouseButton)) {
                xViewportOffset = max(-1439, min(xViewportOffset, 0));
            }

            int xOrigo = xCenter - 72;
            int yOrigo = yCenter + 90;
            isDraggingGraph =
                mouseButton == 0 && mouseX > xOrigo && mouseX < xOrigo + GRAPH_WIDTH && mouseY < yOrigo && mouseY > yOrigo - GRAPH_HEIGHT;
            isDraggingXBar = mouseButton == 0 && mouseX > xOrigo && mouseX < xOrigo + GRAPH_WIDTH && mouseY < yOrigo + 16
                && mouseY > yOrigo + 4;
            isDraggingYBar = mouseButton == 0 && mouseX > xOrigo - 16 && mouseX < xOrigo - 4 && mouseY < yOrigo
                && mouseY > yOrigo - GRAPH_HEIGHT;
        }

        @Override
        public void checkButtons() {
            for (AbstractButton button : BUTTONS) {
                button.visible = getActiveTab() == this;
                if (button.getMessage().getString().equals("Remove")) {
                    button.active = itemDisplay.getSelectedItem() != null;
                }
            }
        }

        @Override
        public void onMouseDrag(int x, int y, int dx, int dy) {
            if (isDraggingGraph) {
                xViewportOffset += dx / xViewportScale;
                yViewportOffset += dy / yViewportScale;
            } else if (isDraggingXBar) {
                float mul = (float) pow(1.25, dx / 2f);
                xViewportScale *= mul;
            } else if (isDraggingYBar) {
                float mul = (float) pow(1.25, -dy / 2f);
                yViewportScale *= mul;
            }
        }

        @Override
        public void onMouseScroll(int dw) {
            float mul = (float) pow(1.25, dw / 60f);
            xViewportScale *= mul;
            yViewportScale *= mul;
        }

        private void drawGraphPart(GuiGraphicsExtractor guiGraphics, int xOrigo, int yOrigo, int prevX, int prevY,
            int x, int y) {
            Vector2f left = new Vector2f(prevX, prevY);
            Vector2f right = new Vector2f(x, y);

            // bounds check
            {
                Vector2f min = new Vector2f(left.x, min(left.y, right.y));
                Vector2f max = new Vector2f(right.x, max(left.y, right.y));

                if (!(min.x < GRAPH_WIDTH && max.x > 0 && min.y < GRAPH_HEIGHT && max.y > 0)) {
                    return;
                }
            }

            // clamp to the edges of the graph
            right = clampCorner(left, right, new Vector2f(), true);
            right = clampCorner(left, right, new Vector2f(GRAPH_WIDTH, GRAPH_HEIGHT), false);
            left = clampCorner(right, left, new Vector2f(), true);
            left = clampCorner(right, left, new Vector2f(GRAPH_WIDTH, GRAPH_HEIGHT), false);

            drawLine(guiGraphics, xOrigo + (int) left.x, yOrigo - (int) left.y, xOrigo + (int) right.x,
                yOrigo - (int) right.y, Color.RED);

            int radius = 2;
            if (xViewportScale < 4) {
                radius = 1;
            }

            if (prevX >= 0 && prevX <= GRAPH_WIDTH && prevY >= 0 && prevY <= GRAPH_HEIGHT) {
                guiGraphics.fill(xOrigo + prevX - radius + 1, yOrigo - prevY - radius + 1, xOrigo + prevX + radius,
                    yOrigo - prevY + radius, Color.getValue(Color.BLACK));
            }

            if (x >= 0 && x <= GRAPH_WIDTH && y >= 0 && y <= GRAPH_HEIGHT) {
                guiGraphics.fill(xOrigo + x - radius + 1, yOrigo - y - radius + 1, xOrigo + x + radius,
                    yOrigo - y + radius, Color.getValue(Color.BLACK));
            }
        }

        /** Every clamp returns a fresh vector: JOML's are mutable, where the arguments must not be. */
        private Vector2f clampYPlane(Vector2fc v, Vector2fc toClamp, float x0, boolean greater) {
            if (toClamp.x() == x0) {
                return new Vector2f(toClamp);
            }
            if (toClamp.x() == v.x()) {
                return new Vector2f(toClamp);
            }

            if ((!greater && toClamp.x() < x0) || (greater && toClamp.x() > x0)) {
                return new Vector2f(toClamp);
            }

            Vector2f dir = toClamp.sub(v, new Vector2f());
            dir.div(dir.x); // let dir.x=1 but keep vector's direction
            float dist = (x0 - toClamp.x());
            return dir.mul(dist).add(toClamp);
        }

        @SuppressWarnings("SuspiciousNameCombination")
        private Vector2f clampXPlane(Vector2fc from, Vector2fc to, float y0, boolean greater) {
            final Vector2f swapped = clampYPlane(new Vector2f(from.y(), from.x()), new Vector2f(to.y(), to.x()), y0,
                greater);
            return new Vector2f(swapped.y, swapped.x);
        }

        private Vector2f clampCorner(Vector2fc from, Vector2fc to, Vector2fc corner, boolean greater) {
            return clampXPlane(from, clampYPlane(from, to, corner.x(), greater), corner.y(), greater);
        }

        private String formatTime(int minutes) {
            if (minutes == 0) {
                return "Now";
            }

            int mins = minutes % 60;
            minutes /= 60;
            int hours = minutes;

            StringBuilder sb = new StringBuilder();

            if (hours > 0) {
                sb.append(hours).append("h");
            }
            if (mins > 0) {
                sb.append(mins).append("min");
            }

            return sb.toString();
        }

        public void updateItemList() {
            List<ItemIdentifierStack> allItems = tile.tasks.stream().map(task -> task.item.makeStack(1))
                .collect(Collectors.toList());
            itemDisplay.setItemList(allItems);
        }

        public void handlePacket(List<ItemIdentifierStack> identList) {
            if (hasSubGui() && getSubGui() instanceof GuiAddTracking) {
                ((GuiAddTracking) getSubGui()).handlePacket(identList);
            } else if (!hasSubGui()) {
                GuiAddTracking sub = new GuiAddTracking(tile);
                setSubGui(sub);
                sub.handlePacket(identList);
            }
        }

    }

    private class TabCrafting implements StatisticsTab {

        private final List<AbstractButton> BUTTONS = new ArrayList<>();
        private @Nullable ItemDisplay itemDisplay;

        @Override
        public void init() {
            SmallGuiButton b6 = new SmallGuiButton(6, leftPos + 10, topPos + 40, 160, 20,
                TextUtil.translate(PREFIX + "gettasks"));
            b6.setPressListener(b -> ClientPacketDistributor.sendToServer(
                new RequestRunningCraftingTasksMessage(tile.getBlockPos())));
            BUTTONS.add(addRenderableWidget(b6));
            SmallGuiButton b7 = new SmallGuiButton(7, leftPos + 90, topPos + 65, 10, 10, "<");
            b7.setPressListener(b -> itemDisplay.prevPage());
            BUTTONS.add(addRenderableWidget(b7));
            SmallGuiButton b8 = new SmallGuiButton(8, leftPos + 160, topPos + 65, 10, 10, ">");
            b8.setPressListener(b -> itemDisplay.nextPage());
            BUTTONS.add(addRenderableWidget(b8));

            if (itemDisplay == null) {
                itemDisplay = new ItemDisplay(null, font, StatisticsScreen.this, null, leftPos + 10, topPos + 18,
                    panelWidth - 20, panelHeight - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
                itemDisplay.setItemList(new ArrayList<>());
            }
            itemDisplay.reposition(leftPos + 10, topPos + 80, panelWidth - 20, 125, 0, 0);

        }

        @Override
        public void draw(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
            itemDisplay.renderItemArea(guiGraphics, 0.0f);
            itemDisplay.renderPageNumber(guiGraphics, right - 50, topPos + 66);
        }

        @Override
        public void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
            guiGraphics.text(minecraft.font, TextUtil.translate(PREFIX + "crafting"), 10, 28,
                Color.getValue(Color.DARKER_GREY), false);
            // Item tooltip omitted — tab has no hovered-item lookup at this point
        }

        @Override
        public void charTyped(char c, int i) {
            if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { //PgUp
                itemDisplay.prevPage();
            } else if (i == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { //PgDn
                itemDisplay.nextPage();
            }
        }

        @Override
        public void handleClick(int mouseX, int mouseY, int mouseButton) {
            itemDisplay.handleClick(mouseX, mouseY, mouseButton);
        }

        @Override
        public void checkButtons() {
            for (AbstractButton button : BUTTONS) {
                button.visible = getActiveTab() == this;
            }
        }

        public void handlePacket(List<ItemIdentifierStack> identList) {
            itemDisplay.setItemList(identList);
        }

    }

}
