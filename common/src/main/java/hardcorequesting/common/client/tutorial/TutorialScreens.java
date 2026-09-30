package hardcorequesting.common.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * The screen names used to tell what screen is active
 * Most screens are containers and referenced by their menu (minecraft:crafting)
 * Screens that don't have names have their reference name defined below (inventory, pause, advancements, gameplay, and any)
 */
@Environment(EnvType.CLIENT)
public class TutorialScreens {
    private static final Set<String> BUILT_IN = Set.of("any", "gameplay", "inventory", "pause", "advancements");

    // Whether a screen name matches the open screen, which is null during gameplay
    public static boolean matches(String name, @Nullable Screen screen) {
        return switch (name) {
            case "any" -> screen != null;
            case "gameplay" -> screen == null;
            case "inventory" -> screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
            case "pause" -> screen instanceof PauseScreen;
            case "advancements" -> screen instanceof AdvancementsScreen;
            default -> {
                ResourceLocation id = ResourceLocation.tryParse(name);
                yield id != null && screen instanceof AbstractContainerScreen<?> container && id.equals(menuId(container));
            }
        };
    }

    // Whether any of the screen names matches the open screen
    public static boolean matchesAny(List<String> names, @Nullable Screen screen) {
        for (String name : names) {
            if (matches(name, screen)) return true;
        }
        return false;
    }

    // Whether a screen name exists
    public static boolean isKnownScreen(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return BUILT_IN.contains(name) || (id != null && BuiltInRegistries.MENU.containsKey(id));
    }

    // A containers menu id, null for menus without defined names
    @Nullable
    private static ResourceLocation menuId(AbstractContainerScreen<?> screen) {
        try {
            return BuiltInRegistries.MENU.getKey(screen.getMenu().getType());
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
