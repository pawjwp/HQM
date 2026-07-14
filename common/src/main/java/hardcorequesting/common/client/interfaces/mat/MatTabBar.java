package hardcorequesting.common.client.interfaces.mat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.network.GeneralUsage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

/*
 * The tab bar drawn on the top of each MAT screen
 * Each tab's width is dynamically determined based on the total tab bar width and the number of enabled tabs.
 * The active tab is drawn as a trapezoid, other tabs are drawn as parallelograms, slanted towards the active one.
 * The active tab has no fill because it opens up into the menu below it, where the texture below is the fill.
 * The inactive tabs' fill is drawn as a quad in the overlay color.
 * The edges of the tabs use textures from mat_tabs.png, including:
 * - A 12x12 region (top left) used for each tab's end cap, horizontally flipped when needed
 * - A 4x4 region (top right) stretched across the bottom and top of the tabs
 */
@Environment(EnvType.CLIENT)
public class MatTabBar {
    // Tab bar placement definitions
    private static final int BAR_WIDTH = 176; // total width of the tab bar
    private static final int TAB_HEIGHT = 12; // height of each tab
    private final int tabBarLeft, tabBarRight; // tab bar bounds

    // Sprite sheet definitions
    private static final ResourceLocation TAB_SHEET = ResourceHelper.getResource("mat_tabs");
    private static final int SHEET_SIZE = 16; // sprite sheet size
    private static final int END_CAP_SPRITE_U = 0; // start of the end cap sprite
    private static final int END_CAP_SPRITE_H = 12; // height of the end cap sprite
    private static final int END_CAP_SPRITE_W = 12; // width of the end cap sprite
    private static final int LINE_SPRITE_U = END_CAP_SPRITE_W; // start of the line sprite
    private static final int LINE_SPRITE_SIZE = 4; // size of the line sprite

    // End cap slant geometry definitions
    private static final int TAB_SLANT = END_CAP_SPRITE_W; // width of the tab's slant (TAB_HEIGHT/TAB_SLANT is the slope of the line)
    private static final int FILL_INSET = 2; // fill and hitbox inset from the tab's edges

    // Bottom line definitions
    private static final int BOTTOM_BAR_INNER_OVERSHOOT = 3; // distance the bottom bar extends into the active tab
    private static final int BOTTOM_BAR_OUTER_OVERSHOOT = -2; // distance the bottom bar extends to the outer edges of the tab bar

    // Interaction definitions
    private static final float INACTIVE_BRIGHTNESS = 0.45F; // color multiplier on an inactive tab's fill and lines
    private static final float HOVER_BRIGHTNESS = 0.75F; // color multiplier on an inactive tab's fill and lines when hovered

    // Active tab definitions
    private static final int ACTIVE_EXTRA_WIDTH = 3 * END_CAP_SPRITE_W / 2; // the active tab is widened to compensate for a smaller area from being a trapezoid

    private final MatMode active;
    private final List<MatMode> modes = MatMode.enabledModes();
    private final int activeIndex;

    public MatTabBar(MatMode active, int panelWidth) {
        this.active = active;
        this.activeIndex = Math.max(modes.indexOf(active), 0);
        this.tabBarLeft = (panelWidth - BAR_WIDTH) / 2;
        this.tabBarRight = tabBarLeft + BAR_WIDTH;
    }

    // Returns the boundary of a tab
    private int boundary(int boundaryIndex, int guiLeft) {
        float tabWidth = (BAR_WIDTH - ACTIVE_EXTRA_WIDTH) / (float) modes.size(); // width of a non-active tab
        float x = boundaryIndex * tabWidth + (boundaryIndex > activeIndex ? ACTIVE_EXTRA_WIDTH : 0);
        return guiLeft + tabBarLeft + Math.round(x);
    }

