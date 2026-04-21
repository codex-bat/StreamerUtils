// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

public enum FollowAlertMode {
    NONE("None", "No follow alerts"),
    PERSONAL("Personal", "Only alerts for your own Twitch channel"),
    GLOBAL("Global", "Only alerts for other streamers' channels"),
    BOTH("Both", "Alerts for all follows");

    private final String displayName;
    private final String description;

    FollowAlertMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    // I'm not used to enums
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}