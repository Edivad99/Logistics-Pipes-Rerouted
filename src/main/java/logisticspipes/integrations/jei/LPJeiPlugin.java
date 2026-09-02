package logisticspipes.integrations.jei;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import logisticspipes.LPConstants;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.world.inventory.DummyMenu;
import network.rs485.logisticspipes.gui.BaseGuiContainer;

@JeiPlugin
public class LPJeiPlugin implements IModPlugin {

    private static final Identifier PLUGIN_ID = LPConstants.rl("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Register extra-area handler for all LP container screens
        registration.addGenericGuiContainerHandler(BaseGuiContainer.class, new LPAdvancedGuiHandler());
        // Ghost ingredient handler: registered on AbstractContainerScreen so JEI calls us for any
        // LP screen; the handler itself checks for GhostSlots in the open menu.
        registration.addGhostIngredientHandler(
            (Class) AbstractContainerScreen.class, new GhostIngredientHandler());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper transferHelper = registration.getTransferHelper();
        // Two menu bases while the GUIs move over to vanilla menus; the DummyContainer one goes
        // with the last of them.
        registration.addRecipeTransferHandler(
            new RecipeTransferHandler<>(DummyContainer.class, transferHelper), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(
            new RecipeTransferHandler<>(DummyMenu.class, transferHelper), RecipeTypes.CRAFTING);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JEIPluginLoader.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JEIPluginLoader.clearRuntime();
    }
}
