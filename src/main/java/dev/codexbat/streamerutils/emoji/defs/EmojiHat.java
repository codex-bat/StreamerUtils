// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiHat {
    private EmojiHat() {}
    public static void register() {
        EmojiRegistry.register("cowboy_hat_face", "\uE107", "cowboy");
        EmojiRegistry.register("partying_face",   "\uE114", "party");
        EmojiRegistry.register("disguised_face",  "\uE121", "disguise");
    }
}

//8