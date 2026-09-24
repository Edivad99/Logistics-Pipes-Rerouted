package logisticspipes.world.item;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import logisticspipes.interfaces.IItemAdvancedExistence;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistence {

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

    /** Named after the fluid it holds, or the container's own name when it holds none. */
    @Override
    public Component getName(ItemStack itemstack) {
        FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(
            ItemIdentifierStack.getFromStack(itemstack), Minecraft.getInstance().level.registryAccess());
        if (fluidStack != null) {
            return fluidStack.makeFluidStack().getHoverName();
        }
        return super.getName(itemstack);
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
