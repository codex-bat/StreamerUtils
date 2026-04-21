// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import java.util.Set;
import net.minecraft.util.Identifier;
import net.minecraft.text.MutableText;

public final class IconGlyphs {
    private IconGlyphs() {}

    public static final StyleSpriteSource CHAT_FONT =
            new StyleSpriteSource.Font(Identifier.of("streamerutils", "icons_chat"));
    public static final StyleSpriteSource TAB_FONT =
            new StyleSpriteSource.Font(Identifier.of("streamerutils", "icons"));
    public static final StyleSpriteSource DEFAULT_FONT =
            new StyleSpriteSource.Font(Identifier.of("minecraft", "default"));

    public static final String NONE = "";
    public static final String LIVE = "\uE000\uE001";
    public static final String HEART = "\uE002";
    public static final String CROWN_TAB = "\uE003";
    public static final String YOUTUBE = "\uE008";
    public static final String TWITCH = "\uE009";
    public static final String KICK = "\uE00A";
    public static final String CROWN_BIG = "\uE00B";

    // developer / mod icons
    public static final String CODEX = "\uE010";          // codex.bat personal icon
    public static final String MOD_CYAN = "\uE011";       // cyan mod logo (transparent)
    public static final String MOD_GREY = "\uE012";       // grey mod logo (transparent)
    public static final String MOD_BLUE = "\uE013";       // blue mod logo (transparent)
    public static final String MOD_DARK_GREY = "\uE014";       // blue mod logo (transparent)
    public static final String MOD_CYAN_BG = "\uE019";    // cyan mod logo + background
    public static final String MOD_GREY_DEPTH = "\uE01A"; // grey mod logo + depth
    public static final String MOD_BLUE_DEPTH = "\uE01B"; // grey mod logo + depth

    private static final Set<String> VALID = Set.of(
            "none", "live", "crown", "youtube", "twitch", "kick", "crown_tab", "heart",
            // dev ones
            "codex", "mod_cyan", "mod_grey", "mod_blue", "mod_dark_grey", "mod_cyan_bg", "mod_grey_depth", "mod_blue_depth"
    );

    // Developer‑only icons – only I may use these lol, or should I like completely restrict them? nah but like... idk
    private static final Set<String> DEV_ONLY = Set.of(
            "codex", "mod_cyan", "mod_grey", "mod_blue", "mod_dark_grey", "mod_cyan_bg", "mod_grey_depth", "mod_blue_depth"
    );

    public static boolean isValid(String iconId) {
        return VALID.contains(iconId);
    }

    public static boolean isDeveloperOnly(String iconId) {
        return DEV_ONLY.contains(iconId);
    }

    public static StyleSpriteSource resolveFont(PlayerSettings settings, boolean isChat) {
        if (isChat && settings.separateChatIconFont()) {
            return CHAT_FONT;
        }
        return TAB_FONT;
    }

    public static Text styledIcon(PlayerSettings settings, boolean isChat) {
        return build(settings, resolveFont(settings, isChat));
    }

    private static Text build(PlayerSettings settings, StyleSpriteSource font) {
        boolean hasLive = settings.streamerLive();
        String icon = resolve(settings.iconId());
        boolean hasIcon = !icon.isEmpty();

        if (!hasLive && !hasIcon) {
            return Text.empty();
        }

        MutableText result = Text.empty();

        if (hasLive) {
            result = result.append(
                    Text.literal(LIVE)
                            .styled(style -> style
                                    .withColor(TextColor.fromRgb(0xFFFFFF))
                                    .withFont(font))
            );
        }

        if (hasLive && hasIcon) {
            result = result.append(
                    Text.literal(" ")
                            .styled(style -> style.withFont(DEFAULT_FONT))
            );
        }

        if (hasIcon) {
            result = result.append(
                    Text.literal(icon)
                            .styled(style -> style
                                    .withColor(TextColor.fromRgb(0xFFFFFF))
                                    .withFont(font))
            );
        }

        return result.append(
                Text.literal(" ")
                        .styled(style -> style.withFont(DEFAULT_FONT))
        );
    }

    public static String resolve(String iconId) {
        return switch (iconId) {
            case "none" -> NONE;
            case "live" -> LIVE;
            case "crown" -> CROWN_BIG;
            case "youtube" -> YOUTUBE;
            case "twitch" -> TWITCH;
            case "kick" -> KICK;
            case "crown_tab" -> CROWN_TAB;
            case "heart" -> HEART;
            // new ones
            case "codex" -> CODEX;
            case "mod_cyan" -> MOD_CYAN;
            case "mod_grey" -> MOD_GREY;
            case "mod_blue" -> MOD_BLUE;
            case "mod_dark_grey" -> MOD_DARK_GREY;
            case "mod_cyan_bg" -> MOD_CYAN_BG;
            case "mod_grey_depth" -> MOD_GREY_DEPTH;
            case "mod_blue_depth" -> MOD_BLUE_DEPTH;
            default -> NONE;
        };
    }

    public static String list() {
        return "none, live, crown, youtube, twitch, kick, crown_tab, heart, " +
                "codex, mod_cyan, mod_grey, mod_blue, mod_dark_grey, mod_cyan_bg, mod_grey_depth, mod_blue_depth";
    }
}