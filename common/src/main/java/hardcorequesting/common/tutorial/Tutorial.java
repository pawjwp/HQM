package hardcorequesting.common.tutorial;

import java.util.List;

// The tutorial itself
public record Tutorial(String id, String title, String description, List<Step> steps) {

    // A step of a tutorial, including the text boxes it shows and the trigger that advances it to the next step
    public record Step(List<TextBox> textBoxes, Trigger trigger) {
    }

    // A text box placed on the screen, including text content, anchor location, anchor side, offset, width,
    // screens to display on, if it has a connecting line, and width of connecting line
    public record TextBox(String text, String anchor, Side side, int offsetX, int offsetY, int width, List<String> screens, boolean line, int lineWidth) {
    }

    // Where a text box is placed relative to its anchor point
    public enum Side {
        ABOVE, BELOW, LEFT, RIGHT, CENTER
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
    }
}
