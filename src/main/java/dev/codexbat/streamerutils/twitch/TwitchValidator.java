// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import java.util.regex.Pattern;

public final class TwitchValidator {
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,25}$");
    private static final Pattern OAUTH_PATTERN = Pattern.compile("^oauth:[a-z0-9]{30,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[a-z0-9]{30}$", Pattern.CASE_INSENSITIVE);

    public static boolean isValidChannelName(String channel) {
        return channel != null && CHANNEL_PATTERN.matcher(channel).matches();
    }

    public static boolean isValidBotUsername(String username) {
        // Same pattern as channel name
        return username != null && CHANNEL_PATTERN.matcher(username).matches();
    }

    public static boolean isValidOAuthToken(String token) {
        return token != null && OAUTH_PATTERN.matcher(token).matches();
    }

    public static boolean isValidClientId(String clientId) {
        return clientId != null && CLIENT_ID_PATTERN.matcher(clientId).matches();
    }

    public static String getValidationError(String field, String value) {
        return switch (field) {
            case "channel" -> isValidChannelName(value) ? null : "Channel name must be 4-25 characters (letters, numbers, underscore)";
            case "bot_username" -> isValidBotUsername(value) ? null : "Bot username must be 4-25 characters (letters, numbers, underscore)";
            case "oauth" -> isValidOAuthToken(value) ? null : "OAuth token must start with 'oauth:' followed by alphanumeric characters";
            case "client_id" -> isValidClientId(value) ? null : "Client ID must be exactly 30 alphanumeric characters";
            default -> "Unknown field";
        };
    }
}