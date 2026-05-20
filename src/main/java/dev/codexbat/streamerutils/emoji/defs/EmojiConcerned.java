// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiConcerned {
    private EmojiConcerned() {}
    public static void register() {
        EmojiRegistry.register("confused_face",         "\uE109", "confused");
        EmojiRegistry.register("diagonal_mouth",        "\uE116", "expressionless", "meh"); // removed "meh" (not official)
        EmojiRegistry.register("worried_face",          "\uE123", "worried");
        EmojiRegistry.register("slightly_frowning",     "\uE130", "slight_frown");
        EmojiRegistry.register("frowning_face",         "\uE13D", "frowning2", "frowning_face");
        EmojiRegistry.register("open_mouth",            "\uE14A", "open_mouth");
        EmojiRegistry.register("hushed_face",           "\uE157", "hushed");
        EmojiRegistry.register("astonished_face",       "\uE164", "astonished");
        EmojiRegistry.register("astonished_custom1",    "\uE171");
        EmojiRegistry.register("astonished_custom2",    "\uE17E");
        EmojiRegistry.register("flushed_face",          "\uE18B", "flushed");
        EmojiRegistry.register("distorted_face",        "\uE198", "noway");
        EmojiRegistry.register("pleading_face",         "\uE1A5", "pleading_face", "plead");
        EmojiRegistry.register("holding_back_tears",    "\uE1B2", "face_holding_back_tears");
        EmojiRegistry.register("frown_open_mouth",      "\uE1BF", "frowning_face_with_open_mouth");
        EmojiRegistry.register("anguished_face",        "\uE1CC", "anguished", "anguish");
        EmojiRegistry.register("fearful_face",          "\uE1D9", "fearful");
        EmojiRegistry.register("anxious_sweat",         "\uE1E6", "anxious_face_with_sweat", "anxious");
        EmojiRegistry.register("sad_relieved",          "\uE1F3", "sad_but_relieved_face");
        EmojiRegistry.register("crying_face",           "\uE200", "cry");
        EmojiRegistry.register("loudly_crying",         "\uE20D", "sob");
        EmojiRegistry.register("screaming_fear",        "\uE21A", "scream", "screaming");

        // Second group
        EmojiRegistry.register("confounded_face",       "\uE10A", "confounded");
        EmojiRegistry.register("persevering_face",      "\uE117", "persevere", "persevering");
        EmojiRegistry.register("disappointed_face",     "\uE124", "disappointed");
        EmojiRegistry.register("downcast_sweat",        "\uE131", "downcast_face_with_sweat");
        EmojiRegistry.register("weary_face",            "\uE13E", "weary");
        EmojiRegistry.register("tired_face",            "\uE14B", "tired");
        EmojiRegistry.register("yawning_face",          "\uE158", "yawning", "bored");
    }
}

//10 & 11