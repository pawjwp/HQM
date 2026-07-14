package hardcorequesting.common.proxies;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.client.QuestBookKeyHandler;
import hardcorequesting.common.client.interfaces.graphic.task.*;
import hardcorequesting.common.client.interfaces.mat.MatCraftingScreen;
import hardcorequesting.common.inventory.ModMenus;
import hardcorequesting.common.items.MatItem;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.network.PacketContext;
import hardcorequesting.common.quests.Quest;
import hardcorequesting.common.quests.QuestTicker;
import hardcorequesting.common.quests.task.TaskType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientProxy extends CommonProxy {
    
    @Override
    public void init() {
        super.init();
        Quest.clientTicker = new QuestTicker();
        QuestBookKeyHandler.register();
        HardcoreQuestingCore.platform.registerOnClientTick(minecraftClient -> {
            Quest.clientTicker.tick(minecraftClient.level, true);
            QuestBookKeyHandler.handleTick(minecraftClient);
        });
        ClientLifecycleEvent.CLIENT_SETUP.register(instance -> {
            setupTaskGraphics();
            setupMat();
        });
    }

    private static void setupMat() {
        // Tints the MAT item similar to spawn egg tinting
        // Layer 0 (mat.png) is untinted
        // Layer 1 (mat_screen_base.png) uses the mode's base color
        // Layer 2 (mat_screen_overlay.png) uses the mode's overlay color
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFFFF;
            MatMode mode = MatItem.getMode(stack);
            return 0xFF000000 | (tintIndex == 1 ? mode.getBaseColor() : mode.getOverlayColor());
        }, ModItems.mat.get());
        MenuRegistry.registerScreenFactory(ModMenus.matCrafting.get(), MatCraftingScreen::new);
    }

    // Has to be done after our task types have been initialized, so we do this at the client setup event
    private static void setupTaskGraphics() {
        TaskGraphics.register(TaskType.CHECKBOX.get(), CheckBoxTaskGraphic::new);
        TaskGraphics.register(TaskType.CONSUME.get(), (task, playerId, questBook) -> ItemTaskGraphic.createConsumeGraphic(task, playerId, questBook, true));
        TaskGraphics.register(TaskType.CRAFT.get(), ItemTaskGraphic::new);
        TaskGraphics.register(TaskType.LOCATION.get(), LocationTaskGraphic::new);
        TaskGraphics.register(TaskType.CONSUME_QDS.get(), (task, playerId, questBook) -> ItemTaskGraphic.createConsumeGraphic(task, playerId, questBook, false));
        TaskGraphics.register(TaskType.DETECT.get(), ItemTaskGraphic::createDetectGraphic);
        TaskGraphics.register(TaskType.KILL.get(), KillMobsTaskGraphic::new);
        TaskGraphics.register(TaskType.TAME.get(), TameMobsTaskGraphic::new);
        TaskGraphics.register(TaskType.DEATH.get(), DeathTaskGraphic::new);
        TaskGraphics.register(TaskType.REPUTATION.get(), ReputationTaskGraphic::new);
        TaskGraphics.register(TaskType.REPUTATION_KILL.get(), KillReputationTaskGraphic::new);
        TaskGraphics.register(TaskType.ADVANCEMENT.get(), AdvancementTaskGraphic::new);
        TaskGraphics.register(TaskType.COMPLETION.get(), CompleteQuestTaskGraphic::new);
        TaskGraphics.register(TaskType.BLOCK_BREAK.get(), ItemTaskGraphic::new);
        TaskGraphics.register(TaskType.BLOCK_PLACE.get(), ItemTaskGraphic::new);
    }
    
    @Override
    public boolean isClient() {
        return true;
    }
    
    @Override
    public boolean isServer() {
        return false;
    }
    
    @Override
    public Player getPlayer(PacketContext ctx) {
        return ctx.isClient() ? Minecraft.getInstance().player : super.getPlayer(ctx);
    }
}
