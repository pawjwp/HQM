package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.tutorial.Tutorial;
import hardcorequesting.common.tutorial.TutorialManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plays a tutorial by drawing its text boxes over the screen
 */
@Environment(EnvType.CLIENT)
public class TutorialPlayer {
    private static final int Z = 500;                     // above item tooltips which are drawn at a z of 400
    private static final int FRAME = 6;                   // space between a text box's edge and its text, including the outlines
    private static final int FILL = 0xBF000000;
    private static final int HOVERED_FILL = 0xEF080808;   // more opaque when hovered
    private static final int BORDER_TOP = 0xBFFFFFFF;
    private static final int BORDER_BOTTOM = 0xBFDFDFDF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TIMER_COLOR = 0xFF00AA00;    // green timer color vanilla toasts use for progress

    @Nullable
    private static Tutorial tutorial;
    private static int stepIndex;
    private static final Map<Tutorial.Trigger, TriggerState> states = new IdentityHashMap<>(); // by identity, since identical triggers can appear twice
    @Nullable
    private static Screen tickScreen; // the screen open last tick

    // Progress of one trigger during the current step
    private static class TriggerState {
        boolean done;
        boolean screenWasOpen; // if a screen_close trigger's screen was open during this step
        int timerTicks;        // ticks counted so far for a timer trigger
    }

