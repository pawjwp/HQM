package hardcorequesting.common.tutorial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// The tutorial itself
public record Tutorial(String id, Component title, Component description, @Nullable String icon, boolean defaultUnlocked, boolean autoPlay, Navigation navigation, List<Step> steps) {

    // A step of a tutorial, including the text boxes and highlights it shows, the trigger that advances it to the next step,
    // the commands run when advancing, and how often those commands repeat
    public record Step(List<TextBox> textBoxes, List<Highlight> highlights, Trigger trigger, List<String> commands, CommandRepeat commandRepeat) {
    }

    // Which of the navigation controls are displayed in text boxes
    public enum Navigation {
        NONE(false, false, false),
        FULL(true, true, true),
        ARROWS(true, false, true),
        COUNTER(false, true, false),
        NEXT(false, false, true),
        NEXT_COUNTER(false, true, true),
        PREVIOUS(true, false, false),
        PREVIOUS_COUNTER(true, true, false);

        public final boolean previous, counter, next;

        Navigation(boolean previous, boolean counter, boolean next) {
            this.previous = previous;
            this.counter = counter;
            this.next = next;
        }
    }

    // How often a step's commands run: once for each player, once each time the tutorial is played through, or every time the step finishes
    public enum CommandRepeat {
        PER_PLAYER, PER_TUTORIAL, PER_STEP
    }

    // An outline around an anchor, the screens to display on, and its width
    public record Highlight(String anchor, List<String> screens, int lineWidth) {
    }

    // A text box placed on the screen, including text content, anchor location, anchor side, gap from the anchor, offset, width, screens to display on,
    // the anchor that hides it, if it has a connecting line, the anchor of that line, the width of that line, if it's clamped in the window, and its icon's name
    // Its width is either the width its text wraps at, or ANCHOR_WIDTH to make the whole box exactly as wide as its anchor.
    public record TextBox(Component text, String anchor, Side side, int gap, int offsetX, int offsetY, int width, List<String> screens, @Nullable String unlessAnchor, boolean line, @Nullable String lineAnchor, int lineWidth, boolean clamp, @Nullable String icon) {
        public static final int DEFAULT_WIDTH = 180;
        public static final int ANCHOR_WIDTH = -1;
    }

    // Where a text box is placed relative to its anchor point, with AUTO picking placing automatically towards the center
    public enum Side {
        ABOVE, BELOW, LEFT, RIGHT, CENTER, AUTO, INSIDE_TOP, INSIDE_BOTTOM, INSIDE_LEFT, INSIDE_RIGHT;

        public Side opposite() {
            return switch (this) {
                case ABOVE -> BELOW;
                case BELOW -> ABOVE;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
                case INSIDE_TOP -> INSIDE_BOTTOM;
                case INSIDE_BOTTOM -> INSIDE_TOP;
                case INSIDE_LEFT -> INSIDE_RIGHT;
                case INSIDE_RIGHT -> INSIDE_LEFT;
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

        // A screens opens, or is open if alreadyOpen
        record ScreenOpen(List<String> screens, boolean alreadyOpen) implements Trigger {
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

        // The player has a number of specified items, by id or #tag, in their inventory or hand
        record HasItem(List<String> items, int count, boolean held) implements Trigger {
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
