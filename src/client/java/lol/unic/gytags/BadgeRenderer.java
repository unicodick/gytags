package lol.unic.gytags;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import lol.unic.gytags.cache.BadgeCache;
import lol.unic.gytags.config.GytagsConfig;

import java.util.List;
import java.util.Map;

public final class BadgeRenderer {
    private static final Map<String, Integer> CAREER_COLORS = Map.of(
            "academ", 0x8F1D10,
            "novichek", 0x546E7A,
            "yrod", 0x3498DB,
            "intern", 0x725482,
            "warden", 0x9B59B6,
            "htyrod", 0x1C9043,
            "deputy", 0xD7342A,
            "head", 0x880031
    );
    private static final Map<String, String> TEAM_SUFFIXES = Map.of(
            "team_1", "¹",
            "team_2", "²",
            "team_3", "³",
            "team_4", "⁴",
            "team_5", "⁵"
    );
    private static final int TEAM_COLOR = 0x9E9E9E;

    private static GytagsConfig config;
    private static BadgeCache cache;

    private BadgeRenderer() {
    }

    public static void configure(GytagsConfig newConfig, BadgeCache newCache) {
        config = newConfig;
        cache = newCache;
    }

    public static Component decorateName(Component original, String nickname) {
        if (config == null || cache == null || !config.display.showInNameTag) {
            return original;
        }
        List<String> badges = cache.badgesFor(nickname);
        Integer careerColor = null;
        String teamSuffix = null;
        for (String badge : badges) {
            if (careerColor == null) {
                careerColor = CAREER_COLORS.get(badge);
            }
            if (teamSuffix == null) {
                teamSuffix = TEAM_SUFFIXES.get(badge);
            }
        }
        if (careerColor == null && teamSuffix == null) {
            return original;
        }

        var decorated = Component.empty();
        if (careerColor != null) {
            int color = careerColor;
            decorated
                    .append(Component.literal("◆").withStyle(style ->
                            style.withColor(TextColor.fromRgb(color))))
                    .append(Component.literal(" "));
        }
        decorated.append(original.copy());
        if (teamSuffix != null) {
            decorated.append(Component.literal(teamSuffix).withStyle(style ->
                    style.withColor(TextColor.fromRgb(TEAM_COLOR))));
        }
        return decorated;
    }
}
