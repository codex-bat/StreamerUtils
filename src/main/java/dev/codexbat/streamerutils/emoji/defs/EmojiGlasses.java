// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiGlasses {
    private EmojiGlasses() {}
    public static void register() {
        EmojiRegistry.register("sunglasses",   "\uE108", "cool");
        EmojiRegistry.register("nerd_face",    "\uE115", "nerd");
        EmojiRegistry.register("monocle_face", "\uE122", "classy", "face_with_monocle", "monocle");
    }
}

//9