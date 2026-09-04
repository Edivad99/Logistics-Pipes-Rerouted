package logisticspipes.integrations.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.gui.SetGhostSlotMessage;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;
import network.rs485.logisticspipes.gui.widget.GhostSlot;
import network.rs485.logisticspipes.gui.widget.Unmodifiable;

/**
 * Enables dragging items from the JEI ingredient panel into LP ghost/filter slots.
 */
public class GhostIngredientHandler implements IGhostIngredientHandler<AbstractContainerScreen<?>> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(AbstractContainerScreen<?> gui, ITypedIngredient<I> typedIngredient,
        boolean doStart) {

        List<Target<I>> targets = new ArrayList<>();
        typedIngredient.getItemStack().ifPresent(stack -> {
            targets.addAll(getItemTargets(gui, stack));
            if (FluidIdentifier.get(stack) != null) {
                targets.addAll(getFluidTargetsFromItem(gui, stack));
            }
        });
        typedIngredient.getIngredient(NeoForgeTypes.FLUID_STACK).ifPresent(fluidStack -> {
            targets.addAll(getFluidTargets(gui, fluidStack));
        });

        return targets;
    }

    @Override
    public void onComplete() {
        // nothing to clean up
    }

    /**
     * Whether an item dragged out of JEI can be dropped on this slot.
     */
    private static boolean acceptsGhostItem(Slot slot) {
        if (slot instanceof Unmodifiable) {
            return false;
        }
        return slot instanceof GhostSlot || slot instanceof DummySlot;
    }

    private <I> List<Target<I>> getItemTargets(AbstractContainerScreen<?> gui, ItemStack ingredient) {
        List<Target<I>> targets = new ArrayList<>();
        for (Slot slot : gui.getMenu().slots) {
            if (!acceptsGhostItem(slot)) {
                continue;
            }

            targets.add(new Target<>() {

                @Override
                public Rect2i getArea() {
                    return new Rect2i(
                        gui.getLeftPos() + slot.x,
                        gui.getTopPos() + slot.y,
                        17,
                        17
                    );
                }

                @Override
                public void accept(I ignored) {
                    ItemStack copy = ingredient.copyWithCount(1);
                    slot.set(copy);
                    ClientPacketDistributor.sendToServer(new SetGhostSlotMessage(slot.index, copy));
                }
            });
        }
        return targets;
    }

    private <I> List<Target<I>> getFluidTargetsFromItem(AbstractContainerScreen<?> gui, ItemStack ingredient) {

        List<Target<I>> targets = new ArrayList<>();

        for (Slot slot : gui.getMenu().slots) {
            if (!(slot instanceof FluidSlot fluidSlot)) {
                continue;
            }

            targets.add(new Target<>() {

                @Override
                public Rect2i getArea() {
                    return new Rect2i(
                        gui.getLeftPos() + fluidSlot.x,
                        gui.getTopPos() + fluidSlot.y,
                        17,
                        17
                    );
                }

                @Override
                public void accept(I ignored) {
                    FluidIdentifier ident = FluidIdentifier.get(ingredient);

                    if (ident == null) {
                        return;
                    }

                    ItemStack stack = ident
                        .getItemIdentifier()
                        .makeNormalStack(1);

                    fluidSlot.set(stack);

                    ClientPacketDistributor.sendToServer(new SetGhostSlotMessage(fluidSlot.index, stack));
                }
            });
        }

        return targets;
    }

    private <I> List<Target<I>> getFluidTargets(AbstractContainerScreen<?> gui, FluidStack fluid) {

        List<Target<I>> targets = new ArrayList<>();

        for (Slot slot : gui.getMenu().slots) {
            if (!(slot instanceof FluidSlot fluidSlot)) {
                continue;
            }

            targets.add(new Target<>() {

                @Override
                public Rect2i getArea() {
                    return new Rect2i(
                        gui.getLeftPos() + fluidSlot.x,
                        gui.getTopPos() + fluidSlot.y,
                        17,
                        17
                    );
                }

                @Override
                public void accept(I ignored) {
                    FluidIdentifier ident = FluidIdentifier.get(fluid);

                    if (ident == null) {
                        return;
                    }

                    ItemStack stack = ident
                        .getItemIdentifier()
                        .makeNormalStack(1);

                    fluidSlot.set(stack);

                    ClientPacketDistributor.sendToServer(new SetGhostSlotMessage(fluidSlot.index, stack));
                }
            });
        }

        return targets;
    }
}
