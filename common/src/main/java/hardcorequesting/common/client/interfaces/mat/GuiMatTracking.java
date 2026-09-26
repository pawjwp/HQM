package hardcorequesting.common.client.interfaces.mat;

import com.mojang.blaze3d.systems.RenderSystem;
import hardcorequesting.common.client.interfaces.BookTheme;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.client.interfaces.widget.LargeButton;
import hardcorequesting.common.client.interfaces.widget.SelectableList;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.items.mat.TrackedLocation;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The MAT's Tracking mode lists tracked locations on the left half and has details about the active one on the right.
 * Clicking a location toggles its tracking state. Certain details are not shown when SHOW_TRACKING_COORDINATES is disabled.
 */
@Environment(EnvType.CLIENT)
public class GuiMatTracking extends GuiBase {
    private static final int HEADER_Y = 20;               // heading y on both sides
    private static final int TEXT_Y = 35;                 // first line of text content on both sides
    private static final int LIST_X = 20;                 // x of the location heading
    private static final int LIST_WIDTH = 137;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 7;
    private static final int LIST_SCROLL_X = 159;
    private static final int SCROLL_Y = 30;
    private static final int SCROLL_LENGTH = 166;
    private static final int DIMENSION_X = 10;            // a dimension line's offset from its row
    private static final int DIMENSION_Y = 10;
    private static final int DELETE_X = 36;               // x of the delete button
    private static final int STOP_X = 95;                 // x of the stop tracking button
    private static final int BUTTON_Y = 200;              // y of both buttons
    private static final int DETAILS_X = 176;             // x of the tracked location's name and details
    private static final int DETAILS_WIDTH = 142;
    private static final int DETAIL_ROW_HEIGHT = 12;
    private static final int DETAIL_VALUE_GAP = 4;        // the minimum gap between a detail's label and value
    private static final int DISTANCE_ROUNDING = 50;      // when the distance is hidden, round to this many blocks

    private record Detail(Component label, Component value) {}

    private final Player player;
    private final MatTabBar tabBar;
    private final ResourceLocation background;
    private final SelectableList<TrackedLocation> locationList;
    private final LargeButton deleteButton;
    private final LargeButton stopButton;
    // The location that is pending deletion
    private TrackedLocation confirmingDelete;

