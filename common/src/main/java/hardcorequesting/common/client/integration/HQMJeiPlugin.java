package hardcorequesting.common.client.integration;

import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.config.HQMConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
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

    private static IGuiProperties getGuiProperties(GuiQuestBook screen) {
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