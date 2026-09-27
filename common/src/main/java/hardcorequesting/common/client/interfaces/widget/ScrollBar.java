package hardcorequesting.common.client.interfaces.widget;


import com.mojang.blaze3d.vertex.PoseStack;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.client.interfaces.WidgetSprites;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ScrollBar implements Drawable, Clickable {
    
    private static final int SCROLL_WIDTH = 7;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int SCROLL_BAR_HEIGHT = 6;
    
    private final int x;
    private final int y;
    private final int left;
    private final Size size;
    private final int length;

    private double scroll;
    private boolean isScrolling;
    private final GuiBase gui;
    
    public ScrollBar(GuiBase gui, Size size, int x, int y, int left) {
        this(gui, size, size.length, x, y, left);
    }

    public ScrollBar(GuiBase gui, int length, int x, int y, int left) {
        this(gui, Size.LONG, length, x, y, left);
    }

    private ScrollBar(GuiBase gui, Size size, int length, int x, int y, int left) {
        this.gui = gui;
        this.x = x;
        this.y = y;
        this.size = size;
        this.length = length;
        this.left = left;
    }
    
    @Environment(EnvType.CLIENT)
    public boolean isVisible() {
        return true;
    }
    
    @Environment(EnvType.CLIENT)
    protected void onUpdate() {
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public void render(GuiGraphics graphics, int mX, int mY) {
        if (isVisible()) {
            WidgetSprites sprites = this.gui.getWidgetSprites();
            // The scrollbar is drawn in two parts, inwards from the top and bottom of the sprite sheet to allow custom heights
            // Long and custom-length scrollbars use the screen's widget sprites, while the other scrollbar sizes use the theme's map texture
            int topHalf = length / 2;
            if (size == Size.LONG) {
                WidgetSprites.UV track = sprites.longTrack();
                this.gui.drawRect(graphics, sprites.texture(), x, y, track.u(), track.v(), SCROLL_WIDTH, topHalf, sprites.textureSize());
                this.gui.drawRect(graphics, sprites.texture(), x, y + topHalf, track.u(), track.v() + size.length - (length - topHalf), SCROLL_WIDTH, length - topHalf, sprites.textureSize());
            } else {
                this.gui.drawRect(graphics, gui.getMapTexture(), x, y, size.u, size.v, SCROLL_WIDTH, topHalf);
                this.gui.drawRect(graphics, gui.getMapTexture(), x, y + topHalf, size.u, size.v + size.length - (length - topHalf), SCROLL_WIDTH, length - topHalf);
            }
            this.gui.drawRect(graphics, sprites.texture(), x + 1, (int) (y + 1 + scroll), sprites.handle().u(), sprites.handle().v(), SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, sprites.textureSize());
        }
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public boolean onClick(int mX, int mY) {
        if (isVisible() && this.gui.inBounds(x, y, SCROLL_WIDTH, length, mX, mY)) {
            isScrolling = true;
            updateScroll(mY);
            return true;
        }
        return false;
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public boolean onDrag(int mX, int mY) {
        if (isVisible() && isScrolling) {
            updateScroll(mY);
            return true;
        }
        return false;
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    public boolean onRelease(int mX, int mY) {
        if (isVisible() && isScrolling) {
            updateScroll(mY);
            isScrolling = false;
            return true;
        }
        return false;
    }
    
    @Environment(EnvType.CLIENT)
    private void updateScroll(int mY) {
        setScroll(mY - y - SCROLL_BAR_HEIGHT / 2);
    }
    
    public float getScroll() {
        return (float) scroll / (length - SCROLL_BAR_HEIGHT - 2);
    }
    
    public <T> List<T> getVisibleEntries(List<T> list, int visibleEntries) {
        return getVisibleEntries(list, 1, visibleEntries);
    }
    
    public <T> List<T> getVisibleEntries(List<T> list, int columns, int visibleRows) {
        int rows = (int) Math.ceil((double) list.size() / (double) columns);
        int hiddenRows = rows - visibleRows;
        int start = columns * Math.max(0, Math.round(hiddenRows * this.getScroll()));
        int end = Math.min(list.size(), start + columns * visibleRows);
        return list.subList(start, end);
    }
    
    @Environment(EnvType.CLIENT)
    private void setScroll(double newScroll) {
        double old = scroll;
        scroll = newScroll;
        if (scroll < 0) {
            scroll = 0;
        } else if (scroll > length - SCROLL_BAR_HEIGHT - 2) {
            scroll = length - SCROLL_BAR_HEIGHT - 2;
        }
        if (scroll != old) {
            onUpdate();
        }
    }
    
    public void resetScroll() {
        scroll = 0;
    }
    
    @Environment(EnvType.CLIENT)
    public void onScroll(double mX, double mY, double scroll) {
        if (isVisible() && this.gui.inBounds(left, y, x + SCROLL_WIDTH - left, length, mX, mY)) {
            setScroll(this.scroll - scroll);
        }
    }
    
    public enum Size {
        TINY(242, 102, 29),
        SMALL(249, 102, 64),
        NORMAL(164, 69, 87),
        LONG(171, 69, 186);
        
        private final int u, v, length;
    
        Size(int u, int v, int length) {
            this.u = u;
            this.v = v;
            this.length = length;
        }
    }
}
