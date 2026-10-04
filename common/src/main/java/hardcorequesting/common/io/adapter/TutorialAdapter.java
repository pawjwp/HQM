package hardcorequesting.common.io.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import hardcorequesting.common.io.SaveHandler;
import hardcorequesting.common.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads tutorial files and throws errors when invalid
 */
public class TutorialAdapter {
    private static final String DEFAULT_ANCHOR = "window/center";
    private static final int DEFAULT_WIDTH = 180;
    private static final Tutorial.Click LEFT_CLICK = new Tutorial.Click(0, false, false, false);
    private static final Set<String> MODIFIERS = Set.of("shift", "ctrl", "alt");

    public static Tutorial read(String id, String text) {
        JsonObject json = SaveHandler.JSON_PARSER.parse(text).getAsJsonObject();
        List<Tutorial.Step> steps = new ArrayList<>();
        for (JsonElement step : GsonHelper.getAsJsonArray(json, "steps", new JsonArray())) {
            steps.add(readStep(step.getAsJsonObject()));
        }
        Component title = json.has("title") ? readText(json.get("title")) : Component.literal(id);
        Component description = json.has("description") ? readText(json.get("description")) : Component.empty();
        return new Tutorial(id, title, description, steps);
    }

    // Interpret plain strings as text, everything else as a JSON text component
    private static Component readText(JsonElement json) {
        if (json.isJsonPrimitive()) return Component.literal(json.getAsString());
        return Component.Serializer.fromJson(json);
    }

    // Tutorials are broken into steps, each one advances based on a provided trigger (or a click, if no trigger is defined)
    private static Tutorial.Step readStep(JsonObject json) {
        List<Tutorial.TextBox> textBoxes = new ArrayList<>();
        for (JsonElement textBox : GsonHelper.getAsJsonArray(json, "text_boxes", new JsonArray())) {
            textBoxes.add(readTextBox(textBox.getAsJsonObject()));
        }
        Tutorial.Trigger trigger = new Tutorial.Trigger.ClickTextBox(List.of(LEFT_CLICK));
        if (json.has("trigger")) {
            trigger = readTrigger(GsonHelper.getAsJsonObject(json, "trigger"));
        }
        return new Tutorial.Step(textBoxes, trigger);
    }

    // A screen trigger without a screen list waits for any screen
    // A key trigger without a screen only triggers during gameplay
    private static Tutorial.Trigger readTrigger(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        List<String> screens = readStrings(json, "screens");
        if (screens.isEmpty()) screens = List.of(type.equals("key") ? "gameplay" : "any");
        return switch (type) {
            case "click_text_box" -> new Tutorial.Trigger.ClickTextBox(readClicks(json));
            case "screen_open" -> new Tutorial.Trigger.ScreenOpen(screens);
            case "screen_close" -> new Tutorial.Trigger.ScreenClose(screens);
            case "key" -> {
                List<String> keys = readStrings(json, "keys");
                if (keys.isEmpty()) throw new JsonSyntaxException("A key trigger needs valid keys");
                yield new Tutorial.Trigger.Key(keys, screens);
            }
            case "click_anchor" -> {
                List<String> anchors = readStrings(json, "anchors");
                if (anchors.isEmpty()) throw new JsonSyntaxException("A click_anchor trigger needs valid anchors");
                yield new Tutorial.Trigger.ClickAnchor(anchors, screens, readClicks(json));
            }
            case "timer" -> {
                int ticks = Math.round(GsonHelper.getAsFloat(json, "seconds", 0) * 20); // Multiply by 20 to convert to tickss
                if (ticks <= 0) throw new JsonSyntaxException("A timer trigger needs a valid second count");
                yield new Tutorial.Trigger.Timer(ticks);
            }
            case "has_item" -> {
                List<String> items = readStrings(json, "items");
                if (items.isEmpty()) throw new JsonSyntaxException("A has_item trigger needs a list of items");
                int count = GsonHelper.getAsInt(json, "count", 1);
                if (count < 1) throw new JsonSyntaxException("A has_item trigger needs a count of at least 1");
                yield new Tutorial.Trigger.HasItem(items, count);
            }
            case "location" -> {
                BlockPos position = null;
                if (json.has("position")) {
                    JsonArray xyz = GsonHelper.getAsJsonArray(json, "position");
                    position = new BlockPos(xyz.get(0).getAsInt(), xyz.get(1).getAsInt(), xyz.get(2).getAsInt());
                }
                List<String> dimensions = readStrings(json, "dimensions");
                List<String> biomes = readStrings(json, "biomes");
                if (dimensions.isEmpty() && position == null && biomes.isEmpty()) throw new JsonSyntaxException("A location trigger needs a position, dimensions, or biomes");
                yield new Tutorial.Trigger.Location(dimensions, position, GsonHelper.getAsInt(json, "radius", 3), biomes);
            }
            case "any" -> new Tutorial.Trigger.Any(readTriggers(json));
            case "all" -> new Tutorial.Trigger.All(readTriggers(json));
            default -> throw new JsonSyntaxException("Unknown trigger type " + type);
        };
    }

