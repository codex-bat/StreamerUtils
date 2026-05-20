// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.core;

import java.util.Set;

public record EmojiDefinition(
        String name,
        String glyph,
        Set<String> aliases
) {}