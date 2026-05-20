// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji;

import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

public final class EmojiFont {
    private EmojiFont() {}

    public static final StyleSpriteSource EMOJI_FONT =
            new StyleSpriteSource.Font(Identifier.of("streamerutils", "emojis"));
}