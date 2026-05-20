// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.core;

public record EmojiMatch(String name, String glyph) {
    public boolean found() {
        return !glyph.isEmpty();
    }

    public static EmojiMatch none() {
        return new EmojiMatch("", "");
    }
}