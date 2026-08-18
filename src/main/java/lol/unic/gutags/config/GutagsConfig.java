package lol.unic.gutags.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GutagsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Display display = new Display();

    public static GutagsConfig load(Path path) {
        try {
            if (!Files.exists(path)) {
                GutagsConfig config = new GutagsConfig();
                config.save(path);
                return config;
            }
            GutagsConfig config = GSON.fromJson(Files.readString(path), GutagsConfig.class);
            if (config == null) {
                config = new GutagsConfig();
            }
            if (config.display == null) {
                config.display = new Display();
            }
            return config;
        } catch (Exception exception) {
            System.err.println("[gutags] could not read config; using defaults: " + exception.getMessage());
            return new GutagsConfig();
        }
    }

    public void save(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException exception) {
            System.err.println("[gutags] could not write config: " + exception.getMessage());
        }
    }

    public static final class Display {
        public volatile boolean showInNameTag = true;
    }
}
