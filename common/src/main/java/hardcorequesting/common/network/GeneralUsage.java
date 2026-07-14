package hardcorequesting.common.network;

import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.GuiReward;
import hardcorequesting.common.client.interfaces.mat.MatScreens;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.event.EventTrigger;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.mat.MatHandler;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.network.message.GeneralUpdateMessage;
import hardcorequesting.common.quests.QuestingData;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.quests.task.QuestTask;
import hardcorequesting.common.team.PlayerEntry;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A class to replace {@link hardcorequesting.common.client.ClientChange} completely one day.
 * The important difference is the message design. Instead of sending json as string
 * to the client this sends plain old NBT, which is much less message space and
 * way more robust in the Minecraft environment.
 * <p>
 * The IMessageHandler is registered for the Server and the Client, so packages aren't restricted to a side
 *
 * @author canitzp
 * @since 5.4.0
 */
public enum GeneralUsage {
    
    BOOK_OPEN {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            GuiQuestBook.displayGui(player, nbt.getBoolean("OP"));
        }
    },
    BOOK_SELECT_TASK {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            QuestingData data = QuestingDataManager.getInstance().getQuestingData(player);
            data.selectedQuestId = nbt.getUUID("QuestId");
            data.selectedTask = nbt.getInt("TaskId");
        }
    },
    BAG_OPENED {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            UUID groupId = nbt.getUUID("GroupId");
            int bag = nbt.getInt("Bag");
            int[] limits = nbt.getIntArray("Limits");
            
            GuiReward.open(player, groupId, bag, limits);
        }
    },
    REQUEST_BOOK_OPEN {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            QuestingDataManager data = QuestingDataManager.getInstance();
            if (!data.isQuestActive()) {
                player.sendSystemMessage(Translator.translatable("hqm.message.noQuestYet"));
                return;
            }
            if (HQMConfig.getInstance().Keybind.REQUIRE_BOOK
                    && !player.getInventory().contains(new ItemStack(ModItems.book.get()))) {
                return;
            }
            EventTrigger.instance().onBookOpening(new EventTrigger.BookOpeningEvent(player.getUUID(), false, true));
            PlayerEntry entry = data.getQuestingData(player).getTeam().getEntry(player.getUUID());
            if (entry != null) {
                sendOpenBook(player, false);
            } else {
                player.sendSystemMessage(Component.translatable("hqm.message.bookNoPlayer"));
            }
        }
    },
    MAT_OPEN {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            MatScreens.open(player, MatMode.fromId(nbt.getInt("Mode")), nbt);
        }
    },
    OPEN_MAT_MODE {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!MatHandler.hasMat(player)) return;
            MatMode mode = MatMode.fromId(nbt.getInt("Mode"));
            if (nbt.getBoolean("SetDefault")) {
                MatHandler.setDefaultMode(player, mode);
            }
            MatHandler.openMatMode(player, mode, nbt.getBoolean("Dock"));
        }
    },
    MAT_DATA_SYNC {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            QuestingData data = QuestingDataManager.getInstance().getQuestingData(player);
            if (data != null) data.matData.fromNBT(nbt.getCompound("Data"));
        }
    },
    MAT_SELECT_LOCATION {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            QuestingData data = QuestingDataManager.getInstance().getQuestingData(player);
            if (data == null) return;
            int index = nbt.getInt("Index");
            MatPlayerData mat = data.matData;
            if (index < 0 || index >= mat.locations.size()) return;
            mat.selectedLocation = mat.selectedLocation == index ? -1 : index;
            if (player instanceof ServerPlayer serverPlayer) sendMatDataSync(serverPlayer);
        }
    };
    
    // server -> client
    public static void sendOpenBook(Player player, boolean op) {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("OP", op);
        BOOK_OPEN.sendMessageToPlayer(nbt, player);
    }

    // server -> client
    public static void sendOpenMat(Player player, MatMode mode) {
        sendOpenMat(player, mode, new CompoundTag());
    }

    // server -> client
    public static void sendOpenMat(Player player, MatMode mode, CompoundTag extra) {
        CompoundTag nbt = extra.copy();
        nbt.putInt("Mode", mode.getId());
        MAT_OPEN.sendMessageToPlayer(nbt, player);
    }

    // server -> client
    public static void sendMatDataSync(ServerPlayer player) {
        MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
        CompoundTag nbt = new CompoundTag();
        nbt.put("Data", mat.toNBT());
        MAT_DATA_SYNC.sendMessageToPlayer(nbt, player);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendOpenMatMode(MatMode mode, boolean setDefault) {
        sendOpenMatMode(mode, setDefault, false);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendOpenMatMode(MatMode mode, boolean setDefault, boolean dock) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Mode", mode.getId());
        nbt.putBoolean("SetDefault", setDefault);
        nbt.putBoolean("Dock", dock);
        OPEN_MAT_MODE.sendMessageToServer(nbt);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendMatSelectLocation(int index) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Index", index);
        MAT_SELECT_LOCATION.sendMessageToServer(nbt);
    }
    
    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendBookSelectTaskUpdate(QuestTask task) {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("QuestId", task.getParent().getQuestId());
        nbt.putInt("TaskId", task.getId());
        BOOK_SELECT_TASK.sendMessageToServer(nbt);
    }
    
    // server -> client
    public static void sendOpenBagUpdate(Player player, UUID groupId, int bag, int[] limits) {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("GroupId", groupId);
        nbt.putInt("Bag", bag);
        nbt.putIntArray("Limits", limits);
        BAG_OPENED.sendMessageToPlayer(nbt, player);
    }
    
    public abstract void receiveData(Player player, CompoundTag nbt);
    
    @Environment(EnvType.CLIENT)
    public void sendMessageToServer(CompoundTag data) {
        NetworkManager.sendToServer(new GeneralUpdateMessage(Minecraft.getInstance().player, data, ordinal()));
    }
    
    public void sendMessageToPlayer(CompoundTag data, Player player) {
        if (player instanceof ServerPlayer) {
            NetworkManager.sendToPlayer(new GeneralUpdateMessage(player, data, ordinal()), (ServerPlayer) player);
        }
    }
    
}
