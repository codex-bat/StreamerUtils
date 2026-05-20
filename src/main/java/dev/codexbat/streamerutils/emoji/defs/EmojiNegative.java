// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiNegative {
    private EmojiNegative() {}
    public static void register() {
        EmojiRegistry.register("steam_from_nose",   "\uE10B", "triumph");
        EmojiRegistry.register("enraged_face",      "\uE118", "rage");
        EmojiRegistry.register("angry_face",        "\uE125", "mad", "angry");
        EmojiRegistry.register("symbols_on_mouth",  "\uE132", "swear", "symbols");
        EmojiRegistry.register("smiling_horns",     "\uE13F", "smiling_imp");
        EmojiRegistry.register("angry_horns",       "\uE14C", "imp");
        EmojiRegistry.register("skull",             "\uE159", "dead");
        EmojiRegistry.register("skull_crossbones",  "\uE166", "poison", "skull_crossbones");
    }
}

//12