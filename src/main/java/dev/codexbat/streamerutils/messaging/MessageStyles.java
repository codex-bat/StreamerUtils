// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.messaging;

import net.minecraft.util.Formatting;

public final class MessageStyles {
    // Label text (e.g., "• Icon: ", "• Color: ")
    public static final Formatting LABEL_COLOR = Formatting.GRAY;
    // Arrow between values
    public static final Formatting ARROW_COLOR = Formatting.GRAY;
    // Default neutral text
    public static final Formatting DEFAULT_COLOR = Formatting.GRAY;
    // Header for reset sections
    public static final Formatting HEADER_COLOR = Formatting.DARK_AQUA;
    // Undo button
    public static final Formatting UNDO_COLOR = Formatting.BLUE;
    // Prefix brackets
    public static final Formatting PREFIX_BRACKET_COLOR = Formatting.DARK_GRAY;
    // Prefix text (SU / StreamerUtils)
    public static final Formatting PREFIX_TEXT_COLOR = Formatting.BLUE;

    private MessageStyles() {}
}