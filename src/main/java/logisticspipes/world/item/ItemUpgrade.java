package logisticspipes.world.item;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;


import logisticspipes.pipes.upgrades.IPipeUpgrade;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemUpgrade extends LogisticsItem {

    //Values
    public static final int MAX_LIQUID_CRAFTER = 3;
    public static final int MAX_CRAFTING_CLEANUP = 4;
    public static final int MAX_ITEM_EXTRACTION = 8;
    public static final int MAX_ITEM_STACK_EXTRACTION = 8;
    public static String SHIFT_INFO_PREFIX = "item.upgrade.info.";
    private final Upgrade upgradeType;

    public ItemUpgrade(Upgrade upgradeType, Properties properties) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    /**
     * Factory for use with DeferredRegister.
     */
    public static ItemUpgrade of(Supplier<? extends IPipeUpgrade> upgradeConstructor, Properties properties) {
        return new ItemUpgrade(new Upgrade(upgradeConstructor), properties);
    }

    public static Item getAndCheckUpgrade(ResourceLocation resource) {
        Objects.requireNonNull(resource, "Resource for upgrade is null. Was the upgrade registered?");
        return Objects.requireNonNull(BuiltInRegistries.ITEM.getValue(resource),
            "Upgrade " + resource + " not found in Item registry");
    }

    public IPipeUpgrade getUpgradeForItem(ItemStack itemStack, IPipeUpgrade currentUpgrade) {
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
        if (Screen.hasShiftDown()) {
            if (!pipe.isEmpty() && !module.isEmpty()) {
                //Can be applied to {0} pipes
                //and {0} modules
                String base1 = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "both1");
                String base2 = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "both2");
                tooltipAdder.accept(Component.literal(MessageFormat.format(base1, join(pipe))));
                tooltipAdder.accept(Component.literal(MessageFormat.format(base2, join(module))));
            } else if (!pipe.isEmpty()) {
                //Can be applied to {0} pipes
                String base = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "pipe");
                tooltipAdder.accept(Component.literal(MessageFormat.format(base, join(pipe))));
            } else {
                //Can be applied to {0} modules
                String base = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "module");
                tooltipAdder.accept(Component.literal(MessageFormat.format(base, join(module))));
            }
        } else {
            TextUtil.addTooltipInformation(stack, tooltipAdder, false);
        }
    }

    private String join(List<String> join) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < join.size() - 2; i++) {
            builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(i)));
            builder.append(", ");
        }
        if (join.size() > 1) {
            builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(join.size() - 2)));
            builder.append(" and ");
        }
        builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(join.size() - 1)));
        return builder.toString();
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
