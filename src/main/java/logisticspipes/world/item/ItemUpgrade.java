package logisticspipes.world.item;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import org.jspecify.annotations.Nullable;

import logisticspipes.Translations;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.utils.TextUtil;

public class ItemUpgrade extends LogisticsItem {

    //Values
    public static final int MAX_LIQUID_CRAFTER = 3;
    public static final int MAX_CRAFTING_CLEANUP = 4;
    public static final int MAX_ITEM_EXTRACTION = 8;
    public static final int MAX_ITEM_STACK_EXTRACTION = 8;
    private final Upgrade upgradeType;

    private ItemUpgrade(Upgrade upgradeType, Properties properties) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    /**
     * Factory for use with DeferredRegister.
     */
    public static ItemUpgrade of(Supplier<? extends IPipeUpgrade> upgradeConstructor, Properties properties) {
        return new ItemUpgrade(new Upgrade(upgradeConstructor), properties);
    }

    public static Item getAndCheckUpgrade(Identifier resource) {
        Objects.requireNonNull(resource, "Resource for upgrade is null. Was the upgrade registered?");
        return Objects.requireNonNull(BuiltInRegistries.ITEM.getValue(resource),
            "Upgrade " + resource + " not found in Item registry");
    }

    public @Nullable IPipeUpgrade getUpgradeForItem(ItemStack itemStack, @Nullable IPipeUpgrade currentUpgrade) {
        if (itemStack.isEmpty()) {
            return null;
        }
        if (itemStack.getItem() != this) {
            return null;
        }
        if (upgradeType.getIPipeUpgradeClass() == null) {
            return null;
        }
        if (currentUpgrade != null) {
            if (upgradeType.getIPipeUpgradeClass().equals(currentUpgrade.getClass())) {
                return currentUpgrade;
            }
        }
        IPipeUpgrade newUpgrade = upgradeType.getIPipeUpgrade();
        return newUpgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        IPipeUpgrade upgrade = getUpgradeForItem(stack, null);
        if (upgrade == null) {
            return;
        }
        List<String> pipe = Arrays.asList(upgrade.getAllowedPipes());
        List<String> module = Arrays.asList(upgrade.getAllowedModules());
        if (pipe.isEmpty() && module.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().hasShiftDown()) {
            if (!pipe.isEmpty() && !module.isEmpty()) {
                tooltipAdder.accept(targetLine(Translations.Tooltip.UPGRADE_PIPES, pipe));
                tooltipAdder.accept(targetLine(Translations.Tooltip.UPGRADE_AND_MODULES, module));
            } else if (!pipe.isEmpty()) {
                tooltipAdder.accept(targetLine(Translations.Tooltip.UPGRADE_PIPES, pipe));
            } else {
                tooltipAdder.accept(targetLine(Translations.Tooltip.UPGRADE_MODULES, module));
            }
        } else {
            TextUtil.addTooltipInformation(stack, tooltipAdder, false);
        }
    }

    private static Component targetLine(String key, List<String> targets) {
        return Component.translatable(key, join(targets).withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY);
    }

    /** "a", "a and b", "a, b and c". */
    private static MutableComponent join(List<String> targets) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < targets.size() - 2; i++) {
            result.append(Component.translatable(Translations.Tooltip.upgradeTarget(targets.get(i)))).append(", ");
        }
        Component last = Component.translatable(Translations.Tooltip.upgradeTarget(targets.getLast()));
        if (targets.size() > 1) {
            last = Component.translatable(Translations.Tooltip.UPGRADE_TARGETS_AND,
                Component.translatable(Translations.Tooltip.upgradeTarget(targets.get(targets.size() - 2))), last);
        }
        return result.append(last);
    }

    private static class Upgrade {

        private final Supplier<? extends IPipeUpgrade> upgradeConstructor;
        private final Class<? extends IPipeUpgrade> upgradeClass;

        private Upgrade(Supplier<? extends IPipeUpgrade> moduleConstructor) {
            upgradeConstructor = moduleConstructor;
            upgradeClass = moduleConstructor.get().getClass();
        }

        private IPipeUpgrade getIPipeUpgrade() {
            if (upgradeConstructor == null) {
                return null;
            }
            return upgradeConstructor.get();
        }

        private Class<? extends IPipeUpgrade> getIPipeUpgradeClass() {
            return upgradeClass;
        }
    }
}
