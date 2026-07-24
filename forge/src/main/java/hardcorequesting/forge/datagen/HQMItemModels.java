package hardcorequesting.forge.datagen;

import hardcorequesting.common.HardcoreQuestingCore;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class HQMItemModels extends ItemModelProvider {
    private static final String[] BAGS = {"basic_bag", "good_bag", "greater_bag", "epic_bag", "legendary_bag"};
    private static final String[] HEARTS = {"quarterheart", "halfheart", "threequartsheart", "heart", "rottenheart"};

    public HQMItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, HardcoreQuestingCore.ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Block items inherit their block model
        withExistingParent("item_barrel", modLoc("block/item_barrel"));
        withExistingParent("quest_tracker", modLoc("block/quest_tracker"));

        // Hearts use a texture matching their name
        for (String heart : HEARTS) baseItem(heart, heart);

        // Every loot bag shares the same sprite
        for (String bag : BAGS) baseItem(bag, "lootbag");

        baseItem("quest_book", "hqm_quest_book");
        baseItem("enabled_quest_book", "hqm_quest_book_op");
        baseItem("hqm_invalid_item", "hqm_invalid_item");

        // Data chips share the same three-layer model, recolored in ClientProxy
        dataChipModel("tutorial_data_chip");
        dataChipModel("statistic_data_chip");
        dataChipModel("location_data_chip");

        matModels();
    }

    // Generate the MAT model, a three-layer file where the top two layers
    // are recolored based on the current mode (see ClientProxy#setupMat)
    private void matModels() {
        ItemModelBuilder base = withExistingParent("mat", modLoc("item/base_item"))
                .texture("layer0", modLoc("item/mat"))
                .texture("layer1", modLoc("item/mat_screen_base"))
                .texture("layer2", modLoc("item/mat_screen_overlay"));

        // Tracking mode has different textures based on direction
        for (int i = 0; i <= 32; i++) {
            int frame = (i + 16) & 31;
            String name = String.format("mat_pointer_%02d", frame);
            float angle = 0F;
            if (i != 0) {
                angle = (2F * i - 1F) / 64F;
                pointerFrame(name);
            }
            base.override()
                    .predicate(new ResourceLocation("tracking"), 1F)
                    .predicate(new ResourceLocation("angle"), angle)
                    .model(new ModelFile.UncheckedModelFile(modLoc("item/" + name)));
        }
    }

    // A tracking pointer frame with three MAT layers and the pointer texture on top
    private void pointerFrame(String name) {
        withExistingParent(name, modLoc("item/base_item"))
                .texture("layer0", modLoc("item/mat"))
                .texture("layer1", modLoc("item/mat_screen_base"))
                .texture("layer2", modLoc("item/mat_screen_overlay"))
                .texture("layer3", modLoc("item/" + name));
    }

    // Generate the MAT model, a three-layer file where the top two layers
    // are recolored based on the current mode (see ClientProxy#setupMat)
    private void dataChipModel(String name) {
        withExistingParent(name, modLoc("item/base_item"))
                .texture("layer0", modLoc("item/data_chip_plug"))
                .texture("layer1", modLoc("item/data_chip_case_base"))
                .texture("layer2", modLoc("item/data_chip_case_overlay"));
    }

    private void baseItem(String name, String texture) {
        withExistingParent(name, modLoc("item/base_item"))
                .texture("layer0", modLoc("item/" + texture));
    }
}
