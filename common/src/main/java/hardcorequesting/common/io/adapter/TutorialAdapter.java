package hardcorequesting.common.io.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import hardcorequesting.common.io.SaveHandler;
import hardcorequesting.common.tutorial.Tutorial;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads tutorial files and throws errors when invalid
 */
public class TutorialAdapter {
    private static final String DEFAULT_ANCHOR = "window/center";
    private static final int DEFAULT_WIDTH = 180;

    public static Tutorial read(String id, String text) {
        JsonObject json = SaveHandler.JSON_PARSER.parse(text).getAsJsonObject();
        List<Tutorial.Step> steps = new ArrayList<>();
        for (JsonElement step : GsonHelper.getAsJsonArray(json, "steps", new JsonArray())) {
            steps.add(readStep(step.getAsJsonObject()));
        }
        return new Tutorial(id, GsonHelper.getAsString(json, "title", id), GsonHelper.getAsString(json, "description", ""), steps);
    }

    // Tutorials are broken into steps, each one advances based on a provided trigger (or a click, if no trigger is defined)
    private static Tutorial.Step readStep(JsonObject json) {
        List<Tutorial.TextBox> textBoxes = new ArrayList<>();
        for (JsonElement textBox : GsonHelper.getAsJsonArray(json, "text_boxes", new JsonArray())) {
            textBoxes.add(readTextBox(textBox.getAsJsonObject()));
        }
        Tutorial.Trigger trigger = new Tutorial.Trigger.ClickTextBox();
        if (json.has("trigger")) {
            trigger = readTrigger(GsonHelper.getAsJsonObject(json, "trigger"));
        }
        return new Tutorial.Step(textBoxes, trigger);
    }

    // A screen trigger without a screen list waits for any screen
    private static Tutorial.Trigger readTrigger(JsonObject json) {
        List<String> screens = readStrings(json, "screens");
        if (screens.isEmpty()) screens = List.of("any");
        String type = GsonHelper.getAsString(json, "type");
        return switch (type) {
            case "click_text_box" -> new Tutorial.Trigger.ClickTextBox();
            case "screen_open" -> new Tutorial.Trigger.ScreenOpen(screens);
            case "screen_close" -> new Tutorial.Trigger.ScreenClose(screens);
            default -> throw new JsonSyntaxException("Unknown trigger type " + type);
        };
    }

    private static Tutorial.TextBox readTextBox(JsonObject json) {
        int offsetX = 0;
        int offsetY = 0;
        if (json.has("offset")) {
            JsonArray offset = GsonHelper.getAsJsonArray(json, "offset");
            offsetX = offset.get(0).getAsInt();
            offsetY = offset.get(1).getAsInt();
        }
        return new Tutorial.TextBox(
            GsonHelper.getAsString(json, "text"),
            GsonHelper.getAsString(json, "anchor", DEFAULT_ANCHOR),
            Tutorial.Side.valueOf(GsonHelper.getAsString(json, "side", "center").toUpperCase(Locale.ROOT)),
            offsetX,
            offsetY,
            GsonHelper.getAsInt(json, "width", DEFAULT_WIDTH),
            readStrings(json, "screens")
        );
    }

    // An array of strings, empty if the key is missing
    private static List<String> readStrings(JsonObject json, String key) {
        List<String> strings = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            strings.add(element.getAsString());
        }
        return strings;
    }
}
