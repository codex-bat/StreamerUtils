// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiHand {
    private EmojiHand() {}
    public static void register() {
        EmojiRegistry.register("open_hands",          "\uE103", "hug");
        EmojiRegistry.register("hand_over_mouth",     "\uE110", "oops", "face_with_hand_over_mouth");
        EmojiRegistry.register("open_eyes_hand_mouth","\uE11D", "face_with_open_eyes_and_hand_over_mouth");
        EmojiRegistry.register("peeking_eye",         "\uE12A", "peek", "face_with_peeking_eye");
        EmojiRegistry.register("shushing_face",       "\uE137", "shush");
        EmojiRegistry.register("thinking_face",       "\uE144", "think", "thinking");
        EmojiRegistry.register("saluting_face",       "\uE151", "salute");
    }
}

//4