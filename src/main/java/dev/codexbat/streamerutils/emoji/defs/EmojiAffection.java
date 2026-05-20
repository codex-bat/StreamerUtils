// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiAffection {
    private EmojiAffection() {}
    public static void register() {
        EmojiRegistry.register("smiling_hearts",          "\uE101", "smiling_face_with_3_hearts", "smiling_face_with_hearts");
        EmojiRegistry.register("heart_eyes",              "\uE10E", "heart_eyes");
        EmojiRegistry.register("star_struck",             "\uE11B", "star_struck", "starstruck");
        EmojiRegistry.register("blowing_kiss",            "\uE128", "kissing_heart");
        EmojiRegistry.register("kissing_face",            "\uE135", "kissing");
        EmojiRegistry.register("smiling_face_simple",     "\uE142", "slightly_smiling_face");
        EmojiRegistry.register("kissing_closed_eyes",     "\uE14F", "kissing_closed_eyes");
        EmojiRegistry.register("kissing_smiling_eyes",    "\uE15C", "kissing_smiling_eyes");
        EmojiRegistry.register("smiling_tear",            "\uE169", "smiling_face_with_tear");
    }
}

//2