package hardcorequesting.common.client.tutorial;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The items that a has_item trigger can activate on and slot anchors can use
 */
@Environment(EnvType.CLIENT)
public class TutorialItems {
    private static final Map<String, CompoundTag> PARSED_NBT = new HashMap<>(); // each item's NBT

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
    // Curly braces at the end match against item NBT
    public static boolean matches(String name, ItemStack stack) {
        if (stack.isEmpty()) return false;
        int brace = name.indexOf('{');
        if (brace >= 0) {
            CompoundTag nbt = parseNbt(name.substring(brace));
            if (nbt == null || !NbtUtils.compareNbt(nbt, stack.getTag(), true)) return false;
            name = name.substring(0, brace);
        }
        if (name.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(name.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.tryParse(name));
    }

    // Whether a name is a valid item id or a tag name
    public static boolean isKnownItem(String name) {
        int brace = name.indexOf('{');
        if (brace >= 0) {
            if (parseNbt(name.substring(brace)) == null) return false;
            name = name.substring(0, brace);
        }
        if (name.startsWith("#")) return ResourceLocation.tryParse(name.substring(1)) != null;
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    // Parsed NBT
    @Nullable
    private static CompoundTag parseNbt(String text) {
        return PARSED_NBT.computeIfAbsent(text, key -> {
            try {
                return TagParser.parseTag(key);
            } catch (CommandSyntaxException e) {
                return null;
            }
        });
    }
}
