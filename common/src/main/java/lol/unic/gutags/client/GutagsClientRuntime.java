package lol.unic.gutags.client;

import lol.unic.gutags.cache.BadgeCache;
import lol.unic.gutags.config.GutagsConfig;
import lol.unic.gutags.network.WsConnectionManager;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.List;

public final class GutagsClientRuntime {
    private static final int PLAYER_REFRESH_INTERVAL_TICKS = 20;

    private final Path configPath;
    private final GutagsConfig config;
    private final BadgeCache cache;
    private final WsConnectionManager connection;
    private final OnlinePlayerCollector playerCollector = new OnlinePlayerCollector();
    private int playerRefreshCooldown;

    public GutagsClientRuntime(Path configPath) {
        this.configPath = configPath;
        this.config = GutagsConfig.load(configPath);
        this.cache = new BadgeCache();
        this.connection = new WsConnectionManager(cache);
        BadgeRenderer.configure(config, cache);
    }

    public void tick(Minecraft client) {
        if (client.level == null) {
            playerRefreshCooldown = 0;
            connection.stop();
            cache.clear();
            return;
        }
        if (playerRefreshCooldown > 0) {
            playerRefreshCooldown--;
            return;
        }
        playerRefreshCooldown = PLAYER_REFRESH_INTERVAL_TICKS - 1;

        List<String> nicknames = playerCollector.collect(client);
        connection.updateNicknames(nicknames);
        if (nicknames.isEmpty()) {
            connection.stop();
            cache.clear();
            return;
        }
        connection.start();
    }

    public void stop() {
        connection.stop();
        cache.clear();
    }

    public int setShowInNameTag(boolean show) {
        config.display.showInNameTag = show;
        config.save(configPath);
        return 1;
    }
}