    public GuiMatTracking(Player player) {
        super(CommonComponents.EMPTY);
        this.player = player;
        this.tabBar = new MatTabBar(MatMode.TRACKING, GuiQuestBook.TEXTURE_WIDTH);
        this.mapTexture = BookTheme.MAT.map;
        this.background = ResourceHelper.getResource(MatMode.TRACKING.getBackgroundName());

        this.locationList = new SelectableList<>(this, LIST_X, TEXT_Y, LIST_WIDTH, ROW_HEIGHT, VISIBLE_ROWS, LIST_SCROLL_X, SCROLL_Y, SCROLL_LENGTH) {
            @Override
            protected List<TrackedLocation> getEntries() {
                return matData().locations;
            }

            @Override
            protected boolean isSelected(TrackedLocation location) {
                return location == getTracked();
            }

            @Override
            protected void drawRow(GuiGraphics graphics, TrackedLocation location, int x, int y, int color) {
                drawString(graphics, trimToWidth(Component.literal(location.name()), LIST_WIDTH), x, y, color);
                drawString(graphics, displayName("dimension", location.dimension()), x + DIMENSION_X, y + DIMENSION_Y, 0.7F, HQMConfig.TEXT_HINT);
            }

            // Cut off text shows the full name of the location on hover
            @Override
            protected FormattedText getTooltip(TrackedLocation location) {
                if (getStringWidth(location.name()) > LIST_WIDTH) return Component.literal(location.name());
                return null;
            }

            @Override
            protected void onRowClicked(TrackedLocation location) {
                GeneralUsage.sendMatSelectLocation(indexOf(location));
            }
        };

        this.deleteButton = new LargeButton(this, "hqm.mat.tracking.delete", DELETE_X, BUTTON_Y) {
            @Override
            public boolean isEnabled() {
                return getTracked() != null;
            }

            // The first click asks for confirmation, a second click deletes it
            @Override
            public void onClick() {
                TrackedLocation tracked = getTracked();
                if (confirmingDelete == tracked) {
                    GeneralUsage.sendMatRemoveLocation(indexOf(tracked));
                    confirmingDelete = null;
                } else {
                    confirmingDelete = tracked;
                }
            }

            @Override
            protected FormattedText getName() {
                if (confirmingDelete != null && confirmingDelete == getTracked()) return Translator.translatable("hqm.mat.tracking.confirmDelete");
                return Translator.translatable("hqm.mat.tracking.delete");
            }
        };

        this.stopButton = new LargeButton(this, "hqm.mat.tracking.stop", STOP_X, BUTTON_Y) {
            @Override
            public boolean isEnabled() {
                return getTracked() != null;
            }

            // Selecting a tracked location again stops tracking it
            @Override
            public void onClick() {
                GeneralUsage.sendMatSelectLocation(matData().selectedLocation);
            }
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        left = (width - GuiQuestBook.TEXTURE_WIDTH) / 2;
        top = (height - GuiQuestBook.TEXTURE_HEIGHT) / 2;
        int x = mouseX - left;
        int y = mouseY - top;

        applyColor(0xFFFFFFFF);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(background, left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT,
                0, 0, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, BookTheme.MAT.sheetSize, BookTheme.MAT.sheetSize);

        renderLocations(graphics, x, y);
        renderDetails(graphics);

        tabBar.render(graphics, left, top, mouseX, mouseY);
        locationList.renderTooltip(graphics, x, y);
        deleteButton.renderTooltip(graphics, x, y);
        stopButton.renderTooltip(graphics, x, y);
        renderDetailTooltip(graphics, x, y);
        tabBar.renderTooltip(graphics, left, top, mouseX, mouseY);
    }

    // Draws the location list, its heading, and the buttons below it
    private void renderLocations(GuiGraphics graphics, int mX, int mY) {
        drawString(graphics, Translator.translatable("hqm.mat.tracking.locations"), LIST_X, HEADER_Y, HQMConfig.TEXT_NORMAL);
        if (matData().locations.isEmpty()) {
            drawString(graphics, Translator.translatable("hqm.mat.tracking.empty"), LIST_X, TEXT_Y, 0.7F, HQMConfig.TEXT_HINT);
        }
        locationList.render(graphics, mX, mY);
        deleteButton.render(graphics, mX, mY);
        stopButton.render(graphics, mX, mY);
    }

    // Draws the tracked location's name and details
    private void renderDetails(GuiGraphics graphics) {
        TrackedLocation location = getTracked();
        if (location == null) {
            drawString(graphics, getLinesFromText(Translator.translatable("hqm.mat.tracking.noneTracked"), 0.7F, DETAILS_WIDTH), DETAILS_X, TEXT_Y, 0.7F, HQMConfig.TEXT_HINT);
            return;
        }
        drawString(graphics, trimToWidth(Component.literal(location.name()), DETAILS_WIDTH), DETAILS_X, HEADER_Y, HQMConfig.TEXT_NORMAL);
        List<Detail> details = getDetails(location);
        for (int i = 0; i < details.size(); i++) {
            Detail detail = details.get(i);
            int rowY = TEXT_Y + i * DETAIL_ROW_HEIGHT;
            FormattedText value = trimToWidth(detail.value(), getValueWidth(detail));
            drawString(graphics, detail.label(), DETAILS_X, rowY, HQMConfig.TEXT_NORMAL);
            drawString(graphics, value, DETAILS_X + DETAILS_WIDTH - getStringWidth(value), rowY, HQMConfig.TEXT_HINT);
        }
    }

    // Cut off text shows the location's full name or the detail's full value on hover
    private void renderDetailTooltip(GuiGraphics graphics, int mX, int mY) {
        TrackedLocation location = getTracked();
        if (location == null) {
            return;
        }
        if (inBounds(DETAILS_X, HEADER_Y, DETAILS_WIDTH, DETAIL_ROW_HEIGHT, mX, mY) && getStringWidth(location.name()) > DETAILS_WIDTH) {
            renderTooltip(graphics, Component.literal(location.name()), mX + left, mY + top);
        }
        List<Detail> details = getDetails(location);
        for (int i = 0; i < details.size(); i++) {
            Detail detail = details.get(i);
            if (inBounds(DETAILS_X, TEXT_Y + i * DETAIL_ROW_HEIGHT, DETAILS_WIDTH, DETAIL_ROW_HEIGHT, mX, mY) && getStringWidth(detail.value()) > getValueWidth(detail)) {
                renderTooltip(graphics, detail.value(), mX + left, mY + top);
            }
        }
    }

    // The width available for a detail's value (minus the size of its label)
    private int getValueWidth(Detail detail) {
        return DETAILS_WIDTH - getStringWidth(detail.label()) - DETAIL_VALUE_GAP;
    }

    // The details including distance, coordinates, dimension, and biome
    private List<Detail> getDetails(TrackedLocation location) {
        List<Detail> details = new ArrayList<>();
        Level level = player.level();
        boolean sameDimension = level.dimension().location().equals(location.dimension());
        if (sameDimension) {
            details.add(new Detail(Translator.translatable("hqm.mat.tracking.distance"), distance(location.pos())));
        }
        if (HQMConfig.getInstance().MAT.SHOW_TRACKING_COORDINATES) {
            BlockPos pos = location.pos();
            details.add(new Detail(Translator.translatable("hqm.mat.tracking.coordinates"), Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())));
            if (!sameDimension) {
                // The location's dimension type is checked by dimension id
                double scale = level.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getOptional(location.dimension())
                        .map(type -> DimensionType.getTeleportationScale(type, level.dimensionType())).orElse(1D);
                // The scaled coordinates for dimensions with different scales from each other
                if (scale != 1) {
                    BlockPos scaled = BlockPos.containing(pos.getX() * scale, pos.getY(), pos.getZ() * scale);
                    details.add(new Detail(Translator.translatable("hqm.mat.tracking.inDimension", displayName("dimension", level.dimension().location())),
                            Component.literal(scaled.getX() + ", " + scaled.getZ())));
                }
            }
        }
        details.add(new Detail(Translator.translatable("hqm.mat.tracking.dimension"), displayName("dimension", location.dimension())));
        ResourceLocation biome = location.biome();
        if (biome != null) {
            details.add(new Detail(Translator.translatable("hqm.mat.tracking.biome"), displayName("biome", biome)));
        }
        return details;
    }

