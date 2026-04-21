// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, PlayerSettings>>() {}.getType();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve(StreamerUtils.MOD_ID);
    private static final Path FILE = CONFIG_DIR.resolve("players.json");

    private static final Map<UUID, PlayerSettings> CACHE = new HashMap<>();

    private SettingsStore() {}

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(FILE)) {
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(FILE)) {
                Map<UUID, PlayerSettings> loaded = GSON.fromJson(reader, MAP_TYPE);
                CACHE.clear();
                if (loaded != null) {
                    CACHE.putAll(loaded);
                }
            }
        } catch (Exception e) {
            StreamerUtils.LOGGER.error("Failed to load player settings", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(CACHE, MAP_TYPE, writer);
            }
        } catch (IOException e) {
            StreamerUtils.LOGGER.error("Failed to save player settings", e);
        }
    }

    public static PlayerSettings get(UUID uuid) {
        return CACHE.getOrDefault(uuid, PlayerSettings.defaults());
    }

    public static void set(UUID uuid, PlayerSettings settings) {
        CACHE.put(uuid, settings);
        save();
    }
}