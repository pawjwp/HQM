package hardcorequesting.common.client.interfaces.edit;

import com.mojang.blaze3d.systems.RenderSystem;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.client.interfaces.widget.ExtendedScrollBar;
import hardcorequesting.common.client.interfaces.widget.ScrollBar;
import hardcorequesting.common.client.interfaces.widget.TextBox;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.quests.ItemPrecision;
import hardcorequesting.common.util.Translator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

// A menu to pick the specific tag on an item task with tag precision.
// Includes a list with all tags the selected item belongs to.
public class ItemTagMenu extends GuiEditMenu {

    private static final int START_X = 20;
    private static final int START_Y = 20;
    private static final int OFFSET_Y = 8;
    private static final int VISIBLE_TAGS = 20;
    private static final int LIST_WIDTH = 130;
    private static final int INFO_X = 180;
    private static final int CYCLE_Y = 140;
    private static final int CLEAR_Y = 160;
    private static final int PREVIEW_CYCLE = 1000;

    private final BiConsumer<TagKey<Item>, Boolean> resultConsumer;
    private final ItemStack stack;
    private final ItemPrecision precision;

    private final List<String> allTags;
    private final List<String> shownTags;
    private final ExtendedScrollBar<String> scrollBar;

    private String selected;
    private boolean cycling;
    private boolean cycleTouched;

    public static void display(GuiQuestBook gui, ItemStack stack, TagKey<Item> currentTag, ItemPrecision precision, Boolean cycleOverride, BiConsumer<TagKey<Item>, Boolean> resultConsumer) {
        gui.setEditMenu(new ItemTagMenu(gui, stack, currentTag, precision, cycleOverride, resultConsumer));
    }

    private ItemTagMenu(GuiQuestBook gui, ItemStack stack, TagKey<Item> currentTag, ItemPrecision precision, Boolean cycleOverride, BiConsumer<TagKey<Item>, Boolean> resultConsumer) {
        super(gui, false);
        this.resultConsumer = resultConsumer;
        this.stack = stack;
        this.precision = precision;
        this.cycleTouched = cycleOverride != null;
        this.cycling = cycleOverride != null && cycleOverride;
        this.selected = currentTag == null ? null : currentTag.location().toString();

        allTags = stack.getTags().map(tag -> tag.location().toString()).sorted().toList();
        shownTags = new ArrayList<>(allTags);

        addScrollBar(scrollBar = new ExtendedScrollBar<>(gui, ScrollBar.Size.LONG, 160, 18, START_X,
                VISIBLE_TAGS, () -> ItemTagMenu.this.shownTags));

        addTextBox(new TextBox(gui, "", 250, 18, false) {
            @Override
            public void textChanged() {
                super.textChanged();
                updateTags(getText());
            }
        });
    }

    private void updateTags(String search) {
        shownTags.clear();
        for (String tag : allTags) {
            if (tag.toLowerCase().contains(search.toLowerCase())) {
                shownTags.add(tag);
            }
        }
        Collections.sort(shownTags);
    }

    private boolean isTagPrecision() {
        return precision == ItemPrecision.TAG_FUZZY || precision == ItemPrecision.TAG_NBT_FUZZY;
    }

    private boolean effectiveCycling() {
        return !cycleTouched || cycling;
    }

