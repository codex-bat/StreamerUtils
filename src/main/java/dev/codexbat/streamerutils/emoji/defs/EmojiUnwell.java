// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiUnwell {
    private EmojiUnwell() {}
    public static void register() {
        EmojiRegistry.register("medical_mask",     "\uE106", "mask");
        EmojiRegistry.register("thermometer",      "\uE113", "face_with_thermometer");
        EmojiRegistry.register("head_bandage",     "\uE120", "hurt", "face_with_head_bandage");
        EmojiRegistry.register("nauseated_face",   "\uE12D");
        EmojiRegistry.register("vomiting_face",    "\uE13A");
        EmojiRegistry.register("sneezing_face",    "\uE147");
        EmojiRegistry.register("hot_face",         "\uE154", "hot");
        EmojiRegistry.register("cold_face",        "\uE161", "cold");
        EmojiRegistry.register("woozy_face",       "\uE16E");
        EmojiRegistry.register("crossed_out_eyes", "\uE17B", "dizzy_face");
        EmojiRegistry.register("spiral_eyes",      "\uE188", "dizzy", "face_with_spiral_eyes");
        EmojiRegistry.register("exploding_head",   "\uE195", "mindblown");
    }
}

//7