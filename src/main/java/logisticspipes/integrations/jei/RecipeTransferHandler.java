package logisticspipes.integrations.jei;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import org.jetbrains.annotations.Nullable;

import logisticspipes.gui.GuiLogisticsCraftingTable;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.gui.popup.GuiRecipeImport;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.NEISetCraftingRecipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public class RecipeTransferHandler implements IRecipeTransferHandler<DummyContainer, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper recipeTransferHandlerHelper;

    public RecipeTransferHandler(IRecipeTransferHandlerHelper recipeTransferHandlerHelper) {
        this.recipeTransferHandlerHelper = recipeTransferHandlerHelper;
    }

    @Override
    public Class<? extends DummyContainer> getContainerClass() {
        return DummyContainer.class;
    }

    @Override
    public Optional<MenuType<DummyContainer>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public IRecipeHolderType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(DummyContainer container, RecipeHolder<CraftingRecipe> recipe,
        IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        LogisticsBaseGuiScreen gui = container.guiHolderForJEI;

        if (!(gui instanceof GuiLogisticsCraftingTable)
            && !(gui instanceof GuiRequestTable)) {
            return recipeTransferHandlerHelper.createInternalError();
        }

        BlockEntity be;

        if (gui instanceof GuiLogisticsCraftingTable craftingTable) {
            be = craftingTable.crafter;
        } else {
            be = ((GuiRequestTable) gui).table.container;
        }

        if (be == null) {
            return recipeTransferHandlerHelper.createInternalError();
        }

        if (!doTransfer) {
            return null;
        }

        NEISetCraftingRecipe packet =
            PacketHandler.getPacket(NEISetCraftingRecipe.class);

        NonNullList<ItemStack> stackList = packet.getStackList();

        ItemStack[][] stacks = new ItemStack[9][];

        boolean hasCandidates = false;

        int slotIndex = 0;

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews()) {

            if (slotView.getRole() != RecipeIngredientRole.INPUT) {
                continue;
            }

            if (slotIndex >= 9) {
                break;
            }

            List<ItemStack> candidates = slotView.getAllIngredients()
                .filter(ingredient ->
                    ingredient.getType() == VanillaTypes.ITEM_STACK)
                .map(ingredient ->
                    ingredient.getItemStack().orElse(ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .toList();

            if (!candidates.isEmpty()) {

                stacks[slotIndex] =
                    candidates.toArray(new ItemStack[0]);

                if (candidates.size() > 1) {
                    hasCandidates = true;
                } else {
                    stackList.set(slotIndex, candidates.get(0));
                }

            } else {
                stackList.set(slotIndex, ItemStack.EMPTY);
            }

            slotIndex++;
        }

        if (hasCandidates) {
            gui.setSubGui(new GuiRecipeImport(be, stacks));
        } else {
            MainProxy.sendPacketToServer(packet.setTilePos(be));
        }
        return null;
    }
}
