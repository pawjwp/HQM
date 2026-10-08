package hardcorequesting.common.network;

import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.GuiReward;
import hardcorequesting.common.client.interfaces.MatUnlockToast;
import hardcorequesting.common.client.interfaces.mat.MatIcons;
import hardcorequesting.common.client.interfaces.mat.MatScreens;
import hardcorequesting.common.client.tutorial.TutorialPlayer;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.MatItem;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.QuestBookItem;
import hardcorequesting.common.items.mat.MatHandler;
import hardcorequesting.common.items.mat.MatMaps;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.items.mat.MatUnlocks;
import hardcorequesting.common.items.mat.TrackedLocation;
import hardcorequesting.common.network.message.GeneralUpdateMessage;
import hardcorequesting.common.quests.QuestingData;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.quests.reward.CommandReward;
import hardcorequesting.common.quests.task.QuestTask;
import hardcorequesting.common.tutorial.Tutorial;
import hardcorequesting.common.tutorial.TutorialManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
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
            // Opens the MAT or quest book, using a held one if present or whichever there are more of.
            // Skips the MAT when it is disabled or questing mode is diabled. The MAT breaks ties when relevant.
            // REQUIRE_BOOK skips any item the player isn't carryingplayer doesn't carry.
            Inventory inventory = player.getInventory();
            ItemStack book = ItemStack.EMPTY;
            int mats = 0, books = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() instanceof MatItem) mats++;
                else if (stack.getItem() instanceof QuestBookItem) {
                    books++;
                    // An OP book is opened over a normal book
                    if (book.isEmpty() || stack.is(ModItems.enabledBook.get())) book = stack;
                }
            }
            boolean requireBook = HQMConfig.getInstance().Keybind.REQUIRE_BOOK;
            boolean canMat = HQMConfig.getInstance().MAT.ENABLE_MAT && MatMode.QUEST.isEnabled() && (!requireBook || mats > 0);
            boolean canBook = !requireBook || books > 0;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof MatItem && canMat) {
                    MatHandler.openMatMode(player, MatMode.QUEST);
                    return;
                }
                if (held.getItem() instanceof QuestBookItem) {
                    QuestBookItem.open(player, held);
                    return;
                }
            }
            if (canMat && (!canBook || mats >= books)) MatHandler.openMatMode(player, MatMode.QUEST);
            else if (canBook) QuestBookItem.open(player, book);
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
            if (HQMConfig.getInstance().Keybind.REQUIRE_BOOK && !MatHandler.hasMat(player)) return;
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
    },
    MAT_REMOVE_LOCATION {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
            int index = nbt.getInt("Index");
            if (index < 0 || index >= mat.locations.size()) return;
            TrackedLocation target = mat.locations.get(index);
            MatUnlocks.removeLocations(serverPlayer, location -> location == target);
        }
    },
    MAT_REQUEST_MAP {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (player instanceof ServerPlayer serverPlayer) MatMaps.sendMap(serverPlayer, nbt.getInt("Index"));
        }
    },
    // Saves the step a tutorial is on
    MAT_TUTORIAL_PROGRESS {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
            String id = nbt.getString("Tutorial");
            if (!mat.unlockedTutorials.contains(id)) return;
            int step = nbt.getInt("Step");
            if (step > 0) mat.tutorialProgress.put(id, step);
            else mat.tutorialProgress.remove(id);
            if (step > mat.tutorialFurthest.getOrDefault(id, 0)) mat.tutorialFurthest.put(id, step);
            sendMatDataSync(serverPlayer);
        }
    },
    // Marks a tutorial completed and resets its saved position
    MAT_TUTORIAL_COMPLETED {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
            String id = nbt.getString("Tutorial");
            if (!mat.unlockedTutorials.contains(id)) return;
            mat.completedTutorials.add(id);
            mat.tutorialProgress.remove(id);
            mat.tutorialFurthest.remove(id);
            sendMatDataSync(serverPlayer);
            sendMatTutorialAutoPlay(serverPlayer);
        }
    },
    // Runs the commands from finishing a step, taken from the server version of the tutorial
    // Commands set to per_player only run once, the client decides when the others repeat
    MAT_TUTORIAL_STEP_DONE {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!(player instanceof ServerPlayer)) return;
            String id = nbt.getString("Tutorial");
            int step = nbt.getInt("Step");
            Tutorial tutorial = TutorialManager.getInstance().tutorials.get(id);
            if (tutorial == null || step < 0 || step >= tutorial.steps().size()) return;
            Tutorial.Step done = tutorial.steps().get(step);
            if (done.commandRepeat() == Tutorial.CommandRepeat.PER_PLAYER) {
                MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
                // don't run per_player commands if the tutorial is not unlocked, since then it is not trackable
                if (!mat.unlockedTutorials.contains(id) || !mat.tutorialCommandsRun.computeIfAbsent(id, key -> new HashSet<>()).add(step)) return;
            }
            for (String command : done.commands()) new CommandReward.Command(command).execute(player);
        }
    },
    MAT_UNLOCK_TOAST {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            MatIcons.Icon icon = MatIcons.fromTag(nbt.getCompound("Icon"));
            Component title = Component.Serializer.fromJson(nbt.getString("Title"));
            Component message = nbt.contains("Message") ? Component.Serializer.fromJson(nbt.getString("Message")) : null;
            MatUnlockToast.show(icon, title, message);
        }
    },
    // Starts a tutorial at a step
    MAT_TUTORIAL_PLAY {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            Tutorial tutorial = TutorialManager.getInstance().tutorials.get(nbt.getString("Tutorial"));
            if (tutorial != null) TutorialPlayer.start(tutorial, nbt.getInt("Step"));
        }
    },
    // Pauses a tutorial
    MAT_TUTORIAL_PAUSE {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            if (!nbt.contains("Tutorial") || nbt.getString("Tutorial").equals(TutorialPlayer.getPlayingId())) TutorialPlayer.pause();
        }
    },
    // Starts an auto-play tutorial if nothing is playing
    MAT_TUTORIAL_AUTO_PLAY {
        @Override
        public void receiveData(Player player, CompoundTag nbt) {
            TutorialPlayer.autoPlay();
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

    // server -> client
    public static void sendMatUnlockToast(ServerPlayer player, CompoundTag icon, Component title, Component message) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Icon", icon);
        nbt.putString("Title", Component.Serializer.toJson(title));
        if (message != null) nbt.putString("Message", Component.Serializer.toJson(message));
        MAT_UNLOCK_TOAST.sendMessageToPlayer(nbt, player);
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
    public static void sendMatRemoveLocation(int index) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Index", index);
        MAT_REMOVE_LOCATION.sendMessageToServer(nbt);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendMatRequestMap(int index) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Index", index);
        MAT_REQUEST_MAP.sendMessageToServer(nbt);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendMatTutorialProgress(String tutorial, int step) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Tutorial", tutorial);
        nbt.putInt("Step", step);
        MAT_TUTORIAL_PROGRESS.sendMessageToServer(nbt);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendMatTutorialCompleted(String tutorial) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Tutorial", tutorial);
        MAT_TUTORIAL_COMPLETED.sendMessageToServer(nbt);
    }

    // client -> server
    @Environment(EnvType.CLIENT)
    public static void sendMatTutorialStepDone(String tutorial, int step) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Tutorial", tutorial);
        nbt.putInt("Step", step);
        MAT_TUTORIAL_STEP_DONE.sendMessageToServer(nbt);
    }

    // server -> client
    public static void sendMatTutorialPlay(ServerPlayer player, String tutorial, int step) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Tutorial", tutorial);
        nbt.putInt("Step", step);
        MAT_TUTORIAL_PLAY.sendMessageToPlayer(nbt, player);
    }

    // server -> client
    public static void sendMatTutorialPause(ServerPlayer player, @Nullable String tutorial) {
        CompoundTag nbt = new CompoundTag();
        if (tutorial != null) nbt.putString("Tutorial", tutorial);
        MAT_TUTORIAL_PAUSE.sendMessageToPlayer(nbt, player);
    }

    // server -> client
    public static void sendMatTutorialAutoPlay(ServerPlayer player) {
        MAT_TUTORIAL_AUTO_PLAY.sendMessageToPlayer(new CompoundTag(), player);
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
