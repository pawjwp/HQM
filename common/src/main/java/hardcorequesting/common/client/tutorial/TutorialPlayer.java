package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.tutorial.Tutorial;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

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

    // A text box's lines and size on-screen
    private record Layout(List<FormattedCharSequence> lines, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public static void register() {
        // Render text boxes on screen during gameplay
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTick) -> {
            if (Minecraft.getInstance().screen == null) render(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
        });
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, partialTick) -> render(graphics, mouseX, mouseY));

        // Clicking the text box moves to the next step of the tutorial instead of clicking whatever is underneath
        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register((minecraft, screen, mouseX, mouseY, button) -> {
            if (tutorial == null || button != InputConstants.MOUSE_BUTTON_LEFT || !isOverTextBox(mouseX, mouseY)) return EventResult.pass();
            if (++stepIndex >= tutorial.steps().size()) pause();
            return EventResult.interruptFalse();
        });
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> pause());
    }

    // Starts the tutorial at its first step, replacing any that are already running
    public static void start(Tutorial tutorial) {
        for (Tutorial.Step step : tutorial.steps()) {
            for (Tutorial.TextBox textBox : step.textBoxes()) {
                if (TutorialAnchors.resolve(textBox.anchor()) == null) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s uses the unknown anchor %s, so that text box is hidden", tutorial.id(), textBox.anchor());
                }
            }
        }
        TutorialPlayer.tutorial = tutorial;
        stepIndex = 0;
        if (tutorial.steps().isEmpty()) pause(); // a tutorial without steps has nothing to show
    }

    public static void pause() {
        // removes the active tutorial, will add progress saving later
        tutorial = null;
    }

    @Nullable
    public static String getPlayingId() {
        if (tutorial == null) return null;
        return tutorial.id();
    }

    private static void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tutorial == null) return;
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z);
        for (Tutorial.TextBox textBox : tutorial.steps().get(stepIndex).textBoxes()) {
            Layout layout = layout(textBox);
            if (layout == null) continue;
            drawFrame(graphics, layout, layout.contains(mouseX, mouseY) ? HOVERED_FILL : FILL);
            for (int i = 0; i < layout.lines().size(); i++) {
                graphics.drawString(font, layout.lines().get(i), layout.x() + FRAME, layout.y() + FRAME + i * font.lineHeight, TEXT_COLOR, false);
            }
        }
        graphics.pose().popPose();
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
        Rect2i anchor = TutorialAnchors.resolve(textBox.anchor());
        if (anchor == null) return null;
        Font font = Minecraft.getInstance().font;
        
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

        return new Layout(lines, x + textBox.offsetX(), y + textBox.offsetY(), width, height);
    }

    // Draws the frame, shaped like vanilla's tooltips with a 1 pixel rounded outline, with a two-color 1 pixel outline inside that
    private static void drawFrame(GuiGraphics graphics, Layout layout, int fill) {
        int left = layout.x(), top = layout.y(), right = left + layout.width(), bottom = top + layout.height();
        graphics.fill(left + 1, top, right - 1, top + 1, fill);
        graphics.fill(left, top + 1, right, bottom - 1, fill);
        graphics.fill(left + 1, bottom - 1, right - 1, bottom, fill);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, BORDER_TOP);
        graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, BORDER_BOTTOM);
        graphics.fillGradient(left + 1, top + 2, left + 2, bottom - 2, BORDER_TOP, BORDER_BOTTOM);
        graphics.fillGradient(right - 2, top + 2, right - 1, bottom - 2, BORDER_TOP, BORDER_BOTTOM);
    }
}
