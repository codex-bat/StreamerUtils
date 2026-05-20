// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.smart;

import dev.codexbat.streamerutils.emoji.core.EmojiMatch;
import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Comparator;

public final class SmartEmojiResolver {
    private SmartEmojiResolver() {}

    private static final int AUTO_THRESHOLD = 180;
    private static final int SUGGEST_THRESHOLD = 120;

    public static Result resolve(ServerPlayerEntity player, String token) {
        String normalized = token.toLowerCase();

        EmojiMatch direct = EmojiRegistry.match(normalized);
        if (direct.found()) {
            EmojiUsageTracker.record(player.getUuid(), direct.name());
            return new Result(direct.glyph(), Confidence.EXACT, direct.name());
        }

        var scoredList = EmojiRegistry.all().stream()
                .map(def -> {
                    int score = FuzzyMatcher.score(normalized, def.name());

                    for (String alias : def.aliases()) {
                        score = Math.max(score, FuzzyMatcher.score(normalized, alias));
                    }

                    String[] words = def.name().split("_");
                    for (String word : words) {
                        if (word.startsWith(normalized)) {
                            score += 200;
                        }
                    }

                    int usage = EmojiUsageTracker.getUsage(player.getUuid(), def.name());
                    score += Math.min(usage * 2, 20);

                    return new Scored(def.name(), def.glyph(), score);
                })
                .sorted(Comparator
                        .comparingInt((Scored s) -> s.score()).reversed()
                        .thenComparingInt(s -> s.name().length()))
                .toList();

        if (scoredList.isEmpty()) {
            return Result.none();
        }

        Scored best = scoredList.get(0);
        Scored second = scoredList.size() > 1 ? scoredList.get(1) : null;

        if (second != null && (best.score() - second.score()) < 5) {
            int bestUsage = EmojiUsageTracker.getUsage(player.getUuid(), best.name());
            int secondUsage = EmojiUsageTracker.getUsage(player.getUuid(), second.name());

            if (secondUsage > bestUsage) {
                best = second;
            } else if (secondUsage == bestUsage && second.name().length() < best.name().length()) {
                best = second;
            }
        }

        if (best.score() >= AUTO_THRESHOLD) {
            EmojiUsageTracker.record(player.getUuid(), best.name());
            return new Result(best.glyph(), Confidence.AUTO, best.name());
        } else if (best.score() >= SUGGEST_THRESHOLD) {
            return new Result(best.glyph(), Confidence.SUGGEST, best.name());
        } else {
            return Result.none();
        }
    }

    private record Scored(String name, String glyph, int score) {}

    public enum Confidence {
        EXACT,
        AUTO,
        SUGGEST,
        NONE
    }

    public record Result(String glyph, Confidence confidence, String resolvedName) {
        public static Result none() {
            return new Result("", Confidence.NONE, "");
        }
    }
}