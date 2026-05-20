// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiSmileys {
    private EmojiSmileys() {}
    public static void register() {
        // numbers 1-6, then flipped 8 + 7, then custom, then 9-14
        EmojiRegistry.register("beaming_face",            "\uE100", "grinning");
        EmojiRegistry.register("grinning_smiling_eyes",   "\uE10D", "grin");
        EmojiRegistry.register("grinning_squinting",      "\uE11A", "laughing", "satisfied");
        EmojiRegistry.register("grinning_sweat",          "\uE127", "sweat_smile");
        EmojiRegistry.register("tears_of_joy",            "\uE134", "joy");
        EmojiRegistry.register("rolling_on_floor",        "\uE141", "rofl");
        EmojiRegistry.register("sharp_grinning",          "\uE14E", "drugs");
        EmojiRegistry.register("slightly_smiling",        "\uE15B", "slight_smile");
        EmojiRegistry.register("upside_down",             "\uE168", "upside_down");
        EmojiRegistry.register("melting_face",            "\uE175", "melting_face");
        EmojiRegistry.register("winking_face",            "\uE182", "wink");
        EmojiRegistry.register("smiling_eyes",            "\uE18F", "smile");
        EmojiRegistry.register("smiling_halo",            "\uE19C", "innocent", "angel");
    }
}

//1