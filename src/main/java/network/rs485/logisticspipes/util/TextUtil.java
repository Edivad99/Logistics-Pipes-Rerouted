/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.util;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class TextUtil {

    private static final String HOLD_SHIFT_TOOLTIP = "misc.holdshift";

    /** Scale and suffix, largest last; see {@link #getThreeDigitFormattedNumber}. */
    private static final double[] NUMBER_SCALES = { 1e0, 1e3, 1e6, 1e9, 1e12, 1e15, 1e18 };
    private static final String[] NUMBER_PREFIXES = { "", "k", "M", "G", "T", "P", "E" };

    private static final Pattern FORMATTING_PATTERN = Pattern.compile(
        java.util.Arrays.stream(ChatFormatting.values())
            .map(formatting -> formatting.getName().toUpperCase(java.util.Locale.ROOT))
            .collect(Collectors.joining("|", "(\\$)(", ")")));

    // Carried between the steps of one transform() call; see the note there.
    private static final EnumSet<ChatFormatting> formattingState = EnumSet.noneOf(ChatFormatting.class);
    private static final EnumSet<ChatFormatting> baseFormattingState = EnumSet.noneOf(ChatFormatting.class);

    private TextUtil() {
    }

    public static String translate(String key, String... args) {
        return translate(key, EnumSet.noneOf(ChatFormatting.class), "", "", args);
    }

    public static String translate(String key, EnumSet<ChatFormatting> baseFormatting, String... args) {
        return translate(key, baseFormatting, "", "", args);
    }

    public static String translate(String key, EnumSet<ChatFormatting> baseFormatting, String prepend, String append,
        String... args) {
        return transform(prepend + I18n.get(key, (Object[]) args) + append, baseFormatting);
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
            while (Language.getInstance().has(descriptionId + ".tip" + tooltipLine)) {
                tooltip.accept(Component.literal(translate(descriptionId + ".tip" + tooltipLine)));
                tooltipLine++;
            }
        } else if (Language.getInstance().has(descriptionId + ".tip1")) {
            tooltip.accept(Component.literal(translate(HOLD_SHIFT_TOOLTIP)));
        }
    }

    /**
     * Logistics Pipes localization files accept special (more descriptive) formatting tags; this
     * method turns them into minecraft font renderer compatible tags. baseFormatting will be
     * preserved over $RESET tags.
     *
     * <p>The two formatting sets are shared rather than local, as they were in the Kotlin object:
     * one transform at a time, on the render thread.
     *
     * @param text           to be formatted
     * @param baseFormatting to be applied at the start of the string and preserved throughout.
     * @return formatted string ready to be rendered by Minecraft's font renderer.
     */
    public static String transform(String text, EnumSet<ChatFormatting> baseFormatting) {
        baseFormattingState.clear();
        baseFormattingState.addAll(baseFormatting);
        formattingState.clear();
        String result = prependIndent(text, colorTag(baseFormattingState) + formattingTags(baseFormattingState));
        while (true) {
            Matcher matcher = FORMATTING_PATTERN.matcher(result);
            if (!matcher.find()) {
                return result;
            }
            StringBuilder replaced = new StringBuilder();
            matcher.reset();
            while (matcher.find()) {
                matcher.appendReplacement(replaced,
                    Matcher.quoteReplacement(replacementString(byName(matcher.group()))));
            }
            matcher.appendTail(replaced);
            result = replaced.toString();
        }
    }

    public static String formatNumberWithCommas(long number) {
        return NumberFormat.getNumberInstance(Minecraft.getInstance().getLanguageManager().getJavaLocale())
            .format(number);
    }

    /** Kotlin's String.prependIndent, which puts the indent in front of every line. */
    private static String prependIndent(String text, String indent) {
        return text.lines().map(line -> indent + line).collect(Collectors.joining("\n"));
    }

    private static ChatFormatting byName(String value) {
        return java.util.Objects.requireNonNull(ChatFormatting.getByName(value.toLowerCase(java.util.Locale.ROOT)));
    }

    private static String replacementString(ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            formattingState.clear();
            return formatting + colorTag(baseFormattingState) + formattingTags(baseFormattingState);
        }
        if (formatting.isColor()) {
            formattingState.removeIf(ChatFormatting::isColor);
        }
        formattingState.add(formatting);
        return colorTag(formattingState) + formattingTags(formattingState);
    }

    private static String colorTag(EnumSet<ChatFormatting> formattings) {
        return formattings.stream().filter(ChatFormatting::isColor).findFirst()
            .or(() -> baseFormattingState.stream().filter(ChatFormatting::isColor).findFirst())
            .map(ChatFormatting::toString)
            .orElse("");
    }

    private static String formattingTags(EnumSet<ChatFormatting> formattings) {
        EnumSet<ChatFormatting> all = EnumSet.copyOf(formattings);
        all.addAll(baseFormattingState);
        return all.stream().filter(ChatFormatting::isFormat).map(ChatFormatting::toString)
            .collect(Collectors.joining());
    }
}
