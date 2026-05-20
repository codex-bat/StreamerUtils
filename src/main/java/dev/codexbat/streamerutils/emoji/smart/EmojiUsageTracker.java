// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.smart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmojiUsageTracker {
    private static final Map<UUID, Map<String, Integer>> USAGE = new HashMap<>();

    private EmojiUsageTracker() {}

    public static void record(UUID playerId, String emojiName) {
        USAGE.computeIfAbsent(playerId, k -> new HashMap<>())
                .merge(emojiName, 1, Integer::sum);
    }

    public static int getUsage(UUID playerId, String emojiName) {
        return USAGE.getOrDefault(playerId, Map.of())
                .getOrDefault(emojiName, 0);
    }
}