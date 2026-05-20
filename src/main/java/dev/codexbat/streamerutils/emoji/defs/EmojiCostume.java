// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.defs;

import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;

public final class EmojiCostume {
    private EmojiCostume() {}
    public static void register() {
        EmojiRegistry.register("pile_of_poo",   "\uE10C", "poop", "poo");
        EmojiRegistry.register("clown_face",    "\uE119", "clown");
        EmojiRegistry.register("ogre",          "\uE126", "ogre");
        EmojiRegistry.register("goblin",        "\uE133", "monster");
        EmojiRegistry.register("ghost",         "\uE140", "spooky");
        EmojiRegistry.register("alien",         "\uE14D", "ufo");
        EmojiRegistry.register("old_alien",     "\uE15A", "flying_saucer"); // 🛸 lil' easter egg :3
        EmojiRegistry.register("alien_monster", "\uE167", "space_invader");
        EmojiRegistry.register("robot",         "\uE174", "bot");
    }
}

//13