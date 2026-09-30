package hardcorequesting.common.tutorial;

import java.util.List;

// The tutorial itself
public record Tutorial(String id, String title, String description, List<Step> steps) {

    // A step of a tutorial, including the text boxes it shows and the trigger that advances it to the next step
    public record Step(List<TextBox> textBoxes, Trigger trigger) {
    }

    // A text box placed on the screen, including text content, anchor location, anchor side, offset, width, and screens to display on
    public record TextBox(String text, String anchor, Side side, int offsetX, int offsetY, int width, List<String> screens) {
    }

    // Where a text box is placed relative to its anchor point
    public enum Side {
        ABOVE, BELOW, LEFT, RIGHT, CENTER
    }

    // The trigger that advances to the next step of the tutorial
    public sealed interface Trigger {
        // The screens a screen trigger can wait for
        default List<String> screens() {
            return List.of();
        }

        // Left clicking a text box (default)
        record ClickTextBox() implements Trigger {
        }

        // A screens opens, or is open
        record ScreenOpen(List<String> screens) implements Trigger {
        }

        // A screen closes
        record ScreenClose(List<String> screens) implements Trigger {
        }
    }
}
