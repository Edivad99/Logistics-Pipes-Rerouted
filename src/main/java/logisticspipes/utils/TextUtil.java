package logisticspipes.utils;

import java.text.NumberFormat;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import logisticspipes.Translations;

public final class TextUtil {

    /**
     * Scale and suffix, largest last; see {@link #getThreeDigitFormattedNumber}.
     */
    private static final double[] NUMBER_SCALES = { 1e0, 1e3, 1e6, 1e9, 1e12, 1e15, 1e18 };
    private static final String[] NUMBER_PREFIXES = { "", "k", "M", "G", "T", "P", "E" };

    private TextUtil() {
    }

    public static String getTrimmedString(String text, int maxWidth, Font fontRenderer) {
        return getTrimmedString(text, maxWidth, fontRenderer, "...");
    }

    public static String getTrimmedString(String text, int maxWidth, Font fontRenderer, CharSequence postfix) {
        if (fontRenderer.width(text) < maxWidth) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (fontRenderer.width(result.toString() + character + postfix) >= maxWidth) {
                break;
            }
            result.append(character);
        }
        return result.toString().trim() + postfix;
    }

    /**
     * Turns the long value into a formatted number string following the metric prefixes.
     * The result is limited to 3 digits and to achieve it when the number is above 100 in
     * each scale it will be translated to the next higher scale as so: 0Px where P is the
     * higher prefix and x is the value in the hundreds equivalent to the previous prefix.
     *
     * @param number             to be formatted.
     * @param forceDisplayNumber whether 1 should return an empty string or itself.
     * @return 3 digit string, not constrained but should never exceed it.
     */
    public static String getThreeDigitFormattedNumber(long number, boolean forceDisplayNumber) {
        for (int i = 0; i < NUMBER_SCALES.length; i++) {
            double scale = NUMBER_SCALES[i];
            if (!(number == 0L || (number >= scale * 0.1 && number < scale * 100))) {
                continue;
            }
            if (number == 1L && !forceDisplayNumber) {
                return "";
            }
            if (number < 1000) {
                // Don't touch less than 3 digit values
                return Long.toString(number);
            }
            String head = Integer.toString((int) (number / scale)) + NUMBER_PREFIXES[i];
            if (number > 10 * scale) {
                return head;
            }
            int decimal = (int) ((number % scale) / (scale / 10));
            return decimal > 0 ? head + decimal : head;
        }
        return "NaN";
    }

    /**
     * Appends the tooltip lines to {@code tooltip}. Takes a {@link Consumer} rather than a list
     * since 1.21.5, which is what {@code Item.appendHoverText} now hands out.
     */
    public static void addTooltipInformation(ItemStack stack, Consumer<Component> tooltip, boolean extended) {
        String descriptionId = stack.getItem().getDescriptionId();
        if (extended) {
            int tooltipLine = 1;
            while (Language.getInstance().has(Translations.Tooltip.itemTip(descriptionId, tooltipLine))) {
                tooltip.accept(Component.translatable(Translations.Tooltip.itemTip(descriptionId, tooltipLine))
                    .withStyle(ChatFormatting.GRAY));
                tooltipLine++;
            }
        } else if (Language.getInstance().has(Translations.Tooltip.itemTip(descriptionId, 1))) {
            tooltip.accept(Component.literal("<")
                .append(Component.translatable(Translations.Tooltip.HOLD))
                .append(CommonComponents.SPACE)
                .append(Component.translatable(Translations.Tooltip.SHIFT)
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                .append(CommonComponents.SPACE)
                .append(Component.translatable(Translations.Tooltip.FOR_DETAILS))
                .append(Component.literal(">")).withStyle(ChatFormatting.GRAY));
        }
    }

    public static String formatNumberWithCommas(long number) {
        return NumberFormat.getNumberInstance(Minecraft.getInstance().getLanguageManager().getJavaLocale())
            .format(number);
    }
}
