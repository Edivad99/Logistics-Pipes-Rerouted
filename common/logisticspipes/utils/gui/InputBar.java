package logisticspipes.utils.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;

public class InputBar extends EditBox {

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    @Setter
    private int minNumber = 0;

    private static final int NUMBER_MAX_LENGTH = 5;

    private final boolean numberOnly;
    private int highlightPos;

    public InputBar(Font font, LogisticsBaseGuiScreen<?> screen, int left, int top, int width, int height) {
        this(font, screen, left, top, width, height, true);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen<?> screen, int left, int top, int width, int height,
        boolean isActive) {
        this(font, screen, left, top, width, height, isActive, false);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen<?> screen, int left, int top, int width, int height,
        boolean isActive, boolean numberOnly) {
        this(font, screen, left, top, width, height, isActive, numberOnly, Align.LEFT);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen<?> screen, int left, int top, int width, int height,
        boolean isActive, boolean numberOnly, Align align) {
        super(font, left + 2, top, width - 4, height - 2, Component.empty());
        screen.registerTextField(this);
        this.numberOnly = numberOnly;
        setMaxLength(numberOnly ? NUMBER_MAX_LENGTH : 128);
    }

    /**
     * 26.2 dropped NeoForge's {@code EditBox#setFilter}; the overrides below stand in for it.
     */
    private boolean accepts(String text) {
        if (!numberOnly) {
            return true;
        }
        try {
            return Integer.parseInt(text) >= minNumber;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public void setValue(String value) {
        if (accepts(value)) {
            super.setValue(value);
        }
    }

    /** {@code EditBox} keeps the selection anchor private and only ever writes it through here. */
    @Override
    public void setHighlightPos(int pos) {
        super.setHighlightPos(pos);
        highlightPos = pos;
    }

    /**
     * Judges the value the insertion would leave behind, not the inserted piece: NeoForge's filter
     * saw only the piece, so with a minimum of 1 the {@code 0} of {@code 10} was refused. An empty
     * result is let through, as backspace can empty the box anyway.
     */
    @Override
    public void insertText(String input) {
        if (!numberOnly) {
            super.insertText(input);
            return;
        }
        String value = getValue();
        int anchor = Math.clamp(highlightPos, 0, value.length());
        int start = Math.min(getCursorPosition(), anchor);
        int end = Math.max(getCursorPosition(), anchor);
        String text = StringUtil.filterText(input);
        // What EditBox will cut the piece down to; nothing is inserted into a full box.
        int room = NUMBER_MAX_LENGTH - value.length() + (end - start);
        if (room <= 0) {
            return;
        }
        if (text.length() > room) {
            text = text.substring(0, room);
        }
        String result = new StringBuilder(value).replace(start, end, text).toString();
        if (result.isEmpty() || accepts(result)) {
            super.insertText(input);
        }
    }

    public void reposition(int left, int top, int width, int height) {
        setX(left + 2);
        setY(top);
        setWidth(width - 4);
        // height set at construction
    }

    /**
     * @return Boolean, true if click was handled.
     */
    public boolean handleClick(double x, double y, int k) {
        boolean inside = isVisible() && x >= getX() && x < getX() + width && y >= getY() && y < getY() + height;
        // Screens that consume the click themselves never let Screen#mouseClicked assign the focus,
        // and without focus the box refuses every typed character, so track it here.
        setFocused(inside);
        if (inside) {
            if (k == InputConstants.MOUSE_BUTTON_RIGHT) {
                setValue("");
            } else {
                // handleClick is LP's own entry point, called from screens that have already
                // consumed the click; EditBox now wants the 1.21.9 event, so build one here.
                mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(k, 0)), false);
            }
        }
        return inside;
    }

    /**
     * @return Boolean, true if key was handled.
     */
    public boolean handleKey(char c, int i) {
        return charTyped(new CharacterEvent(c)); // was: textboxKeyTyped(c, i) in 1.12.2
    }

    public void setInteger(int newValue) {
        setValue(Integer.toString(Math.max(minNumber, newValue)));
    }

    public int getInteger() {
        try {
            return Math.max(minNumber, Integer.parseInt(getValue()));
        } catch (NumberFormatException ignored) {
            return minNumber;
        }
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    @Nullable
    public static EditBox focusedAmong(Iterable<?> candidates) {
        for (Object candidate : candidates) {
            if (candidate instanceof EditBox editBox && editBox.canConsumeInput()) {
                return editBox;
            }
        }
        return null;
    }
}
