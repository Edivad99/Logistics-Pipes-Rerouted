package logisticspipes.world.item;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistance {

    static int capacity = 8000;

    public LogisticsFluidContainer(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canExistInNormalInventory(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return false;
    }

    /**
     * The fluid's display name for this container stack, or the item's own key when it holds no
     * fluid. Was an override of {@code Item#getDescriptionId(ItemStack)} until 1.21.3 dropped it.
     */
    private String descriptionIdFor(ItemStack stack) {
        FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(
            ItemIdentifierStack.getFromStack(stack), Minecraft.getInstance().level.registryAccess());
        if (fluidStack != null) {
            // Fluid.getDescriptionId() removed in 1.20.1; use FluidStack.getDisplayName()
            return fluidStack.makeFluidStack().getHoverName().getString();
        }
        return getDescriptionId();
    }

    @Override
    public Component getName(ItemStack itemstack) {
        // getUnlocalizedNameInefficiently removed in 1.20.1; use getDescriptionId() for base key
        String translationKey = descriptionIdFor(itemstack);
        String baseKey = getDescriptionId();
        return Component.literal(
            I18n.get(translationKey + (translationKey.equals(baseKey) ? ".name" : "")).trim());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        if (Minecraft.getInstance().hasShiftDown()) {
            FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(
                ItemIdentifierStack.getFromStack(stack), Minecraft.getInstance().level.registryAccess());
            if (fluidStack != null) {
                tooltipAdder.accept(
                    Component.literal("Type:  " + fluidStack.makeFluidStack().getHoverName().getString()));
                tooltipAdder.accept(Component.literal("Value: " + fluidStack.getAmount() + "mB"));
            }
        }
    }

    // fillItemCategory removed in 1.20.1 — creative tab content registered via BuildCreativeModeTabContentsEvent
}
