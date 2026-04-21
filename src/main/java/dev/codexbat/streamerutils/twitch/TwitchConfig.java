// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.codexbat.streamerutils.StreamerUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Uuids;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class TwitchConfig {
    // Default values for a new config
    public String twitchChannelName = "";
    public String botUsername = "";
    public String oauthToken = "";
    public String clientId = "";
    public boolean enabled = false; // disabled until properly configured

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("streamerutils");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ------------------------------------------------------------------------
    // Per‑player loading / saving
    // ------------------------------------------------------------------------

    private static Path getPlayerConfigPath(UUID uuid) {
        return CONFIG_DIR.resolve("twitch_" + uuid.toString() + ".json");
    }

    public static TwitchConfig loadForPlayer(UUID uuid) {
        Path path = getPlayerConfigPath(uuid);
        if (Files.exists(path)) {
            try {
                String encrypted = Files.readString(path);
                String json = ConfigFileHandler.readFromDisk(encrypted);
                TwitchConfig cfg = GSON.fromJson(json, TwitchConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                StreamerUtils.LOGGER.error("Failed to load config for player {}", uuid, e);
            }
        }
        TwitchConfig config = new TwitchConfig();
        config.enabled = false;
        return config;
    }

    public void saveForPlayer(UUID uuid) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Path path = getPlayerConfigPath(uuid);
            String json = GSON.toJson(this);
            String encrypted = ConfigFileHandler.prepForWrite(json);
            Files.writeString(path, encrypted);
        } catch (Exception e) {
            StreamerUtils.LOGGER.error("Failed to save config for player {}", uuid, e);
        }
    }

    public static boolean deleteForPlayer(UUID uuid) {
        try {
            Path path = getPlayerConfigPath(uuid);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Global fallback (legacy support)
    // ------------------------------------------------------------------------

    private static final Path GLOBAL_CONFIG_PATH = CONFIG_DIR.resolve("twitch_global.json");

    public static TwitchConfig loadGlobal() {
        if (Files.exists(GLOBAL_CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(GLOBAL_CONFIG_PATH)) {
                return GSON.fromJson(reader, TwitchConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new TwitchConfig(); // disabled by default
    }

    public void saveGlobal() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(GLOBAL_CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // Helper to get effective config for a player (personal > global)
    // ------------------------------------------------------------------------

    public static TwitchConfig getEffectiveConfig(UUID uuid) {
        TwitchConfig personal = loadForPlayer(uuid);
        if (personal.enabled && !personal.oauthToken.isEmpty()) {
            return personal;
        }
        // Fall back to global if personal is not set up
        return loadGlobal();
    }

    // Convenience check: does the player have a valid personal config?
    public static boolean hasPersonalConfig(UUID uuid) {
        TwitchConfig cfg = loadForPlayer(uuid);
        return cfg.enabled && !cfg.oauthToken.isEmpty();
    }
}