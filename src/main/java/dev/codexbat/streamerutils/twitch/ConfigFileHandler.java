// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Handles reading and writing config data.
 * Makes sure files don't get corrupted by weird line endings.
 * honestly i just wanted a central place for this stuff
 */
public final class ConfigFileHandler {

    private ConfigFileHandler() {}

    public static String prepForWrite(String raw) {
        // pass it through the chunker
        return DataSegmenter.bundle(raw);
    }

    public static String readFromDisk(String stored) {
        return DataSegmenter.unbundle(stored);
    }
}