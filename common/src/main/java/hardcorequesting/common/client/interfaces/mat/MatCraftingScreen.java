package hardcorequesting.common.client.interfaces.mat;

import com.mojang.blaze3d.systems.RenderSystem;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

// The MAT's crafting mode, an extension of CraftingScreen with the texture replaced and the tab bar added on top
@Environment(EnvType.CLIENT)
public class MatCraftingScreen extends CraftingScreen {
    private static final ResourceLocation BG_TEXTURE = ResourceHelper.getResource(MatMode.CRAFTING.getBackgroundName());
    private static final int PANEL_W = 176, PANEL_H = 200;
    private static final int SHEET_SIZE = 256;
    // Distance from the panel top to the start of the vanilla screen
    private static final int HEADER_H = 34;

    public MatCraftingScreen(CraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private int panelLeft() {
        return leftPos - (PANEL_W - imageWidth) / 2;
    }

    private int panelTop() {
        return topPos - HEADER_H;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Allow transparent pixels
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BG_TEXTURE, panelLeft(), panelTop(), PANEL_W, PANEL_H, 0, 0, PANEL_W, PANEL_H, SHEET_SIZE, SHEET_SIZE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Vanilla's labels are not used so this is overridden to remove them
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}