    // A text box's lines and size on-screen, the anchor it was placed against, and the side it was placed on
    private record Layout(List<FormattedCharSequence> lines, int x, int y, int width, int height, Rect2i anchor, Tutorial.Side side) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    public static void register() {
        // Render text boxes on screen during gameplay
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTick) -> {
            if (Minecraft.getInstance().screen == null) render(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTick);
        });
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, partialTick) -> render(graphics, mouseX, mouseY, partialTick));

        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register((minecraft, screen, mouseX, mouseY, button) -> {
            if (tutorial == null) return EventResult.pass();
            boolean clickedTextBox = false;
            for (Tutorial.Trigger trigger : currentTriggers()) {
                // Clicking a text box completes a click_text_box trigger instead of clicking whatever is underneath
                if (trigger instanceof Tutorial.Trigger.ClickTextBox click && !state(trigger).done && matchesAnyClick(click.clicks(), button) && isOverTextBox(mouseX, mouseY)) {
                    state(trigger).done = true;
                    clickedTextBox = true;
                }
                // Clicking an anchor completes a click_anchor trigger and also clicks what's underneath
                if (trigger instanceof Tutorial.Trigger.ClickAnchor click && matchesAnyClick(click.clicks(), button)
                        && TutorialScreens.matchesAny(click.screens(), screen) && isOverAnchor(click.anchors(), mouseX, mouseY)) {
                    state(trigger).done = true;
                }
            }
            advanceIfDone();
            if (clickedTextBox) return EventResult.interruptFalse();
            return EventResult.pass();
        });

        // Check key and mouse presses without stopping them from running
        ClientScreenInputEvent.KEY_PRESSED_PRE.register((minecraft, screen, keyCode, scanCode, modifiers) -> {
            keyPressed(InputConstants.getKey(keyCode, scanCode), screen);
            return EventResult.pass();
        });
        ClientRawInputEvent.KEY_PRESSED.register((minecraft, keyCode, scanCode, action, modifiers) -> {
            if (action == GLFW.GLFW_PRESS && tickScreen == null) keyPressed(InputConstants.getKey(keyCode, scanCode), null);
            return EventResult.pass();
        });
        // Mouse presses are checked before anything uses them, with or without a screen open
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register((minecraft, button, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) keyPressed(InputConstants.Type.MOUSE.getOrCreate(button), minecraft.screen);
            return EventResult.pass();
        });

        // Screen and timer triggers are checked each tick
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            tickScreen = minecraft.screen;
            if (tutorial == null) return;
            for (Tutorial.Trigger trigger : currentTriggers()) {
                TriggerState state = state(trigger);
                boolean open = TutorialScreens.matchesAny(trigger.screens(), minecraft.screen);
                if (trigger instanceof Tutorial.Trigger.ScreenOpen && open) {
                    state.done = true;
                } else if (trigger instanceof Tutorial.Trigger.ScreenClose) {
                    if (open) state.screenWasOpen = true;
                    else if (state.screenWasOpen) state.done = true;
                } else if (trigger instanceof Tutorial.Trigger.Timer timer && !state.done && !minecraft.isPaused() && ++state.timerTicks >= timer.ticks()) {
                    state.done = true;
                } else if (trigger instanceof Tutorial.Trigger.HasItem hasItem && !state.done && TutorialItems.count(minecraft.player, hasItem.items()) >= hasItem.count()) {
                    state.done = true;
                } else if (trigger instanceof Tutorial.Trigger.Location location && !state.done && TutorialLocations.matches(location, minecraft.player)) {
                    state.done = true;
                }
            }
            advanceIfDone();
        });
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> pause());
    }

    // Starts the tutorial at a step, replacing any that are already running, and saves that step on the server
    public static void start(Tutorial tutorial, int startStep) {
        states.clear();
        for (Tutorial.Step step : tutorial.steps()) {
            for (Tutorial.TextBox textBox : step.textBoxes()) {
                if (!TutorialAnchors.isKnownAnchor(textBox.anchor())) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown anchor %s, so that text box is not shown", tutorial.id(), textBox.anchor());
                }
                for (String screen : textBox.screens()) {
                    if (!TutorialScreens.isKnownScreen(screen)) {
                        HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown screen %s, so that text box is not shown", tutorial.id(), screen);
                    }
                }
            }
            for (Tutorial.Highlight highlight : step.highlights()) {
                if (!TutorialAnchors.isKnownAnchor(highlight.anchor())) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown anchor %s, so that highlight is not shown", tutorial.id(), highlight.anchor());
                } else if (highlight.anchor().startsWith("window/")) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s highlights %s, which is a point with nothing to outline", tutorial.id(), highlight.anchor());
                }
                for (String screen : highlight.screens()) {
                    if (!TutorialScreens.isKnownScreen(screen)) {
                        HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown screen %s, so that highlight is not shown", tutorial.id(), screen);
                    }
                }
            }
            List<Tutorial.Trigger> stepTriggers = new ArrayList<>();
            collectTriggers(step.trigger(), stepTriggers, false);
            for (Tutorial.Trigger trigger : stepTriggers) {
                for (String screen : trigger.screens()) {
                    if (!TutorialScreens.isKnownScreen(screen)) {
                        HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown screen %s in a trigger, so that step can't advance on it", tutorial.id(), screen);
                    }
                }
                if (trigger instanceof Tutorial.Trigger.Key key) {
                    for (String name : key.keys()) {
                        if (!TutorialKeys.isKnownKey(name)) {
                            HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown key %s, so that step is unable to advance", tutorial.id(), name);
                        }
                    }
                }
                if (trigger instanceof Tutorial.Trigger.ClickAnchor click) {
                    for (String anchor : click.anchors()) {
                        if (!TutorialAnchors.isContainerAnchor(anchor)) {
                            HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses %s in a click_anchor trigger, which is likely not actually clickable", tutorial.id(), anchor);
                        }
                    }
                }
                if (trigger instanceof Tutorial.Trigger.HasItem hasItem) {
                    for (String item : hasItem.items()) {
                        if (!TutorialItems.isKnownItem(item)) {
                            HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown item %s, it is likely unable to advance", tutorial.id(), item);
                        }
                    }
                }
                if (trigger instanceof Tutorial.Trigger.Location location) {
                    for (String dimension : location.dimensions()) {
                        if (!TutorialLocations.isKnownDimension(dimension)) {
                            HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown dimension %s, so it never matches", tutorial.id(), dimension);
                        }
                    }
                    for (String biome : location.biomes()) {
                        if (!TutorialLocations.isKnownBiome(biome)) {
                            HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown biome %s, so it never matches", tutorial.id(), biome);
                        }
                    }
                }
            }
        }
        TutorialPlayer.tutorial = tutorial;
        stepIndex = startStep;
        if (stepIndex >= tutorial.steps().size()) stepIndex = 0;
        if (tutorial.steps().isEmpty()) {
            pause(); // a tutorial without steps has nothing to show
        } else {
            states.clear();
            GeneralUsage.sendMatTutorialProgress(tutorial.id(), stepIndex);
        }
    }

    // Reloads the playing tutorial, setting it to a new version if applicable or pausing if not
    public static void reload() {
        if (tutorial == null) return;
        Tutorial reloaded = TutorialManager.getInstance().tutorials.get(tutorial.id());
        if (reloaded == null) pause();
        else start(reloaded, stepIndex);
    }

    // Removes the active tutorial
    public static void pause() {
        tutorial = null;
    }

    @Nullable
    public static String getPlayingId() {
        if (tutorial == null) return null;
        return tutorial.id();
    }

    // The current step's triggers, leaving out unfinished parts of completed "any" and "all" groups
    private static List<Tutorial.Trigger> currentTriggers() {
        List<Tutorial.Trigger> list = new ArrayList<>();
        collectTriggers(tutorial.steps().get(stepIndex).trigger(), list, false);
        return list;
    }

    // Adds a trigger to the list, or adds any/all the triggers inside it
    private static void collectTriggers(Tutorial.Trigger trigger, List<Tutorial.Trigger> list, boolean insideDoneGroup) {
        if (trigger.triggers().isEmpty()) {
            if (!insideDoneGroup || state(trigger).done) list.add(trigger);
        } else {
            boolean done = insideDoneGroup || isDone(trigger);
            for (Tutorial.Trigger part : trigger.triggers()) collectTriggers(part, list, done);
        }
    }

    private static TriggerState state(Tutorial.Trigger trigger) {
        return states.computeIfAbsent(trigger, t -> new TriggerState());
    }

    // The "any" triggers are done when any condition is met
    // The "all" triggers are done when all conditions are met, in any order
    // Other triggers are done once they have happened
    private static boolean isDone(Tutorial.Trigger trigger) {
        if (trigger instanceof Tutorial.Trigger.Any any) return any.triggers().stream().anyMatch(TutorialPlayer::isDone);
        if (trigger instanceof Tutorial.Trigger.All all) return all.triggers().stream().allMatch(TutorialPlayer::isDone);
        return state(trigger).done;
    }

    // Moves to the next step when the current step's trigger is done and saves it on the server, completes the tutorial after the last step
    private static void advanceIfDone() {
        if (tutorial == null || !isDone(tutorial.steps().get(stepIndex).trigger())) return;
        states.clear();
        stepIndex++;
        if (stepIndex < tutorial.steps().size()) {
            GeneralUsage.sendMatTutorialProgress(tutorial.id(), stepIndex);
        } else {
            GeneralUsage.sendMatTutorialCompleted(tutorial.id());
            pause();
        }
    }

    // Completes key triggers when one of their keys is pressed while on the correct screen
    private static void keyPressed(InputConstants.Key pressed, @Nullable Screen screen) {
        if (tutorial == null) return;
        for (Tutorial.Trigger trigger : currentTriggers()) {
            if (trigger instanceof Tutorial.Trigger.Key key && TutorialScreens.matchesAny(key.screens(), screen) && TutorialKeys.matchesAny(key.keys(), pressed)) {
                state(trigger).done = true;
            }
        }
        advanceIfDone();
    }

    private static void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (tutorial == null) return;
        Font font = Minecraft.getInstance().font;
        boolean clickable = false;
        Tutorial.Trigger.Timer timer = null;
        for (Tutorial.Trigger trigger : currentTriggers()) {
            if (trigger instanceof Tutorial.Trigger.ClickTextBox && !state(trigger).done) clickable = true;
            if (trigger instanceof Tutorial.Trigger.Timer next && (timer == null || state(timer).done)) timer = next;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z);
        for (Tutorial.Highlight highlight : tutorial.steps().get(stepIndex).highlights()) {
            drawHighlight(graphics, highlight);
        }
        for (Tutorial.TextBox textBox : tutorial.steps().get(stepIndex).textBoxes()) {
            Layout layout = layout(textBox);
            if (layout == null) continue;
            drawFrame(graphics, layout, clickable && layout.contains(mouseX, mouseY) ? HOVERED_FILL : FILL);
            // Timer triggers have a green bar on the bottom like vanilla's tutorial toasts
            if (timer != null) {
                float progress = Math.min((state(timer).timerTicks + partialTick) / timer.ticks(), 1);
                graphics.fill(layout.x() + 1, layout.bottom() - 2, layout.x() + 1 + (int) ((layout.width() - 2) * progress), layout.bottom() - 1, TIMER_COLOR);
            }
            if (textBox.line()) drawConnectingLine(graphics, textBox, layout);
            for (int i = 0; i < layout.lines().size(); i++) {
                graphics.drawString(font, layout.lines().get(i), layout.x() + FRAME, layout.y() + FRAME + i * font.lineHeight, TEXT_COLOR, false);
            }
        }
        graphics.pose().popPose();
    }

    // If the button matches any click types
    private static boolean matchesAnyClick(List<Tutorial.Click> clicks, int button) {
        for (Tutorial.Click click : clicks) {
            if (click.button() == button && (!click.shift() || Screen.hasShiftDown())
                    && (!click.ctrl() || Screen.hasControlDown()) && (!click.alt() || Screen.hasAltDown())) return true;
        }
        return false;
    }

    // If the mouse is inside any of the anchors
    private static boolean isOverAnchor(List<String> anchors, double mouseX, double mouseY) {
        for (String name : anchors) {
            Rect2i anchor = TutorialAnchors.resolve(name);
            if (anchor != null && anchor.contains((int) mouseX, (int) mouseY)) return true;
        }
        return false;
    }

    private static boolean isOverTextBox(double mouseX, double mouseY) {
        for (Tutorial.TextBox textBox : tutorial.steps().get(stepIndex).textBoxes()) {
            Layout layout = layout(textBox);
            if (layout != null && layout.contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    @Nullable
    private static Layout layout(Tutorial.TextBox textBox) {
        Screen screen = Minecraft.getInstance().screen;
        Font font = Minecraft.getInstance().font;

        // Null when the text box isn't shown, when its anchor can't be found, or if its screen isn't open
        if (!textBox.screens().isEmpty() && !TutorialScreens.matchesAny(textBox.screens(), screen)) return null;
        
        Rect2i anchor = TutorialAnchors.resolve(textBox.anchor());
        if (anchor == null) return null;
        
        // Each text box is as wide and tall as it needs to be to fit all lines (and line width is limited by configured size)
        List<FormattedCharSequence> lines = font.split(textBox.text(), textBox.width());
        int width = lines.stream().mapToInt(font::width).max().orElse(0) + 2 * FRAME;
        int height = lines.size() * font.lineHeight + 2 * FRAME;

        Tutorial.Side side = textBox.side();
        if (side == Tutorial.Side.AUTO) side = autoSide(anchor, width, height, textBox.gap());
        Rect2i box = place(side, anchor, width, height, textBox.gap());
        int x = box.getX() + textBox.offsetX();
        int y = box.getY() + textBox.offsetY();

        // Clamps the text box inside the window
        if (textBox.clamp()) {
            Window window = Minecraft.getInstance().getWindow();
            x = Mth.clamp(x, 0, Math.max(window.getGuiScaledWidth() - width, 0));
            y = Mth.clamp(y, 0, Math.max(window.getGuiScaledHeight() - height, 0));
        }

        return new Layout(lines, x, y, width, height, anchor, side);
    }

    // The box's rectangle placed on the side of an anchor
    private static Rect2i place(Tutorial.Side side, Rect2i anchor, int width, int height, int gap) {
        int x = switch (side) {
            case LEFT -> anchor.getX() - width - gap;
            case RIGHT -> anchor.getX() + anchor.getWidth() + gap;
            default -> anchor.getX() + (anchor.getWidth() - width) / 2;
        };
        int y = switch (side) {
            case ABOVE -> anchor.getY() - height - gap;
            case BELOW -> anchor.getY() + anchor.getHeight() + gap;
            default -> anchor.getY() + (anchor.getHeight() - height) / 2;
        };
        return new Rect2i(x, y, width, height);
    }

    // The automatically chosen side, usually facing the middle of the window, falling back if that doesn't fit
    private static Tutorial.Side autoSide(Rect2i anchor, int width, int height, int gap) {
        Window window = Minecraft.getInstance().getWindow();
        int windowWidth = window.getGuiScaledWidth();
        int windowHeight = window.getGuiScaledHeight();

        // Distance from the center of the anchor to the center of the window
        float towardX = (windowWidth / 2F - (anchor.getX() + anchor.getWidth() / 2F)) / windowWidth;
        float towardY = (windowHeight / 2F - (anchor.getY() + anchor.getHeight() / 2F)) / windowHeight;
        Tutorial.Side horizontal = towardX > 0 ? Tutorial.Side.RIGHT : Tutorial.Side.LEFT;
        Tutorial.Side vertical = towardY > 0 ? Tutorial.Side.BELOW : Tutorial.Side.ABOVE; // when exactly at the middle, place above
        List<Tutorial.Side> order = Math.abs(towardX) > Math.abs(towardY)
                ? List.of(horizontal, horizontal.opposite(), vertical, vertical.opposite())
                : List.of(vertical, vertical.opposite(), horizontal, horizontal.opposite());

        for (Tutorial.Side side : order) {
            Rect2i box = place(side, anchor, width, height, gap);
            if (box.getX() >= 0 && box.getY() >= 0 && box.getX() + width <= windowWidth && box.getY() + height <= windowHeight) return side;
        }
        return order.get(0);
    }

    // Draws the line connecting a text box to its anchor between the two edges facing each other
    private static void drawConnectingLine(GuiGraphics graphics, Tutorial.TextBox textBox, Layout layout) {
        Rect2i anchor = layout.anchor();
        int anchorRight = anchor.getX() + anchor.getWidth();    // right anchor edge
        int anchorBottom = anchor.getY() + anchor.getHeight();  // bottom anchor edge

        // Each gap is positive if the box is on that side of the anchor
        int gapLeft = anchor.getX() - layout.right();
        int gapRight = layout.x() - anchorRight;
        int gapAbove = anchor.getY() - layout.bottom();
        int gapBelow = layout.y() - anchorBottom;
        boolean horizontallyClear = gapLeft > 0 || gapRight > 0;
        boolean verticallyClear = gapAbove > 0 || gapBelow > 0;
        if (!horizontallyClear && !verticallyClear) return; // if the anchor and text box overlap, don't draw lines at all

        // if the line connects horizontally or not
        boolean horizontal = switch (layout.side()) {
            case LEFT, RIGHT -> horizontallyClear;  // connect horizontally if there is a horizontal gap
            case ABOVE, BELOW -> !verticallyClear;  // connect horizontally only if there is no vertical gap
            default -> Math.max(gapLeft, gapRight) > Math.max(gapAbove, gapBelow); // centered: connect horizontally if the horizontal gap is greater
        };
        int lineWidth = textBox.lineWidth();
        float half = lineWidth / 2F; // half pixels round up and left when centering

        // The two straight parts run from their edge to the nearest side of the turn, which includes both corners
        if (horizontal) {
            boolean boxOnRight = gapRight > 0;
            int boxEdge = boxOnRight ? layout.x() + 1 : layout.right() - 1;             // the box's edge facing the anchor, past the dark outline
            int anchorEdge = boxOnRight ? anchorRight : anchor.getX();                  // the anchor's edge facing the box
            int boxRow = Mth.floor(layout.y() + layout.height() / 2F - half);           // top of the line leaving the box, at the box's midpoint
            int anchorRow = Mth.floor(anchor.getY() + anchor.getHeight() / 2F - half);  // top of the line reaching the anchor, at the anchor's midpoint
            int turn = Mth.floor((boxEdge + anchorEdge) / 2F - half);                   // left of the vertical part, halfway between the edges

            graphics.fill(turn, Math.min(boxRow, anchorRow), turn + lineWidth, Math.max(boxRow, anchorRow) + lineWidth, BORDER_TOP);                // vertical part
            graphics.fill(Math.min(boxEdge, turn + lineWidth), boxRow, Math.max(boxEdge, turn), boxRow + lineWidth, BORDER_TOP);                    // from the box to the turn
            graphics.fill(Math.min(anchorEdge, turn + lineWidth), anchorRow, Math.max(anchorEdge, turn), anchorRow + lineWidth, BORDER_TOP);        // from the turn to the anchor
        } else {
            boolean boxBelow = gapBelow > 0;
            int boxEdge = boxBelow ? layout.y() + 1 : layout.bottom() - 1;               // the box's edge facing the anchor, past the dark outline
            int anchorEdge = boxBelow ? anchorBottom : anchor.getY();                    // the anchor's edge facing the box
            int boxColumn = Mth.floor(layout.x() + layout.width() / 2F - half);          // left of the line leaving the box, at the box's midpoint
            int anchorColumn = Mth.floor(anchor.getX() + anchor.getWidth() / 2F - half); // left of the line reaching the anchor, at the anchor's midpoint
            int turn = Mth.floor((boxEdge + anchorEdge) / 2F - half);                    // top of the horizontal part, halfway between the edges

            graphics.fill(Math.min(boxColumn, anchorColumn), turn, Math.max(boxColumn, anchorColumn) + lineWidth, turn + lineWidth, BORDER_TOP);    // horizontal part
            graphics.fill(boxColumn, Math.min(boxEdge, turn + lineWidth), boxColumn + lineWidth, Math.max(boxEdge, turn), BORDER_TOP);              // from the box to the turn
            graphics.fill(anchorColumn, Math.min(anchorEdge, turn + lineWidth), anchorColumn + lineWidth, Math.max(anchorEdge, turn), BORDER_TOP);  // from the turn to the anchor
        }
    }

    // Draws the highlight, an outline just outside the anchor box
    private static void drawHighlight(GuiGraphics graphics, Tutorial.Highlight highlight) {
        if (!highlight.screens().isEmpty() && !TutorialScreens.matchesAny(highlight.screens(), Minecraft.getInstance().screen)) return;
        Rect2i anchor = TutorialAnchors.resolve(highlight.anchor());
        if (anchor == null || anchor.getWidth() == 0) return; // single-point anchors have nothing to outline
        int lineWidth = highlight.lineWidth();
        int anchorRight = anchor.getX() + anchor.getWidth();    // right anchor edge
        int anchorBottom = anchor.getY() + anchor.getHeight();  // bottom anchor edge
        int left = anchor.getX() - lineWidth;
        int top = anchor.getY() - lineWidth;
        int right = anchorRight + lineWidth;
        int bottom = anchorBottom + lineWidth;

        // drawn as 4 non-overlapping rectangles
        graphics.fill(left, top, right, anchor.getY(), BORDER_TOP);                  // top (and top corners)
        graphics.fill(left, anchorBottom, right, bottom, BORDER_TOP);                // bottom (and bottom corners)
        graphics.fill(left, anchor.getY(), anchor.getX(), anchorBottom, BORDER_TOP); // left
        graphics.fill(anchorRight, anchor.getY(), right, anchorBottom, BORDER_TOP);  // right
    }

    // Draws the frame, shaped like vanilla's tooltips with a 1 pixel rounded outline, with a two-color 1 pixel outline inside that
    private static void drawFrame(GuiGraphics graphics, Layout layout, int fill) {
        int left = layout.x(), top = layout.y(), right = layout.right(), bottom = layout.bottom();
        graphics.fill(left + 1, top, right - 1, top + 1, fill);
        graphics.fill(left, top + 1, right, bottom - 1, fill);
        graphics.fill(left + 1, bottom - 1, right - 1, bottom, fill);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, BORDER_TOP);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, BORDER_BOTTOM);
        graphics.fillGradient(left + 1, top + 2, left + 2, bottom - 2, BORDER_TOP, BORDER_BOTTOM);
        graphics.fillGradient(right - 2, top + 2, right - 1, bottom - 2, BORDER_TOP, BORDER_BOTTOM);
    }
}
