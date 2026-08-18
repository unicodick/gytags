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
    private static final Map<String, TeamVisual> TEAM_VISUALS = Map.of(
            "team_1", new TeamVisual("¹", 0x0A0A0A),
            "team_2", new TeamVisual("²", 0xE67E22),
            "team_3", new TeamVisual("³", 0x931515),
            "team_4", new TeamVisual("⁴", 0x237C00),
            "team_5", new TeamVisual("⁵", 0x6C009F)
    );

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
        TeamVisual teamVisual = null;
        for (String badge : badges) {
            if (careerColor == null) {
                careerColor = CAREER_COLORS.get(badge);
            }
            if (teamVisual == null) {
                teamVisual = TEAM_VISUALS.get(badge);
            }
        }
        if (careerColor == null && teamVisual == null) {
            return original;
        }

        var decorated = Component.empty();
        if (careerColor != null) {
            int color = careerColor;
            decorated
                    .append(Component.literal("◆").withStyle(style ->
                            style.withColor(TextColor.fromRgb(color))));
        }
        decorated.append(original.copy());
        if (teamVisual != null) {
            String suffix = teamVisual.suffix();
            int color = teamVisual.color();
            decorated.append(Component.literal(suffix).withStyle(style ->
                    style.withColor(TextColor.fromRgb(color))));
        }
        return decorated;
    }

    private record TeamVisual(String suffix, int color) {
    }
}
