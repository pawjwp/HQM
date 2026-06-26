package hardcorequesting.common.client.interfaces.edit;

import com.mojang.blaze3d.vertex.PoseStack;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.widget.ArrowSelectionHelper;
import hardcorequesting.common.client.interfaces.widget.LargeButton;
import hardcorequesting.common.client.interfaces.widget.NumberTextBox;
import hardcorequesting.common.client.interfaces.widget.TextBox;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.quests.task.icon.VisitLocationTask;
import hardcorequesting.common.util.HQMUtil;
import hardcorequesting.common.util.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LocationMenu extends GuiEditMenu {
    
    private final Consumer<Result> resultConsumer;
    private VisitLocationTask.Visibility visibility;
    private final BlockPos.MutableBlockPos pos;
    private int radius;
    private String dimension;
    private String biome;
    private String structure;

    public static void display(GuiQuestBook gui, VisitLocationTask.Visibility visibility, BlockPos initPos, int initRadius, String initDimension, String initBiome, String initStructure, Consumer<Result> resultConsumer) {
        gui.setEditMenu(new LocationMenu(gui, visibility, initPos, initRadius, initDimension, initBiome, initStructure, resultConsumer));
    }

    private LocationMenu(GuiQuestBook gui, VisitLocationTask.Visibility visibilityIn, BlockPos initPos, int initRadius, String initDimension, String initBiome, String initStructure, Consumer<Result> resultConsumer) {
        super(gui, true);
    
        this.resultConsumer = resultConsumer;
        this.visibility = visibilityIn;
        this.pos = new BlockPos.MutableBlockPos(initPos.getX(), initPos.getY(), initPos.getZ());
        this.radius = initRadius;
        this.dimension = initDimension;
        this.biome = initBiome;
        this.structure = initStructure;

        addTextBox(new NumberTextBox(gui, 20, 30, Translator.translatable("hqm.locationMenu.xTarget"), true, pos::getX, pos::setX));
        
        addTextBox(new NumberTextBox(gui, 20, 30 + BOX_OFFSET, Translator.translatable("hqm.locationMenu.yTarget"), true, pos::getY, pos::setY));
        
        addTextBox(new NumberTextBox(gui, 20, 30 + 2 * BOX_OFFSET, Translator.translatable("hqm.locationMenu.zTarget"), true, pos::getZ, pos::setZ));

        addTextBox(new NumberTextBox(gui, 20, 30 + 3 * BOX_OFFSET, Translator.translatable("hqm.locationMenu.radius"), true, () -> radius, value -> radius = value) {
            @Override
            protected void draw(GuiGraphics graphics, boolean selected, int mX, int mY) {
                super.draw(graphics, selected, mX, mY);
    
                this.gui.drawString(graphics, this.gui.getLinesFromText(Translator.translatable("hqm.locationMenu.negRadius"), 0.7F, 130), x, y + BOX_OFFSET + TEXT_OFFSET, 0.7F, HQMConfig.TEXT_NORMAL);
            }
        });

        addTextBox(new LabeledBox(gui, "hqm.locationMenu.dim", 180, 30 + 2 * BOX_OFFSET, () -> dimension, value -> dimension = value));

        addTextBox(new LabeledBox(gui, "hqm.locationMenu.biome", 180, 30 + 3 * BOX_OFFSET, () -> biome, value -> biome = value));

        addTextBox(new LabeledBox(gui, "hqm.locationMenu.structure", 180, 30 + 4 * BOX_OFFSET, () -> structure, value -> structure = value));

        addClickable(new LargeButton(gui, "hqm.locationMenu.location", 100, 20) {
            @Override
            public void onClick() {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    pos.set(player.getX(), player.getY(), player.getZ());
                    dimension = player.level().dimension().location().toString();
                    biome = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("");
                    reloadTextBoxes();
                }
            }
        });
        
        addClickable(new ArrowSelectionHelper(gui, 180, 30) {
            @Override
            protected void onArrowClick(boolean left) {
                if (left) {
                    visibility = HQMUtil.cyclePrev(VisitLocationTask.Visibility.values(), visibility);
                } else {
                    visibility = HQMUtil.cycleNext(VisitLocationTask.Visibility.values(), visibility);
                }
            }
    
            @Override
            protected FormattedText getArrowText() {
                return visibility.getName();
            }
    
            @Override
            protected FormattedText getArrowDescription() {
                return visibility.getDescription();
            }
        });
    }
    
    @Override
    public void save() {
        resultConsumer.accept(new Result(visibility, pos.immutable(), radius, dimension, biome, structure));
    }

    private static class LabeledBox extends TextBox {
        private final String label;
        private final Supplier<String> getter;
        private final Consumer<String> setter;

        LabeledBox(GuiQuestBook gui, String label, int x, int y, Supplier<String> getter, Consumer<String> setter) {
            super(gui, getter.get(), x, y, true);
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void textChanged() {
            super.textChanged();
            setter.accept(getText());
        }

        @Override
        protected void draw(GuiGraphics graphics, boolean selected, int mX, int mY) {
            super.draw(graphics, selected, mX, mY);
            this.gui.drawString(graphics, Translator.translatable(label), x, y + NumberTextBox.TEXT_OFFSET, HQMConfig.TEXT_NORMAL);
        }

        @Override
        public void reloadText() {
            setTextAndCursor(getter.get());
        }
    }

    public static class Result {
        private final VisitLocationTask.Visibility visibility;
        private final BlockPos pos;
        private final int radius;
        private final String dimension;
        private final String biome;
        private final String structure;

        private Result(VisitLocationTask.Visibility visibility, BlockPos pos, int radius, String dimension, String biome, String structure) {
            this.visibility = visibility;
            this.pos = pos;
            this.radius = radius;
            this.dimension = dimension;
            this.biome = biome;
            this.structure = structure;
        }
    
        public VisitLocationTask.Visibility getVisibility() {
            return visibility;
        }
    
        public BlockPos getPos() {
            return pos;
        }
    
        public int getRadius() {
            return radius;
        }
    
        public String getDimension() {
            return dimension;
        }

        public String getBiome() {
            return biome;
        }

        public String getStructure() {
            return structure;
        }
    }
}
