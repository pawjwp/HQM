package hardcorequesting.common.tutorial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// The tutorial itself
public record Tutorial(String id, Component title, Component description, @Nullable String icon, boolean defaultUnlocked, boolean autoPlay, boolean navigation, List<Step> steps) {

    // A step of a tutorial, including the text boxes and highlights it shows and the trigger that advances it to the next step
    public record Step(List<TextBox> textBoxes, List<Highlight> highlights, Trigger trigger) {
    }

    // An outline around an anchor, the screens to display on, and its width
    public record Highlight(String anchor, List<String> screens, int lineWidth) {
    }

    // A text box placed on the screen, including text content, anchor location, anchor side, gap from the anchor, offset, width,
    // screens to display on, if it has a connecting line, width of connecting line, if it's clamped in the window, and its icon's name
    public record TextBox(Component text, String anchor, Side side, int gap, int offsetX, int offsetY, int width, List<String> screens, boolean line, int lineWidth, boolean clamp, @Nullable String icon) {
    }

    // Where a text box is placed relative to its anchor point, with AUTO picking placing automatically towards the center
    public enum Side {
        ABOVE, BELOW, LEFT, RIGHT, CENTER, AUTO;

        public Side opposite() {
            return switch (this) {
                case ABOVE -> BELOW;
                case BELOW -> ABOVE;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
                default -> this;
            };
        }
    }

    // A mouse button (0 left, 1 right, 2 middle) and the modifier keys that must be held with it
    public record Click(int button, boolean shift, boolean ctrl, boolean alt) {
    }

    // The trigger that advances to the next step of the tutorial
    public sealed interface Trigger {
        // The screens a screen trigger can wait for
        default List<String> screens() {
            return List.of();
        }

        // The triggers inside an "any" or "all" trigger
        default List<Trigger> triggers() {
            return List.of();
        }

        // Clicking a text box with one of the clicks (default left click)
        record ClickTextBox(List<Click> clicks) implements Trigger {
        }

        // A screens opens, or is open
        record ScreenOpen(List<String> screens) implements Trigger {
        }

        // A screen closes
        record ScreenClose(List<String> screens) implements Trigger {
        }

        // A key is pressed while one of the screens is open, by keybind name or exact key name
        record Key(List<String> keys, List<String> screens) implements Trigger {
        }

        // One of the clicks in one of the anchors while a screen is open
        record ClickAnchor(List<String> anchors, List<String> screens, List<Click> clicks) implements Trigger {
        }

        // A number of ticks pass
        record Timer(int ticks) implements Trigger {
        }

        // The player has a number of specified items, by id or #tag
        record HasItem(List<String> items, int count) implements Trigger {
        }

        // The player is located at the listed position, in the listed dimensions, or in the listed biomes
        record Location(List<String> dimensions, @Nullable BlockPos position, int radius, List<String> biomes) implements Trigger {
        }

        // A quest is completed (even if completed previously)
        record QuestComplete(List<String> quests) implements Trigger {
        }

        // A reward of a quest is claimed during the step
        record RewardClaim(List<String> quests) implements Trigger {
        }

        // One of the triggers happens
        record Any(List<Trigger> triggers) implements Trigger {
        }

        // Every one of the triggers happens, in any order
        record All(List<Trigger> triggers) implements Trigger {
        }
    }
}
