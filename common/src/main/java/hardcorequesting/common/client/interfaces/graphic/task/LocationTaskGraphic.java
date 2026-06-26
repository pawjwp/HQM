package hardcorequesting.common.client.interfaces.graphic.task;

import com.mojang.blaze3d.vertex.PoseStack;
import hardcorequesting.common.client.EditMode;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.edit.LocationMenu;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.quests.task.icon.VisitLocationTask;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class LocationTaskGraphic extends IconTaskGraphic<VisitLocationTask.Part> {
    
    // Cap to 3 lines to stay within bounds
    private static final int MAX_DETAIL_LINES = 3;

    private final VisitLocationTask task;
    
    public LocationTaskGraphic(VisitLocationTask task, UUID playerId, GuiQuestBook gui) {
        super(task, playerId, gui);
        this.task = task;
    }
    
    @Override
    protected void drawElementText(GuiGraphics graphics, VisitLocationTask.Part part, int index, int x, int y) {
        if (task.visited(index, playerId)) {
            gui.drawString(graphics, Translator.translatable("hqm.locationMenu.visited").withStyle(ChatFormatting.DARK_GREEN), x, y, 0.7F, HQMConfig.TEXT_NORMAL);
            return;
        }
        if (!part.getVisibility().doShowCoordinate()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean rightDim = part.getDimension().isEmpty() || Objects.equals(mc.level.dimension().location().toString(), part.getDimension());

        // Only the first lines are drawn, in order of priority
        List<FormattedText> lines = new ArrayList<>();
        if (part.getRadius() >= 0) {
            lines.add(Translator.plain("(" + part.getPosition().toShortString() + ")"));
            if (rightDim) {
                int distance = (int) mc.player.distanceToSqr(part.getPosition().getX() + 0.5, part.getPosition().getY() + 0.5, part.getPosition().getZ() + 0.5);
                FormattedText str = Translator.translatable("hqm.locationMenu.mAway", distance);
                if (part.getVisibility().doShowRadius()) {
                    str = FormattedText.composite(str, Translator.plain(" ["), Translator.translatable("hqm.locationMenu.mRadius", part.getRadius()), Translator.plain("]"));
                }
                lines.add(str);
            }
        }
        if (!rightDim) {
            lines.add(Translator.translatable("hqm.locationMenu.wrongDim"));
        }
        if (!part.getBiome().isEmpty()) {
            lines.add(Translator.translatable("hqm.locationMenu.biomeTarget", part.getBiome()));
        }
        if (!part.getStructure().isEmpty()) {
            lines.add(Translator.translatable("hqm.locationMenu.structureTarget", part.getStructure()));
        }

        for (int i = 0; i < Math.min(lines.size(), MAX_DETAIL_LINES); i++) {
            gui.drawString(graphics, lines.get(i), x, y + 6 * i, 0.7F, HQMConfig.TEXT_NORMAL);
        }
    }
    
    @Override
    protected boolean handleEditPartClick(EditMode mode, VisitLocationTask.Part part, int id) {
        if (mode == EditMode.LOCATION) {
            LocationMenu.display(gui, part.getVisibility(), part.getPosition(), part.getRadius(), part.getDimension(), part.getBiome(), part.getStructure(),
                    result -> task.setInfo(id, result.getVisibility(), result.getPos(), result.getRadius(), result.getDimension(), result.getBiome(), result.getStructure()));
            return true;
        } else {
            return super.handleEditPartClick(mode, part, id);
        }
    }
}