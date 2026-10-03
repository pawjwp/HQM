package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.tutorial.Tutorial;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

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

    @Nullable
    private static Tutorial tutorial;
    private static int stepIndex;
    private static boolean screenWasOpen; // if a screen_close step's screen was open during this step
    @Nullable
    private static Screen tickScreen; // the screen open last tick

    // A text box's lines and size on-screen, and the anchor it was placed against
    private record Layout(List<FormattedCharSequence> lines, int x, int y, int width, int height, Rect2i anchor) {
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
            if (Minecraft.getInstance().screen == null) render(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
        });
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, partialTick) -> render(graphics, mouseX, mouseY));

        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register((minecraft, screen, mouseX, mouseY, button) -> {
            if (tutorial == null || button != InputConstants.MOUSE_BUTTON_LEFT) return EventResult.pass();
            Tutorial.Trigger trigger = tutorial.steps().get(stepIndex).trigger();
            // Clicking the text box of a click step moves to the next step of the tutorial instead of clicking whatever is underneath
            if (trigger instanceof Tutorial.Trigger.ClickTextBox && isOverTextBox(mouseX, mouseY)) {
                advance();
                return EventResult.interruptFalse();
            }
            // Clicking an anchor moves to the next step and also clicks what's underneath
            if (trigger instanceof Tutorial.Trigger.ClickAnchor click && TutorialScreens.matchesAny(click.screens(), screen)
                    && isOverAnchor(click.anchors(), mouseX, mouseY)) {
                advance();
            }
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

        // Screen steps check the open screen each tick
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            tickScreen = minecraft.screen;
            if (tutorial == null) return;
            Tutorial.Trigger trigger = tutorial.steps().get(stepIndex).trigger();
            boolean open = TutorialScreens.matchesAny(trigger.screens(), minecraft.screen);
            if (trigger instanceof Tutorial.Trigger.ScreenOpen && open) {
                advance();
            } else if (trigger instanceof Tutorial.Trigger.ScreenClose) {
                if (open) screenWasOpen = true;
                else if (screenWasOpen) advance();
            }
        });
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> pause());
    }

    // Starts the tutorial at a step, replacing any that are already running, and saves that step on the server
    public static void start(Tutorial tutorial, int startStep) {
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
            for (String screen : step.trigger().screens()) {
                if (!TutorialScreens.isKnownScreen(screen)) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown screen %s in a trigger, so that step can't advance on it", tutorial.id(), screen);
                }
            }
            if (step.trigger() instanceof Tutorial.Trigger.Key key) {
                for (String name : key.keys()) {
                    if (!TutorialKeys.isKnownKey(name)) {
                        HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses an unknown key %s, so that step is unable to advance", tutorial.id(), name);
                    }
                }
            }
            if (step.trigger() instanceof Tutorial.Trigger.ClickAnchor click) {
                for (String anchor : click.anchors()) {
                    if (!TutorialAnchors.isContainerAnchor(anchor)) {
                        HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses %s in a click_anchor trigger, which is likely not actually clickable", tutorial.id(), anchor);
                    }
                }
            }
        }
        TutorialPlayer.tutorial = tutorial;
        screenWasOpen = false;
        stepIndex = startStep;
        if (stepIndex >= tutorial.steps().size()) stepIndex = 0;
        if (tutorial.steps().isEmpty()) pause(); // a tutorial without steps has nothing to show
        else GeneralUsage.sendMatTutorialProgress(tutorial.id(), stepIndex);
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

    // Moves to the next step and saves it on the server, completes the tutorial after the last step
    private static void advance() {
        stepIndex++;
        screenWasOpen = false;
        if (stepIndex < tutorial.steps().size()) {
            GeneralUsage.sendMatTutorialProgress(tutorial.id(), stepIndex);
        } else {
            GeneralUsage.sendMatTutorialCompleted(tutorial.id());
            pause();
        }
    }

    // Advances a key-triggered step when one of its keys is pressed while on the correct screen
    private static void keyPressed(InputConstants.Key pressed, @Nullable Screen screen) {
        if (tutorial == null) return;
        if (tutorial.steps().get(stepIndex).trigger() instanceof Tutorial.Trigger.Key key
                && TutorialScreens.matchesAny(key.screens(), screen) && TutorialKeys.matchesAny(key.keys(), pressed)) {
            advance();
        }
    }

    // Only allow clicking if the step's trigger is click
    private static boolean isClickStep() {
        return tutorial != null && tutorial.steps().get(stepIndex).trigger() instanceof Tutorial.Trigger.ClickTextBox;
    }

    private static void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tutorial == null) return;
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z);
        for (Tutorial.TextBox textBox : tutorial.steps().get(stepIndex).textBoxes()) {
            Layout layout = layout(textBox);
            if (layout == null) continue;
            drawFrame(graphics, layout, isClickStep() && layout.contains(mouseX, mouseY) ? HOVERED_FILL : FILL);
            if (textBox.line()) drawConnectingLine(graphics, textBox, layout);
            for (int i = 0; i < layout.lines().size(); i++) {
                graphics.drawString(font, layout.lines().get(i), layout.x() + FRAME, layout.y() + FRAME + i * font.lineHeight, TEXT_COLOR, false);
            }
        }
        graphics.pose().popPose();
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
        List<FormattedCharSequence> lines = font.split(FormattedText.of(textBox.text()), textBox.width());
        int width = lines.stream().mapToInt(font::width).max().orElse(0) + 2 * FRAME;
        int height = lines.size() * font.lineHeight + 2 * FRAME;

        int x = switch (textBox.side()) {
            case LEFT -> anchor.getX() - width;
            case RIGHT -> anchor.getX() + anchor.getWidth();
            default -> anchor.getX() + (anchor.getWidth() - width) / 2;
        };
        int y = switch (textBox.side()) {
            case ABOVE -> anchor.getY() - height;
            case BELOW -> anchor.getY() + anchor.getHeight();
            default -> anchor.getY() + (anchor.getHeight() - height) / 2;
        };

        return new Layout(lines, x + textBox.offsetX(), y + textBox.offsetY(), width, height, anchor);
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
        boolean horizontal = switch (textBox.side()) {
            case LEFT, RIGHT -> horizontallyClear;  // connect horizontally if there is a horizontal gap
            case ABOVE, BELOW -> !verticallyClear;  // connect horizontally only if there is no vertical gap
            case CENTER -> Math.max(gapLeft, gapRight) > Math.max(gapAbove, gapBelow); // connect horizontally if the horizontal gap is greater
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
