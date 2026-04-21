// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.nio.file.Path;

public final class GlobalSettings {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("streamerutils.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String leftBracket = "<";
    private static String rightBracket = ">";

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    leftBracket = data.leftBracket != null ? data.leftBracket : "<";
                    rightBracket = data.rightBracket != null ? data.rightBracket : ">";
                }
            } catch (IOException e) {
                StreamerUtils.LOGGER.error("Failed to load global settings", e);
            }
        }
        save(); // write defaults if file didn't exist or was incomplete
    }

    public static void save() {
        Data data = new Data(leftBracket, rightBracket);
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            StreamerUtils.LOGGER.error("Failed to save global settings", e);
        }
    }

    public static void setBrackets(String left, String right) {
        leftBracket = left == null ? "" : left;
        rightBracket = right == null ? "" : right;
        save();
    }

    public static void resetBrackets() {
        leftBracket = "<";
        rightBracket = ">";
        save();
    }

    public static String getLeftBracket()  { return leftBracket; }
    public static String getRightBracket() { return rightBracket; }

    private static class Data {
        String leftBracket;
        String rightBracket;
        Data(String left, String right) { leftBracket = left; rightBracket = right; }
    }
}