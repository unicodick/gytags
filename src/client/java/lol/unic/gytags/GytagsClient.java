package lol.unic.gytags;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.loader.api.FabricLoader;
import lol.unic.gytags.cache.BadgeCache;
import lol.unic.gytags.config.GytagsConfig;
import lol.unic.gytags.network.WsConnectionManager;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.List;

public final class GytagsClient implements ClientModInitializer {
    public static final String MOD_ID = "gytags";
    private static final int PLAYER_REFRESH_INTERVAL_TICKS = 20;

    private static BadgeCache cache;
    private static GytagsConfig config;
    private static WsConnectionManager connection;
    private static Path configPath;
    private static final OnlinePlayerCollector PLAYER_COLLECTOR = new OnlinePlayerCollector();
    private static int playerRefreshCooldown;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("gytags.json");
        config = GytagsConfig.load(configPath);
        cache = new BadgeCache();
        connection = new WsConnectionManager(cache);

        BadgeRenderer.configure(config, cache);
        ClientTickEvents.END_CLIENT_TICK.register(GytagsClient::collectOnlinePlayers);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> connection.stop());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("gytags")
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

    public static GytagsConfig config() {
        return config;
    }

    public static BadgeCache cache() {
        return cache;
    }

    private static int setShowInNameTag(boolean show) {
        config.display.showInNameTag = show;
        config.save(configPath);
        return 1;
    }
}
