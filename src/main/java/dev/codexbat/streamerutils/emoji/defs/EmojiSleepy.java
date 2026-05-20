// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiSleepy {
    private EmojiSleepy() {}
    public static void register() {
        EmojiRegistry.register("relieved_face",  "\uE105", "relief", "relieved");
        EmojiRegistry.register("sleepy_custom",  "\uE112");
        EmojiRegistry.register("pensive_face",   "\uE11F", "pensive");
        EmojiRegistry.register("sleepy_face",    "\uE12C", "sleepy");
        EmojiRegistry.register("drooling_face",  "\uE139", "drool");
        EmojiRegistry.register("sleeping_face",  "\uE146", "sleep");
        EmojiRegistry.register("bags_under_eyes","\uE153", "face_with_bags_under_eyes");
    }
}

//6