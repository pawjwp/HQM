package hardcorequesting.common.client.integration;

import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.inventory.MatCraftingMenu;
import hardcorequesting.common.inventory.ModMenus;
import hardcorequesting.common.items.MatItem;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.mat.MatMode;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Lets JEI interact with the items shown in the quest book.
 * An IScreenHandler is registered for the quest book, allowing keybinds to work.
 * This also draws the ingredient list if SHOW_JEI_SIDEBAR is enabled.
 * If it is disabled, it will hide the list by reporting the HQM menu as filling the entire screen.
 * Also adds the MAT's Crafting mode as a crafting catalyst and fixes JEI's recipe transfer button for the crafting mode.
 * EMI loads this same plugin through its JEMI compatibility layer, querying the global handler directly.
 */
@JeiPlugin
public class HQMJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation("hardcorequesting", "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(GuiQuestBook.class, HQMJeiPlugin::getGuiProperties);

        IIngredientManager ingredientManager = registration.getJeiHelpers().getIngredientManager();
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(double mouseX, double mouseY) {
                if (Minecraft.getInstance().screen instanceof GuiQuestBook book) {
                    ItemStack stack = book.getStackUnderMouse(mouseX, mouseY);
                    if (!stack.isEmpty()) {
                        Rect2i area = new Rect2i((int) mouseX, (int) mouseY, 1, 1);
                        return ingredientManager.createClickableIngredient(stack, area, false)
                                .map(ingredient -> (IClickableIngredient<?>) ingredient);
                    }
                }
                return Optional.empty();
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Registers a recipe transfer handler for the crafting menu with the same slot/inventory layout as the vanilla crafting table
        registration.addRecipeTransferHandler(MatCraftingMenu.class, ModMenus.matCrafting.get(), RecipeTypes.CRAFTING, 1, 9, 10, 36);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (HQMConfig.getInstance().MAT.ENABLE_MAT && MatMode.CRAFTING.isEnabled()) {
            ItemStack mat = new ItemStack(ModItems.mat.get());
            MatItem.setMode(mat, MatMode.CRAFTING);
            registration.addRecipeCatalyst(mat, RecipeTypes.CRAFTING);
        }
    }

    private static IGuiProperties getGuiProperties(GuiQuestBook screen) {
        // Return null to avoid error logging
        if (screen.width <= 0 || screen.height <= 0) {
            return null;
        }
        if (HQMConfig.getInstance().Integration.SHOW_JEI_SIDEBAR) {
            int left = (screen.width - GuiQuestBook.TEXTURE_WIDTH) / 2;
            int top = (screen.height - GuiQuestBook.TEXTURE_HEIGHT) / 2;
            return new BookGuiProperties(left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, screen.width, screen.height);
        }
        // If SHOW_JEI_SIDEBAR is is not true, report the book as filling the window so the JEI overlay has no room to draw itself.
        return new BookGuiProperties(0, 0, screen.width, screen.height, screen.width, screen.height);
    }

    private record BookGuiProperties(int getGuiLeft, int getGuiTop, int getGuiXSize, int getGuiYSize,
                                     int getScreenWidth, int getScreenHeight) implements IGuiProperties {
        @Override
        public Class<? extends Screen> getScreenClass() {
            return GuiQuestBook.class;
        }
    }
}