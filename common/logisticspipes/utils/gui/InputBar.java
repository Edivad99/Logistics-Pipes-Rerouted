package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import lombok.Setter;

public class InputBar extends EditBox implements LogisticsBaseGuiScreen.EventListener {

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    @Setter
    private int minNumber = 0;

    public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height) {
        this(font, screen, left, top, width, height, true);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height,
        boolean isActive) {
        this(font, screen, left, top, width, height, isActive, false);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height,
        boolean isActive, boolean numberOnly) {
        this(font, screen, left, top, width, height, isActive, numberOnly, Align.LEFT);
    }

    public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height,
        boolean isActive, boolean numberOnly, Align align) {
        super(font, left + 2, top, width - 4, height - 2, Component.empty());
        screen.onGuiEvents.add(this);
        if (numberOnly) {
            setFilter((String s) -> {
                try {
                    return Integer.parseInt(s) >= minNumber;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            });
            setMaxLength(5);
        } else {
            setMaxLength(128);
        }
    }

    public void reposition(int left, int top, int width, int height) {
        setX(left + 2);
        setY(top);
        setWidth(width - 4);
        // height set at construction
    }

    @Override
    public void onUpdateScreen() {
        //tick(); // was: updateCursorCounter() in 1.12.2
    }

    @Override
    public boolean onKeyboardInput() {
        return (isFocused() || Minecraft.getInstance().hasAltDown()) && StringUtil.isAllowedChatCharacter(' ');
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
            if (k == 1) {
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
}
