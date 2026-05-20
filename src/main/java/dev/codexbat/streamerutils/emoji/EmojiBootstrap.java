// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji;

import dev.codexbat.streamerutils.emoji.defs.*;
// import more groups here

public final class EmojiBootstrap {
    private EmojiBootstrap() {}

    public static void init() {
        EmojiSmileys.register();
        EmojiAffection.register();
        EmojiTongue.register();
        EmojiHand.register();
        EmojiNeutral.register();
        EmojiSleepy.register();
        EmojiUnwell.register();
        EmojiHat.register();
        EmojiGlasses.register();
        EmojiConcerned.register();
        EmojiNegative.register();
        EmojiCostume.register();
        // EmojiPeople.register();
        // EmojiAnimals.register();
        // EmojiCustom.register();
    }
}