    public void render(GuiGraphics graphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int tabCount = modes.size();
        if (tabCount == 0) return;
        int hovered = tabIndexAt(guiLeft, guiTop, mouseX, mouseY);
        int fillTopSlant = TAB_HEIGHT - FILL_INSET; // horizontal slant offset at the fill's top edge

        // Start by drawing the tabs' inner fill
        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
            int left = boundary(tabIndex, guiLeft), right = boundary(tabIndex + 1, guiLeft);
            int fillColor = shade(modes.get(tabIndex).getOverlayColor(), brightness(tabIndex, hovered));
            int fillTop = guiTop + FILL_INSET, fillBottom = guiTop + fillTopSlant;
            // Right-leaning parallelogram
            if (tabIndex < activeIndex) {
                fillQuad(graphics, left + fillTopSlant, fillTop, right + fillTopSlant, fillTop,
                        right + FILL_INSET, fillBottom, left + FILL_INSET, fillBottom, fillColor);
            }
            // Left-leaning parallelogram
            else if (tabIndex > activeIndex) {
                fillQuad(graphics, left - fillTopSlant, fillTop, right - fillTopSlant, fillTop,
                        right - FILL_INSET, fillBottom, left - FILL_INSET, fillBottom, fillColor);
            }
            // Trapezoid in the center
            /*
            else {
                fillQuad(graphics, left + fillTopSlant, fillTop, right - fillTopSlant, fillTop,
                        right - FILL_INSET, fillBottom, left + FILL_INSET, fillBottom, fillColor);
            }*/
        }

