package lol.unic.gutags;

import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.List;

public final class OnlinePlayerCollector {
    public List<String> collect(Minecraft client) {
        if (client.level == null) {
            return List.of();
        }

        return client.level.players().stream()
                .map(player -> player.getName().getString())
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
