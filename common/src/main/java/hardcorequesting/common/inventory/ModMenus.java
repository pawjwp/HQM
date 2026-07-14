package hardcorequesting.common.inventory;

import hardcorequesting.common.HardcoreQuestingCore;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class ModMenus {
    public static Supplier<MenuType<MatCraftingMenu>> matCrafting;

    public static void init() {
        matCrafting = HardcoreQuestingCore.platform.registerMenu("mat_mode_crafting", MatCraftingMenu::new);
    }
}