    private List<ItemStack> previewItems(String tag) {
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, new ResourceLocation(tag)))
                .map(holders -> holders.stream().map(holder -> new ItemStack(holder.value())).toList())
                .orElse(Collections.emptyList());
    }

    @Override
    public void draw(GuiGraphics graphics, int mX, int mY) {
        super.draw(graphics, mX, mY);

        ResourceHelper.bindResource(GuiQuestBook.MAP_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int nameY = START_Y;
        for (String tag : scrollBar.getVisibleEntries()) {
            boolean isSelected = tag.equals(selected);
            boolean inBounds = gui.inBounds(START_X, nameY, LIST_WIDTH, 6, mX, mY);
            gui.drawString(graphics, Translator.plain(tag), START_X, nameY, 0.7F,
                    isSelected ? inBounds ? HQMConfig.TEXT_SELECTED_HOVERED : HQMConfig.TEXT_SELECTED : inBounds ? HQMConfig.TEXT_HOVERED : HQMConfig.TEXT_NORMAL);
            nameY += OFFSET_Y;
        }
        if (allTags.isEmpty()) {
            gui.drawString(graphics, Translator.translatable("hqm.itemTagMenu.noTags").withStyle(ChatFormatting.GRAY), START_X, START_Y, 0.7F, HQMConfig.TEXT_NORMAL);
        }

        gui.drawString(graphics, Translator.translatable("hqm.itemTagMenu.search"), INFO_X, 20, HQMConfig.TEXT_NORMAL);
        gui.drawItemStack(graphics, stack, INFO_X, 38, mX, mY, false);

        gui.drawString(graphics, Translator.translatable("hqm.itemTagMenu.selectedTag"), INFO_X, 62, HQMConfig.TEXT_NORMAL);
        if (selected != null) {
            gui.drawString(graphics, Translator.plain(selected), INFO_X, 72, 0.7F, HQMConfig.TEXT_NORMAL);
            List<ItemStack> preview = previewItems(selected);
            if (!preview.isEmpty()) {
                ItemStack shown = preview.get((int) (System.currentTimeMillis() / PREVIEW_CYCLE % preview.size()));
                gui.drawItemStack(graphics, shown, INFO_X + LIST_WIDTH - 16, 60, mX, mY, false);
            }
        } else {
            gui.drawString(graphics, Translator.translatable("hqm.itemTagMenu.default"), INFO_X, 72, 0.7F, HQMConfig.TEXT_NORMAL);
        }

        gui.drawString(graphics, Translator.translatable(effectiveCycling() ? "hqm.itemTagMenu.cycleOn" : "hqm.itemTagMenu.cycleOff").withStyle(gui.inBounds(INFO_X, CYCLE_Y, LIST_WIDTH, 8, mX, mY) ? ChatFormatting.WHITE : ChatFormatting.GRAY), INFO_X, CYCLE_Y, 0.7F, HQMConfig.TEXT_NORMAL);

        gui.drawString(graphics, Translator.translatable("hqm.itemTagMenu.clear").withStyle(gui.inBounds(INFO_X, CLEAR_Y, LIST_WIDTH, 8, mX, mY) ? ChatFormatting.WHITE : ChatFormatting.GRAY), INFO_X, CLEAR_Y, 0.7F, HQMConfig.TEXT_NORMAL);

        if (!isTagPrecision()) {
            List<FormattedText> hint = gui.getLinesFromText(Translator.translatable("hqm.itemTagMenu.notTagPrecision"), 0.7F, LIST_WIDTH);
            gui.drawString(graphics, hint, INFO_X, 100, 0.7F, ChatFormatting.RED.getColor());
        }
    }

    @Override
    public void onClick(int mX, int mY, int b) {
        super.onClick(mX, mY, b);

        int nameY = START_Y;
        for (String tag : scrollBar.getVisibleEntries()) {
            if (gui.inBounds(START_X, nameY, LIST_WIDTH, 6, mX, mY)) {
                selected = tag.equals(selected) ? null : tag;
                return;
            }
            nameY += OFFSET_Y;
        }

        if (gui.inBounds(INFO_X, CYCLE_Y, LIST_WIDTH, 8, mX, mY)) {
            cycling = !effectiveCycling();
            cycleTouched = true;
        } else if (gui.inBounds(INFO_X, CLEAR_Y, LIST_WIDTH, 8, mX, mY)) {
            selected = null;
        }
    }

    @Override
    public void save() {
        resultConsumer.accept(selected == null ? null : TagKey.create(Registries.ITEM, new ResourceLocation(selected)), effectiveCycling());
    }
}
