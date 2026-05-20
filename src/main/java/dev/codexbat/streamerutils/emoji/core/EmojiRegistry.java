// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji.core;

import java.util.*;

public final class EmojiRegistry {
    private EmojiRegistry() {}

    private static final Map<String, EmojiDefinition> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_NAME = new HashMap<>();

    public static void register(String name, String glyph, String... aliases) {
        String normalized = normalize(name);

        if (BY_NAME.containsKey(normalized)) {
            throw new IllegalStateException("Duplicate emoji name: " + name);
        }

        Set<String> aliasSet = new HashSet<>();
        for (String alias : aliases) {
            String norm = normalize(alias);
            if (!norm.equals(normalized)) {
                aliasSet.add(norm);
            }
        }

        EmojiDefinition def = new EmojiDefinition(normalized, glyph, aliasSet);
        BY_NAME.put(normalized, def);

        for (String alias : aliases) {
            String a = normalize(alias);
            if (a.isEmpty()) {
                continue;
            }

            String previous = ALIAS_TO_NAME.putIfAbsent(a, normalized);
            if (previous != null && !previous.equals(normalized)) {
                throw new IllegalStateException(
                        "Emoji alias collision: '" + alias + "' already maps to " + previous
                );
            }
        }
    }

    public static boolean exists(String name) {
        return match(name).found();
    }

    public static EmojiMatch match(String name) {
        String key = normalize(name);

        EmojiDefinition direct = BY_NAME.get(key);
        if (direct != null) {
            return new EmojiMatch(direct.name(), direct.glyph());
        }

        String aliasTarget = ALIAS_TO_NAME.get(key);
        if (aliasTarget != null) {
            EmojiDefinition aliased = BY_NAME.get(aliasTarget);
            if (aliased != null) {
                return new EmojiMatch(aliased.name(), aliased.glyph());
            }
        }

        return EmojiMatch.none();
    }

    public static String resolve(String name) {
        return match(name).glyph();
    }

    public static Collection<EmojiDefinition> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}