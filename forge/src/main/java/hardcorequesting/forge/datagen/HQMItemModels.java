package hardcorequesting.forge.datagen;

import hardcorequesting.common.HardcoreQuestingCore;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
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

        matModels();
    }

    // Generate the MAT model, a three-layer file where the top two layers
    // are recolored based on the current mode (see ClientProxy#setupMat)
    private void matModels() {
        withExistingParent("mat", modLoc("item/base_item"))
                .texture("layer0", modLoc("item/mat"))
                .texture("layer1", modLoc("item/mat_screen_base"))
                .texture("layer2", modLoc("item/mat_screen_overlay"));
    }

    private void baseItem(String name, String texture) {
        withExistingParent(name, modLoc("item/base_item"))
                .texture("layer0", modLoc("item/" + texture));
    }
}
