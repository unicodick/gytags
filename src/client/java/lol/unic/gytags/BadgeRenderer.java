package lol.unic.gytags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import lol.unic.gytags.cache.BadgeCache;
import lol.unic.gytags.config.GytagsConfig;

import java.util.Map;

public final class BadgeRenderer {
    private static final String BADGE_SEPARATOR = " ";
    private static final Map<String, BadgeVisual> VISUALS = Map.of(
            "academ", new BadgeVisual("◆", ChatFormatting.DARK_RED),
            "novichek", new BadgeVisual("◆", ChatFormatting.GRAY),
            "yrod", new BadgeVisual("◆", ChatFormatting.AQUA),
            "staff", new BadgeVisual("◆", ChatFormatting.GOLD)
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
        String badge = cache.badgeFor(nickname);
        if (badge == null) {
            return original;
        }

        BadgeVisual visual = VISUALS.get(badge);
        if (visual == null) {
            return original;
        }
        return original.copy()
                .append(Component.literal(BADGE_SEPARATOR))
                .append(Component.literal(visual.glyph()).withStyle(visual.color()));
    }

    private record BadgeVisual(String glyph, ChatFormatting color) {
    }
}
