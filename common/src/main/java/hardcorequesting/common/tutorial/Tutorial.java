package hardcorequesting.common.tutorial;

import java.util.List;

// The tutorial itself
public record Tutorial(String id, String title, String description, List<Step> steps) {

    // A step of a tutorial, including the text boxes it shows and the trigger that advances it to the next step
    public record Step(List<TextBox> textBoxes, Trigger trigger) {
    }

    // A text box placed on the screen, including the text content, anchor location, anchor side, offset, and width
    public record TextBox(String text, String anchor, Side side, int offsetX, int offsetY, int width) {
    }

    // What part of a text box is placed on the anchor point
    public enum Side {
        ABOVE, BELOW, LEFT, RIGHT, CENTER
    }

    // The trigger that advances to the next step of the tutorial
    public enum Trigger {
        CLICK_TEXT_BOX // Left clicking on a text box (default)
    }
}
