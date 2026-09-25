package hardcorequesting.common.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * A retextured copy of the vanilla CraftingMenu with a tab bar
 */
public class MatCraftingMenu extends CraftingMenu {
    public MatCraftingMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public MatCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.matCrafting.get();
    }

    // Stay open until the player manually closes the UI
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
