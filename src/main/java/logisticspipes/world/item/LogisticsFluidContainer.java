package logisticspipes.world.item;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IItemAdvancedExistence;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.component.LPDataComponents;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistence {

    public LogisticsFluidContainer(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A fluid container holding the given fluid, the form fluids take while they travel. */
    public static ItemIdentifierStack of(FluidIdentifierStack stack) {
        ItemStack item = new ItemStack(LPItems.FLUID_CONTAINER.get(), 1);
        FluidStack fluidStack = stack.makeFluidStack();
        if (!fluidStack.isEmpty()) {
            item.set(LPDataComponents.FLUID, SimpleFluidContent.copyOf(fluidStack));
        }
        return ItemIdentifierStack.getFromStack(item);
    }

    /** The fluid inside a fluid container, or null when the stack is not one or holds none. */
    public static @Nullable FluidIdentifierStack getFluid(ItemIdentifierStack stack) {
        ItemStack itemStack = stack.makeNormalStack();
        if (itemStack.getItem() instanceof LogisticsFluidContainer && itemStack.has(LPDataComponents.FLUID)) {
            return FluidIdentifierStack.getFromStack(
                itemStack.getOrDefault(LPDataComponents.FLUID, SimpleFluidContent.EMPTY).copy());
        }
        return null;
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
        FluidIdentifierStack fluidStack = getFluid(ItemIdentifierStack.getFromStack(itemstack));
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
            FluidIdentifierStack fluidStack = getFluid(ItemIdentifierStack.getFromStack(stack));
            if (fluidStack != null) {
                tooltipAdder.accept(
                    Component.literal("Type:  " + fluidStack.makeFluidStack().getHoverName().getString()));
                tooltipAdder.accept(Component.literal("Value: " + fluidStack.getAmount() + "mB"));
            }
        }
    }

    // fillItemCategory removed in 1.20.1 — creative tab content registered via BuildCreativeModeTabContentsEvent
}
