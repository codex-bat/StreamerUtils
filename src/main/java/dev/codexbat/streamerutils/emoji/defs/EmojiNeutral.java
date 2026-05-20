// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiNeutral {
    private EmojiNeutral() {}
    public static void register() {
        EmojiRegistry.register("zipper_mouth",     "\uE104", "secret");
        EmojiRegistry.register("raised_eyebrow",   "\uE111", "skeptical", "face_with_raised_eyebrow");
        EmojiRegistry.register("neutral_face",     "\uE11E", "neutral");
        EmojiRegistry.register("neutral_custom",   "\uE12B");
        EmojiRegistry.register("expressionless",   "\uE138", "blank");
        EmojiRegistry.register("no_mouth",         "\uE145", "silent");
        EmojiRegistry.register("dotted_line_face", "\uE152", "invisible");
        EmojiRegistry.register("face_in_clouds",   "\uE15F", "clouds");
        EmojiRegistry.register("smirking_face",    "\uE16C", "smirk");
        EmojiRegistry.register("unamused_face",    "\uE179", "unamused");
        EmojiRegistry.register("unamused_custom",  "\uE186");
        EmojiRegistry.register("rolling_eyes",     "\uE193", "annoyed");
        EmojiRegistry.register("grimacing_face",   "\uE1A0", "grimace");
        EmojiRegistry.register("grimacing_custom", "\uE1AD", "custom");
        EmojiRegistry.register("face_exhaling",    "\uE1BA", "exhale");
        EmojiRegistry.register("lying_face",       "\uE1C7", "lie");
        EmojiRegistry.register("shaking_face",     "\uE1D4", "shake");
        EmojiRegistry.register("head_shake_horizontal", "\uE1E1", "no");
        EmojiRegistry.register("head_shake_vertical",   "\uE1EE", "nod");
    }
}

//5