        // Draw the tabs' top lines
        int innerTrim = LINE_SPRITE_SIZE / 2; // trim an inactive tab's inner edge by half the line width
        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
            int left = boundary(tabIndex, guiLeft), right = boundary(tabIndex + 1, guiLeft);
            int color = shade(modes.get(tabIndex).getBaseColor(), brightness(tabIndex, hovered));
            if (tabIndex < activeIndex)      drawLine(graphics, left + TAB_SLANT, guiTop, right + TAB_SLANT - innerTrim, color); // leaning right (/)
            else if (tabIndex > activeIndex) drawLine(graphics, left - TAB_SLANT + innerTrim, guiTop, right - TAB_SLANT, color); // leaning left (\)
            else                                 drawLine(graphics, left + TAB_SLANT, guiTop, right - TAB_SLANT, color); // active trapezoid
        }

        // Draw the endcaps going from the outside-in so tabs closer to the active one are on top
        for (int boundaryIndex = 0; boundaryIndex <= activeIndex; boundaryIndex++) drawEndCap(graphics, boundaryIndex, guiLeft, guiTop, hovered); // left of active
        for (int boundaryIndex = tabCount; boundaryIndex > activeIndex; boundaryIndex--) drawEndCap(graphics, boundaryIndex, guiLeft, guiTop, hovered); // right of active

        // Draw the bottom bar on either side of the active tab
        int barY = guiTop + TAB_HEIGHT - LINE_SPRITE_SIZE;
        int barColor = shade(active.getBaseColor(), 1F);
        drawLine(graphics, guiLeft + tabBarLeft - BOTTOM_BAR_OUTER_OVERSHOOT, barY, boundary(activeIndex, guiLeft) + BOTTOM_BAR_INNER_OVERSHOOT, barColor);
        drawLine(graphics, boundary(activeIndex + 1, guiLeft) - BOTTOM_BAR_INNER_OVERSHOOT, barY, guiLeft + tabBarRight + BOTTOM_BAR_OUTER_OVERSHOOT, barColor);
    }

    private void drawEndCap(GuiGraphics graphics, int boundaryIndex, int guiLeft, int tabBarTop, int hovered) {
        boolean leftSlant = boundaryIndex <= activeIndex; // '/' up to and including the active tab's left edge
        int owner = leftSlant ? boundaryIndex : boundaryIndex - 1; // the tab nearer the active one owns the end cap's color
        int color = shade(modes.get(owner).getBaseColor(), brightness(owner, hovered));
        int bottomX = boundary(boundaryIndex, guiLeft);
        if (leftSlant) sprite(graphics, bottomX, tabBarTop, TAB_SLANT, TAB_HEIGHT,
                END_CAP_SPRITE_U, END_CAP_SPRITE_W, END_CAP_SPRITE_H, color, false);
        else sprite(graphics, bottomX - TAB_SLANT, tabBarTop, TAB_SLANT, TAB_HEIGHT,
                END_CAP_SPRITE_U, END_CAP_SPRITE_W, END_CAP_SPRITE_H, color, true);
    }

    // Draws a horizontal line (stretched line sprite) from the start to end positions in the specified color
    private void drawLine(GuiGraphics graphics, int startX, int y, int endX, int argb) {
        if (endX > startX) sprite(graphics, startX, y, endX - startX, LINE_SPRITE_SIZE, LINE_SPRITE_U, LINE_SPRITE_SIZE, LINE_SPRITE_SIZE, argb, false);
    }

    public void renderTooltip(GuiGraphics graphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int hovered = tabIndexAt(guiLeft, guiTop, mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(modes.get(hovered).getNameKey()), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(int guiLeft, int guiTop, double mouseX, double mouseY) {
        int index = tabIndexAt(guiLeft, guiTop, (int) mouseX, (int) mouseY);
        if (index < 0) return false;
        MatMode mode = modes.get(index);
        if (mode != active) {
            GeneralUsage.sendOpenMatMode(mode, HQMConfig.getInstance().MAT.TABS_SWITCH_DEFAULT_MODE);
        }
        return true;
    }

    private int tabIndexAt(int guiLeft, int guiTop, int mouseX, int mouseY) {
        if (mouseY < guiTop + FILL_INSET || mouseY >= guiTop + TAB_HEIGHT - FILL_INSET) return -1;
        float heightFraction = (guiTop + TAB_HEIGHT - mouseY) / (float) TAB_HEIGHT; // 0 at the bottom edge, 1 at the top
        for (int tabIndex = 0; tabIndex < modes.size(); tabIndex++) {
            if (mouseX >= boundaryAt(tabIndex, heightFraction, guiLeft) && mouseX < boundaryAt(tabIndex + 1, heightFraction, guiLeft)) return tabIndex;
        }
        return -1;
    }

    private float boundaryAt(int boundaryIndex, float heightFraction, int guiLeft) {
        return boundary(boundaryIndex, guiLeft) + heightFraction * (boundaryIndex <= activeIndex ? TAB_SLANT : -TAB_SLANT);
    }

    // Returns brightness of the tab based on if it is active, hovered, or inactive
    private float brightness(int tabIndex, int hovered) {
        if (tabIndex == activeIndex) return 1F;
        return tabIndex == hovered ? HOVER_BRIGHTNESS : INACTIVE_BRIGHTNESS;
    }

    // Multiplies each channel by the brightness value, darkening it to the specified percentage
    private static int shade(int rgb, float brightness) {
        int r = (int) (((rgb >> 16) & 0xFF) * brightness);
        int g = (int) (((rgb >> 8) & 0xFF) * brightness);
        int b = (int) ((rgb & 0xFF) * brightness);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // Draw a sprite at a given location, stretched, tinted, and mirrored as needed
    private static void sprite(GuiGraphics graphics, float x, float y, float w, float h, int u, int spriteWidth, int spriteHeight, int argb, boolean flipH) {
        float u0 = u / (float) SHEET_SIZE, u1 = (u + spriteWidth) / (float) SHEET_SIZE;
        float v0 = 0F, v1 = spriteHeight / (float) SHEET_SIZE;
        if (flipH) { float t = u0; u0 = u1; u1 = t; }
        float a = ((argb >> 24) & 0xFF) / 255F, r = ((argb >> 16) & 0xFF) / 255F;
        float g = ((argb >> 8) & 0xFF) / 255F, b = (argb & 0xFF) / 255F;
        // Allow transparent pixels
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.setShaderTexture(0, TAB_SHEET);
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, x, y + h, 0).uv(u0, v1).endVertex();
        builder.vertex(matrix, x + w, y + h, 0).uv(u1, v1).endVertex();
        builder.vertex(matrix, x + w, y, 0).uv(u1, v0).endVertex();
        builder.vertex(matrix, x, y, 0).uv(u0, v0).endVertex();
        tesselator.end();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    // Draw a quad given 4 coordinate pairs
    private static void fillQuad(GuiGraphics graphics, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255F, r = ((argb >> 16) & 0xFF) / 255F;
        float g = ((argb >> 8) & 0xFF) / 255F, b = (argb & 0xFF) / 255F;
        // Allow transparent pixels
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        // Counter-clockwise
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x4, y4, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x3, y3, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        tesselator.end();
    }
}