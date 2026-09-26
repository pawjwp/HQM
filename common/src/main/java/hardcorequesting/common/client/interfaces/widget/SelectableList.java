package hardcorequesting.common.client.interfaces.widget;

import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.config.HQMConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

/**
 * A scrolling list of rows with a few states; normal, hovered, selected, and selected-hovered.
 */
public abstract class SelectableList<T> implements Drawable, Clickable {
    private final int x;
    private final int y;
    private final int width;
    private final int rowHeight;
    private final GuiBase gui;
    private final ExtendedScrollBar<T> scrollBar;

    public SelectableList(GuiBase gui, int x, int y, int width, int rowHeight, int visibleRows, int scrollBarX, int scrollBarY, int scrollBarLength) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.rowHeight = rowHeight;
        this.gui = gui;
        this.scrollBar = new ExtendedScrollBar<>(gui, scrollBarLength, scrollBarX, scrollBarY, x, visibleRows, () -> getEntries());
    }

    @Override
    public void render(GuiGraphics graphics, int mX, int mY) {
        List<T> visible = scrollBar.getVisibleEntries();
        for (int i = 0; i < visible.size(); i++) {
            T entry = visible.get(i);
            int rowY = y + i * rowHeight;
            boolean hover = gui.inBounds(x, rowY, width, rowHeight, mX, mY);
            drawRow(graphics, entry, x, rowY, getColor(isSelected(entry), hover));
        }
        scrollBar.render(graphics, mX, mY);
    }

    @Override
    public boolean onClick(int mX, int mY) {
        if (scrollBar.onClick(mX, mY)) {
            return true;
        }
        T entry = getHoveredEntry(mX, mY);
        if (entry != null) {
            onRowClicked(entry);
            return true;
        }
        return false;
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int mX, int mY) {
        T entry = getHoveredEntry(mX, mY);
        if (entry != null) {
            FormattedText tooltip = getTooltip(entry);
            if (tooltip != null) {
                gui.renderTooltip(graphics, tooltip, mX + gui.getLeft(), mY + gui.getTop());
            }
        }
    }

    @Override
    public boolean onDrag(int mX, int mY) {
        return scrollBar.onDrag(mX, mY);
    }

    @Override
    public boolean onRelease(int mX, int mY) {
        return scrollBar.onRelease(mX, mY);
    }

    public void onScroll(double mX, double mY, double scroll) {
        scrollBar.onScroll(mX, mY, scroll);
    }

    // The entry under the cursor, null if there is none
    private T getHoveredEntry(int mX, int mY) {
        List<T> visible = scrollBar.getVisibleEntries();
        for (int i = 0; i < visible.size(); i++) {
            if (gui.inBounds(x, y + i * rowHeight, width, rowHeight, mX, mY)) {
                return visible.get(i);
            }
        }
        return null;
    }

    // Tooltip shown while an entry's row is hovered
    protected FormattedText getTooltip(T entry) {
        return null;
    }

    private static int getColor(boolean selected, boolean hover) {
        if (selected && hover) return HQMConfig.TEXT_SELECTED_HOVERED;
        if (selected) return HQMConfig.TEXT_SELECTED;
        if (hover) return HQMConfig.TEXT_HOVERED;
        return HQMConfig.TEXT_NORMAL;
    }

    protected abstract List<T> getEntries();

    protected abstract boolean isSelected(T entry);

    // Draws a row's contents with its top-left corner at (x, y), in the given color
    protected abstract void drawRow(GuiGraphics graphics, T entry, int x, int y, int color);

    protected abstract void onRowClicked(T entry);
}
