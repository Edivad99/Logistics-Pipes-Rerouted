package logisticspipes.integrations.jei;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.gui.popup.GuiRecipeImport;
import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;
import logisticspipes.client.gui.screen.LogisticsCraftingTableScreen;
import logisticspipes.client.gui.screen.RequestTableScreen;
import logisticspipes.network.to_server.crafting.ImportCraftingRecipeMessage;
import logisticspipes.utils.gui.IJeiScreenHolder;

public class RecipeTransferHandler<C extends AbstractContainerMenu & IJeiScreenHolder>
    implements IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> {

    private final Class<C> menuClass;

    private final IRecipeTransferHandlerHelper recipeTransferHandlerHelper;

    public RecipeTransferHandler(Class<C> menuClass, IRecipeTransferHandlerHelper recipeTransferHandlerHelper) {
        this.menuClass = menuClass;
        this.recipeTransferHandlerHelper = recipeTransferHandlerHelper;
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return menuClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public IRecipeHolderType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(C container, RecipeHolder<CraftingRecipe> recipe,
        IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return null;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
        IRecipeTransferContext<RecipeHolder<CraftingRecipe>, C> context, boolean doTransfer) {
        LogisticsBaseGuiScreen<?> gui = context.getContainer().getScreenForJEI();

        if (!(gui instanceof LogisticsCraftingTableScreen)
            && !(gui instanceof RequestTableScreen)) {
            return recipeTransferHandlerHelper.createInternalError();
        }

        BlockEntity be;

        if (gui instanceof LogisticsCraftingTableScreen craftingTable) {
            be = craftingTable.crafter;
        } else {
            be = ((RequestTableScreen) gui).table.getContainer();
        }

        if (be == null) {
            return recipeTransferHandlerHelper.createInternalError();
        }

        if (!doTransfer) {
            return null;
        }

        NonNullList<ItemStack> stackList = NonNullList.withSize(9, ItemStack.EMPTY);

        ItemStack[][] stacks = new ItemStack[9][];

        boolean hasCandidates = false;

        int slotIndex = 0;

        for (IRecipeSlotView slotView : context.getRecipeSlots().getSlotViews()) {

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
                    stackList.set(slotIndex, candidates.getFirst());
                }

            } else {
                stackList.set(slotIndex, ItemStack.EMPTY);
            }

            slotIndex++;
        }

        if (hasCandidates) {
            gui.setSubGui(new GuiRecipeImport(be, stacks));
        } else {
            ClientPacketDistributor.sendToServer(
                new ImportCraftingRecipeMessage(be.getBlockPos(), stackList));
        }
        context.completeRecipeTransfer(RecipeTransferResult.SUCCESS);
        return null;
    }
}
