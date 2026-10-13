package hardcorequesting.common.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The items that a has_item trigger can activate on
 */
@Environment(EnvType.CLIENT)
public class TutorialItems {

    // How many of the player's items match any of the names, counting the main inventory, armor and offhand
    public static int count(Player player, List<String> names) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (List<ItemStack> slots : List.of(inventory.items, inventory.armor, inventory.offhand)) {
            for (ItemStack stack : slots) {
                if (!stack.isEmpty() && matchesAny(names, stack)) count += stack.getCount();
            }
        }
        return count;
    }

    // Whether any of the items match
    private static boolean matchesAny(List<String> names, ItemStack stack) {
        for (String name : names) {
            if (matches(name, stack)) return true;
        }
        return false;
    }

    // If the an ItemStack is a valid match for the provided item name
    public static boolean matches(String name, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (name.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(name.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.tryParse(name));
    }

    // Whether a name is a valid item id or a tag name
    public static boolean isKnownItem(String name) {
        if (name.startsWith("#")) return ResourceLocation.tryParse(name.substring(1)) != null;
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }
}
