// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

public record PlayerSettings(
        String iconId,
        int colorRgb,
        boolean streamerLive,
        boolean shortPrefix,
        boolean joinMessageEnabled,
        FollowAlertMode followAlertMode,
        boolean twitchSetup,
        boolean separateChatIconFont
) {
    public static PlayerSettings defaults() {
        return new PlayerSettings(
                "none",
                0xFFFFFF,
                false,
                false,
                true,
                FollowAlertMode.BOTH,
                false,
                true
        );
    }

    public PlayerSettings withIcon(String iconId) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withColor(int colorRgb) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withStreamerLive(boolean streamerLive) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withShortPrefix(boolean shortPrefix) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withJoinMessageEnabled(boolean joinMessageEnabled) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withFollowAlertMode(FollowAlertMode mode) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, mode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withTwitchSetup(boolean twitchSetup) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }

    public PlayerSettings withSeparateChatIconFont(boolean separateChatIconFont) {
        return new PlayerSettings(iconId, colorRgb, streamerLive, shortPrefix, joinMessageEnabled, followAlertMode, twitchSetup, separateChatIconFont);
    }
}