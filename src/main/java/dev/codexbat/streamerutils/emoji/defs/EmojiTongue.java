// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiTongue {
    private EmojiTongue() {}
    public static void register() {
        EmojiRegistry.register("savoring_food",  "\uE102", "yum");
        EmojiRegistry.register("face_tongue",    "\uE10F", "silly", "stuck_out_tongue");
        EmojiRegistry.register("wink_tongue",    "\uE11C", "tongue_wink", "stuck_out_tongue_winking_eye");
        EmojiRegistry.register("zany_face",      "\uE129", "crazy");
        EmojiRegistry.register("tongue_custom1", "\uE136");
        EmojiRegistry.register("tongue_custom2", "\uE143");
        EmojiRegistry.register("squinting_tongue","\uE150", "stuck_out_tongue_closed_eyes");
        EmojiRegistry.register("money_mouth",    "\uE15D", "money");
    }
}

//3