    // The triggers inside an "any" or "all" trigger
    private static List<Tutorial.Trigger> readTriggers(JsonObject json) {
        List<Tutorial.Trigger> triggers = new ArrayList<>();
        for (JsonElement trigger : GsonHelper.getAsJsonArray(json, "triggers", new JsonArray())) {
            triggers.add(readTrigger(trigger.getAsJsonObject()));
        }
        if (triggers.isEmpty()) throw new JsonSyntaxException("An any or all trigger needs a valid list of triggers");
        return triggers;
    }

    // A click trigger with no click types uses left click by default
    private static List<Tutorial.Click> readClicks(JsonObject json) {
        List<Tutorial.Click> clicks = new ArrayList<>();
        for (String click : readStrings(json, "clicks")) {
            clicks.add(readClick(click));
        }
        if (clicks.isEmpty()) clicks.add(LEFT_CLICK);
        return clicks;
    }

    // Interpret click types, including applying modifiers
    private static Tutorial.Click readClick(String text) {
        List<String> parts = List.of(text.split("\\+"));
        int button = switch (parts.get(parts.size() - 1)) {
            case "left" -> 0;
            case "right" -> 1;
            case "middle" -> 2;
            default -> throw new JsonSyntaxException("Unknown mouse button in click " + text);
        };
        List<String> modifiers = parts.subList(0, parts.size() - 1);
        for (String modifier : modifiers) {
            if (!MODIFIERS.contains(modifier)) throw new JsonSyntaxException("Unknown modifier in click " + text);
        }
        return new Tutorial.Click(button, modifiers.contains("shift"), modifiers.contains("ctrl"), modifiers.contains("alt"));
    }

    private static Tutorial.TextBox readTextBox(JsonObject json) {
        int offsetX = 0;
        int offsetY = 0;
        if (json.has("offset")) {
            JsonArray offset = GsonHelper.getAsJsonArray(json, "offset");
            offsetX = offset.get(0).getAsInt();
            offsetY = offset.get(1).getAsInt();
        }
        if (!json.has("text")) throw new JsonSyntaxException("A text box needs text");
        return new Tutorial.TextBox(
            readText(json.get("text")),
            GsonHelper.getAsString(json, "anchor", DEFAULT_ANCHOR),
            Tutorial.Side.valueOf(GsonHelper.getAsString(json, "side", "center").toUpperCase(Locale.ROOT)),
            offsetX,
            offsetY,
            GsonHelper.getAsInt(json, "width", DEFAULT_WIDTH),
            readStrings(json, "screens"),
            GsonHelper.getAsBoolean(json, "line", false),
            GsonHelper.getAsInt(json, "line_width", 1)
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
