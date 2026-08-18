package lol.unic.gutags;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.loader.api.FabricLoader;
import lol.unic.gutags.cache.BadgeCache;
import lol.unic.gutags.config.GutagsConfig;
import lol.unic.gutags.network.WsConnectionManager;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.List;

public final class GutagsClient implements ClientModInitializer {
    private static final int PLAYER_REFRESH_INTERVAL_TICKS = 20;

    private static BadgeCache cache;
    private static GutagsConfig config;
    private static WsConnectionManager connection;
    private static Path configPath;
    private static final OnlinePlayerCollector PLAYER_COLLECTOR = new OnlinePlayerCollector();
    private static int playerRefreshCooldown;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("gutags.json");
        config = GutagsConfig.load(configPath);
        cache = new BadgeCache();
        connection = new WsConnectionManager(cache);

        BadgeRenderer.configure(config, cache);
        ClientTickEvents.END_CLIENT_TICK.register(GutagsClient::collectOnlinePlayers);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> connection.stop());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("gutags")
                        .then(ClientCommandManager.literal("on").executes(context -> setShowInNameTag(true)))
                        .then(ClientCommandManager.literal("off").executes(context -> setShowInNameTag(false)))
        ));
    }

    private static void collectOnlinePlayers(Minecraft client) {
        if (client.level == null) {
            playerRefreshCooldown = 0;
            connection.stop();
            return;
        }
        if (playerRefreshCooldown > 0) {
            playerRefreshCooldown--;
            return;
        }
        playerRefreshCooldown = PLAYER_REFRESH_INTERVAL_TICKS - 1;

        List<String> nicknames = PLAYER_COLLECTOR.collect(client);
        if (nicknames.isEmpty()) {
            return;
        }
        connection.updateNicknames(nicknames);
        connection.start();
    }

    private static int setShowInNameTag(boolean show) {
        config.display.showInNameTag = show;
        config.save(configPath);
        return 1;
    }
}
