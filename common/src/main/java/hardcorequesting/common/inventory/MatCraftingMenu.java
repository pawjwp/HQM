package hardcorequesting.common.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// A retextured copy of the vanilla CraftingMenu with a tab bar
public class MatCraftingMenu extends CraftingMenu {
    public MatCraftingMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.matCrafting.get();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Give items back to the player when the inventory is closed, might
        if (!player.level().isClientSide) {
            for (int i = 1; i <= 9; i++) {
                Slot slot = this.slots.get(i);
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
        }
    }
}
