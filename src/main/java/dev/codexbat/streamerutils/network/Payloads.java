// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class Payloads {

    /**
     * C→S: client mod is installed and ready to manage Twitch locally.
     * No secret data, no config data.
     */
    public record ClientHelloPayload() implements CustomPayload {
        public static final Id<ClientHelloPayload> ID =
                new Id<>(Identifier.of("streamerutils", "client_hello"));
        public static final PacketCodec<PacketByteBuf, ClientHelloPayload> CODEC =
                PacketCodec.unit(new ClientHelloPayload());

        @Override
        public Id<ClientHelloPayload> getId() {
            return ID;
        }
    }

    /**
     * C→S: client-side Twitch is configured and active for this player.
     * Server uses this to mark twitchSetup=true and avoid legacy Twitch startup.
     */
    public record ClientSetupPayload(boolean configured) implements CustomPayload {
        public static final Id<ClientSetupPayload> ID =
                new Id<>(Identifier.of("streamerutils", "client_setup"));
        public static final PacketCodec<PacketByteBuf, ClientSetupPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.BOOLEAN, ClientSetupPayload::configured,
                        ClientSetupPayload::new
                );

        @Override
        public Id<ClientSetupPayload> getId() {
            return ID;
        }
    }

    /**
     * C→S: periodic Twitch state mirror from the client-side mod.
     * This lets the server know whether the streamer is live and update UI/logic.
     */
    public record StreamStatePayload(
            boolean live,
            long startedAtEpochMs,
            int viewerCount,
            String lastFollower
    ) implements CustomPayload {
        public static final Id<StreamStatePayload> ID =
                new Id<>(Identifier.of("streamerutils", "stream_state"));
        public static final PacketCodec<PacketByteBuf, StreamStatePayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.BOOLEAN, StreamStatePayload::live,
                        PacketCodecs.LONG, StreamStatePayload::startedAtEpochMs,
                        PacketCodecs.INTEGER, StreamStatePayload::viewerCount,
                        PacketCodecs.STRING, StreamStatePayload::lastFollower,
                        StreamStatePayload::new
                );

        @Override
        public Id<StreamStatePayload> getId() {
            return ID;
        }
    }

    /**
     * C→S: follower alert request from client-side Twitch.
     */
    public record FollowAlertPayload(String followerName) implements CustomPayload {
        public static final Id<FollowAlertPayload> ID =
                new Id<>(Identifier.of("streamerutils", "follow_alert"));
        public static final PacketCodec<PacketByteBuf, FollowAlertPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, FollowAlertPayload::followerName,
                        FollowAlertPayload::new
                );

        @Override
        public Id<FollowAlertPayload> getId() {
            return ID;
        }
    }

    /**
     * C→S: chat command or Twitch event asking the server to apply glow.
     */
    public record HighlightRequestPayload(String viewerName) implements CustomPayload {
        public static final Id<HighlightRequestPayload> ID =
                new Id<>(Identifier.of("streamerutils", "highlight_request"));
        public static final PacketCodec<PacketByteBuf, HighlightRequestPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, HighlightRequestPayload::viewerName,
                        HighlightRequestPayload::new
                );

        @Override
        public Id<HighlightRequestPayload> getId() {
            return ID;
        }
    }

    /**
     * C→S: chat command or Twitch event asking the server to spawn a firework.
     */
    public record FireworkRequestPayload(String viewerName) implements CustomPayload {
        public static final Id<FireworkRequestPayload> ID =
                new Id<>(Identifier.of("streamerutils", "firework_request"));
        public static final PacketCodec<PacketByteBuf, FireworkRequestPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, FireworkRequestPayload::viewerName,
                        FireworkRequestPayload::new
                );

        @Override
        public Id<FireworkRequestPayload> getId() {
            return ID;
        }
    }

    // --- Appearance ---
    public record SetIconPayload(String iconId) implements CustomPayload {
        public static final Id<SetIconPayload> ID = new Id<>(Identifier.of("streamerutils", "set_icon"));
        public static final PacketCodec<PacketByteBuf, SetIconPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, SetIconPayload::iconId, SetIconPayload::new);
        @Override public Id<SetIconPayload> getId() { return ID; }
    }

    public record SetColorPayload(int rgb) implements CustomPayload {
        public static final Id<SetColorPayload> ID = new Id<>(Identifier.of("streamerutils", "set_color"));
        public static final PacketCodec<PacketByteBuf, SetColorPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, SetColorPayload::rgb, SetColorPayload::new);
        @Override public Id<SetColorPayload> getId() { return ID; }
    }

    public record SetPrefixPayload(boolean shortPrefix) implements CustomPayload {
        public static final Id<SetPrefixPayload> ID = new Id<>(Identifier.of("streamerutils", "set_prefix"));
        public static final PacketCodec<PacketByteBuf, SetPrefixPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOLEAN, SetPrefixPayload::shortPrefix, SetPrefixPayload::new);
        @Override public Id<SetPrefixPayload> getId() { return ID; }
    }

    public record SetWelcomePayload(boolean enabled) implements CustomPayload {
        public static final Id<SetWelcomePayload> ID = new Id<>(Identifier.of("streamerutils", "set_welcome"));
        public static final PacketCodec<PacketByteBuf, SetWelcomePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOLEAN, SetWelcomePayload::enabled, SetWelcomePayload::new);
        @Override public Id<SetWelcomePayload> getId() { return ID; }
    }

    public record SetAlertModePayload(String mode) implements CustomPayload {
        public static final Id<SetAlertModePayload> ID = new Id<>(Identifier.of("streamerutils", "set_alert_mode"));
        public static final PacketCodec<PacketByteBuf, SetAlertModePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, SetAlertModePayload::mode, SetAlertModePayload::new);
        @Override public Id<SetAlertModePayload> getId() { return ID; }
    }

    public record SetStreamLivePayload(boolean live) implements CustomPayload {
        public static final Id<SetStreamLivePayload> ID = new Id<>(Identifier.of("streamerutils", "set_stream_live"));
        public static final PacketCodec<PacketByteBuf, SetStreamLivePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.BOOLEAN, SetStreamLivePayload::live, SetStreamLivePayload::new);
        @Override public Id<SetStreamLivePayload> getId() { return ID; }
    }

    // --- Reset packets (optional, could be generic) ---
    public record RequestResetPayload(String target) implements CustomPayload { // "color","icon","stream","follow","all"
        public static final Id<RequestResetPayload> ID = new Id<>(Identifier.of("streamerutils", "request_reset"));
        public static final PacketCodec<PacketByteBuf, RequestResetPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, RequestResetPayload::target, RequestResetPayload::new);
        @Override public Id<RequestResetPayload> getId() { return ID; }
    }

    // --- Sync from server → client (to populate GUI) ---
    public record SyncSettingsPayload(String iconId, int colorRgb, boolean streamerLive,
                                      String alertMode, boolean shortPrefix, boolean welcomeEnabled, boolean separateChatFont) implements CustomPayload {
        public static final Id<SyncSettingsPayload> ID = new Id<>(Identifier.of("streamerutils", "sync_settings"));
        public static final PacketCodec<PacketByteBuf, SyncSettingsPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, SyncSettingsPayload::iconId,
                        PacketCodecs.INTEGER, SyncSettingsPayload::colorRgb,
                        PacketCodecs.BOOLEAN, SyncSettingsPayload::streamerLive,
                        PacketCodecs.STRING, SyncSettingsPayload::alertMode,
                        PacketCodecs.BOOLEAN, SyncSettingsPayload::shortPrefix,
                        PacketCodecs.BOOLEAN, SyncSettingsPayload::welcomeEnabled,
                        PacketCodecs.BOOLEAN, SyncSettingsPayload::separateChatFont,
                        SyncSettingsPayload::new
                );
        @Override public Id<SyncSettingsPayload> getId() { return ID; }
    }

    /**
     * A single emoji definition transferred over the wire.
     * Mirrors EmojiDefinition but uses List<String> for codec compatibility.
     */
    public record EmojiEntry(String name, String glyph, List<String> aliases) {
        public static final PacketCodec<PacketByteBuf, EmojiEntry> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING,
                        EmojiEntry::name,
                        PacketCodecs.STRING,
                        EmojiEntry::glyph,
                        PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING),
                        EmojiEntry::aliases,
                        EmojiEntry::new
                );
    }

    /**
     * S→C: full emoji registry snapshot.
     * Sent once after the client hello so the client can drive autocomplete
     * without baking the emoji list into the client jar.
     */
    public record SyncEmojiRegistryPayload(List<EmojiEntry> emojis) implements CustomPayload {
        public static final Id<SyncEmojiRegistryPayload> ID =
                new Id<>(Identifier.of("streamerutils", "sync_emoji_registry"));
        public static final PacketCodec<PacketByteBuf, SyncEmojiRegistryPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.collection(ArrayList::new, EmojiEntry.CODEC),
                        SyncEmojiRegistryPayload::emojis,
                        SyncEmojiRegistryPayload::new
                );
        @Override public Id<SyncEmojiRegistryPayload> getId() { return ID; }
    }

    /**
     * C→S: full player-settings snapshot from the GUI.
     * Rate-limited on the client; server applies each field to PlayerSettings.
     * Note: twitchSetup is server-determined and is intentionally excluded.
     */
    public record PlayerSettingsPayload(
            String  iconId,
            int     colorRgb,
            boolean streamerLive,
            boolean shortPrefix,
            boolean joinMessageEnabled,
            String  followAlertMode,
            boolean separateChatIconFont
    ) implements CustomPayload {
        public static final Id<PlayerSettingsPayload> ID =
                new Id<>(Identifier.of("streamerutils", "player_settings"));
        public static final PacketCodec<PacketByteBuf, PlayerSettingsPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING,  PlayerSettingsPayload::iconId,
                        PacketCodecs.INTEGER, PlayerSettingsPayload::colorRgb,
                        PacketCodecs.BOOLEAN, PlayerSettingsPayload::streamerLive,
                        PacketCodecs.BOOLEAN, PlayerSettingsPayload::shortPrefix,
                        PacketCodecs.BOOLEAN, PlayerSettingsPayload::joinMessageEnabled,
                        PacketCodecs.STRING,  PlayerSettingsPayload::followAlertMode,
                        PacketCodecs.BOOLEAN, PlayerSettingsPayload::separateChatIconFont,
                        PlayerSettingsPayload::new
                );
        @Override public Id<PlayerSettingsPayload> getId() { return ID; }
    }

    private Payloads() {}
}