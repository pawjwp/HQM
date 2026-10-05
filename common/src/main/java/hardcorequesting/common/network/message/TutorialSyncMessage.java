package hardcorequesting.common.network.message;

import com.google.common.collect.Maps;
import hardcorequesting.common.client.tutorial.TutorialPlayer;
import hardcorequesting.common.io.LocalDataManager;
import hardcorequesting.common.network.IMessage;
import hardcorequesting.common.network.IMessageHandler;
import hardcorequesting.common.network.PacketContext;
import hardcorequesting.common.tutorial.TutorialManager;
import hardcorequesting.common.util.SyncUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

/**
 * Syncs tutorial files to the clients after reloading
 */
public class TutorialSyncMessage implements IMessage {
    private Map<String, String> files;

    public TutorialSyncMessage() {
    }

    public TutorialSyncMessage(Map<String, String> files) {
        this.files = files;
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf, PacketContext context) {
        files = readFiles(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        writeFiles(files, buf);
    }

    public static void writeFiles(Map<String, String> files, FriendlyByteBuf buf) {
        buf.writeInt(files.size());
        for (Map.Entry<String, String> entry : files.entrySet()) {
            SyncUtil.writeLargeString(entry.getKey(), buf);
            SyncUtil.writeLargeString(entry.getValue(), buf);
        }
    }

    public static Map<String, String> readFiles(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, String> files = Maps.newLinkedHashMap();
        for (int i = 0; i < count; i++) {
            String fileName = SyncUtil.readLargeString(buf);
            files.put(fileName, SyncUtil.readLargeString(buf));
        }
        return files;
    }

    public static void loadFiles(TutorialManager manager, Map<String, String> files) {
        LocalDataManager data = new LocalDataManager();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            data.provide("tutorials/" + entry.getKey(), entry.getValue());
        }
        manager.load(data);
    }

    public static class Handler implements IMessageHandler<TutorialSyncMessage, IMessage> {

        @Environment(EnvType.CLIENT)
        @Override
        public IMessage onMessage(TutorialSyncMessage message, PacketContext ctx) {
            ctx.getTaskQueue().accept(() -> handle(message));
            return null;
        }

        @Environment(EnvType.CLIENT)
        private void handle(TutorialSyncMessage message) {
            if (!Minecraft.getInstance().hasSingleplayerServer()) loadFiles(TutorialManager.getInstance(), message.files);
            TutorialPlayer.reload();
        }
    }
}