    // The horizontal distance, not counting vertical space, or the rounded version if applicable
    private Component distance(BlockPos pos) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        long distance = Math.round(Math.sqrt(dx * dx + dz * dz));
        if (HQMConfig.getInstance().MAT.SHOW_TRACKING_COORDINATES) {
            return Component.translatable("hqm.mat.tracking.distance.exact", distance);
        }
        return Component.translatable("hqm.mat.tracking.distance.rounded", Math.round((double) distance / DISTANCE_ROUNDING) * DISTANCE_ROUNDING);
    }

    // The translated name of a dimension or biome, formatted like "dimension.<mod_ame>.<dimension_name>"
    private static Component displayName(String type, ResourceLocation id) {
        String key = id.toLanguageKey(type);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        StringBuilder name = new StringBuilder();
        for (String word : id.getPath().split("[_/]")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.literal(name.toString());
    }

    @Nullable
    private TrackedLocation getTracked() {
        MatPlayerData mat = matData();
        if (mat.selectedLocation < 0 || mat.selectedLocation >= mat.locations.size()) {
            return null;
        }
        return mat.locations.get(mat.selectedLocation);
    }

    // The index of this entry, compared by identity as two locations have the same name/target
    private int indexOf(TrackedLocation location) {
        List<TrackedLocation> locations = matData().locations;
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i) == location) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar.mouseClicked(left, top, mouseX, mouseY)) {
            return true;
        }
        int x = (int) (mouseX - left);
        int y = (int) (mouseY - top);
        if (deleteButton.onClick(x, y) || stopButton.onClick(x, y) || locationList.onClick(x, y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (locationList.onDrag((int) (mouseX - left), (int) (mouseY - top))) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (locationList.onRelease((int) (mouseX - left), (int) (mouseY - top))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        locationList.onScroll(mouseX - left, mouseY - top, scroll);
        return true;
    }

    private MatPlayerData matData() {
        return QuestingDataManager.getInstance().getQuestingData(player).matData;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
