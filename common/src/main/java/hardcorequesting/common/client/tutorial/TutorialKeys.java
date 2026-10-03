package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The keys that a key trigger can be activated on
 * Either a keybind name like key.jump or a key name like key.keyboard.e
 */
@Environment(EnvType.CLIENT)
public class TutorialKeys {

    // Whether a key name matches the pressed key
    public static boolean matches(String name, InputConstants.Key pressed) {
        KeyMapping keybind = findKeybind(name);
        if (keybind != null) return keybind.saveString().equals(pressed.getName()); // name of the key it's bound to
        return name.equals(pressed.getName());
    }

    // Whether any key names match the pressed key
    public static boolean matchesAny(List<String> names, InputConstants.Key pressed) {
        for (String name : names) {
            if (matches(name, pressed)) return true;
        }
        return false;
    }

    // Whether a key name is a keybind or the name of a key itself
    public static boolean isKnownKey(String name) {
        if (findKeybind(name) != null) return true;
        try {
            InputConstants.getKey(name);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Keybinds are all from options.keyMappings
    @Nullable
    private static KeyMapping findKeybind(String name) {
        for (KeyMapping keybind : Minecraft.getInstance().options.keyMappings) {
            if (keybind.getName().equals(name)) return keybind;
        }
        return null;
    }
}
