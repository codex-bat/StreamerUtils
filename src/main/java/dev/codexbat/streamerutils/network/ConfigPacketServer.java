// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.network;

import dev.codexbat.streamerutils.*;
import dev.codexbat.streamerutils.emoji.core.EmojiDefinition; // needed for validation? Not in server, but just in case
import dev.codexbat.streamerutils.emoji.core.EmojiRegistry;
import dev.codexbat.streamerutils.twitch.SoundAlertManager;
import dev.codexbat.streamerutils.twitch.TwitchIntegration;
import dev.codexbat.streamerutils.network.Payloads;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigPacketServer {

    private static final Set<UUID> clientModPlayers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final ConcurrentHashMap<UUID, ClientMirrorState> mirroredState = new ConcurrentHashMap<>();

    public record ClientMirrorState(
            boolean configured,
            boolean live,
            long startedAtEpochMs,
            int viewerCount,
            String lastFollower
    ) {}

    public static void register() {
        // Register all incoming payloads
        PayloadTypeRegistry.playC2S().register(Payloads.ClientHelloPayload.ID, Payloads.ClientHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.ClientSetupPayload.ID, Payloads.ClientSetupPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.StreamStatePayload.ID, Payloads.StreamStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.FollowAlertPayload.ID, Payloads.FollowAlertPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.HighlightRequestPayload.ID, Payloads.HighlightRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.FireworkRequestPayload.ID, Payloads.FireworkRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetIconPayload.ID, Payloads.SetIconPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetColorPayload.ID, Payloads.SetColorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetPrefixPayload.ID, Payloads.SetPrefixPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetWelcomePayload.ID, Payloads.SetWelcomePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetAlertModePayload.ID, Payloads.SetAlertModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SetStreamLivePayload.ID, Payloads.SetStreamLivePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.RequestResetPayload.ID, Payloads.RequestResetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.PlayerSettingsPayload.ID, Payloads.PlayerSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.SyncSettingsPayload.ID, Payloads.SyncSettingsPayload.CODEC); // THE FIRST EVER S2C PACKET LET'S GO.
        PayloadTypeRegistry.playS2C().register(Payloads.SyncEmojiRegistryPayload.ID, Payloads.SyncEmojiRegistryPayload.CODEC);

        // ---------- Existing handlers (unchanged) ----------
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ClientHelloPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                UUID uuid = context.player().getUuid();
                clientModPlayers.add(uuid);
                StreamerUtils.LOGGER.info("Player {} has the client-side Twitch mod.", uuid);

                // ── NEW: claim the deferred welcome slot and send the client version.
                //         If joinMessageEnabled is false, or the slot already expired,
                //         trySendClientWelcome is a no-op.
                StreamerUtils.trySendClientWelcome(context.player());

                // Send current settings to the client
                PlayerSettings s = SettingsStore.get(uuid);
                ServerPlayNetworking.send(context.player(), new Payloads.SyncSettingsPayload(
                        s.iconId(),
                        s.colorRgb(),
                        s.streamerLive(),
                        s.followAlertMode().name(),
                        s.shortPrefix(),
                        s.joinMessageEnabled(),
                        s.separateChatIconFont()
                ));

                // Send the full emoji registry so the client can drive autocomplete.
                ServerPlayNetworking.send(context.player(), buildEmojiSyncPayload());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.ClientSetupPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                UUID uuid = context.player().getUuid();
                clientModPlayers.add(uuid);
                if (payload.configured()) {
                    PlayerSettings settings = SettingsStore.get(uuid);
                    SettingsStore.set(uuid, settings.withTwitchSetup(true));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.StreamStatePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = context.server();
                UUID uuid = player.getUuid();

                clientModPlayers.add(uuid);
                mirroredState.put(uuid, new ClientMirrorState(
                        true, payload.live(), payload.startedAtEpochMs(),
                        payload.viewerCount(), payload.lastFollower()
                ));

                PlayerSettings settings = SettingsStore.get(uuid);
                if (!settings.twitchSetup()) {
                    SettingsStore.set(uuid, settings.withTwitchSetup(true));
                }
                if (settings.streamerLive() != payload.live()) {
                    SettingsStore.set(uuid, settings.withStreamerLive(payload.live()));
                    NameplateManager.apply(server, player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.FollowAlertPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = context.server();
                UUID uuid = player.getUuid();

                clientModPlayers.add(uuid);
                mirroredState.compute(uuid, (id, oldState) -> {
                    if (oldState == null) {
                        return new ClientMirrorState(true, false, 0L, 0, payload.followerName());
                    }
                    return new ClientMirrorState(
                            oldState.configured(), oldState.live(),
                            oldState.startedAtEpochMs(), oldState.viewerCount(),
                            payload.followerName()
                    );
                });
                SoundAlertManager.playFollowSound(server, payload.followerName(), uuid);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.HighlightRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = context.server();
                UUID uuid = player.getUuid();

                clientModPlayers.add(uuid);
                ServerPlayerEntity streamer = findStreamerPlayer(server, uuid);
                if (streamer == null) return;

                streamer.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING, 200, 0, false, false, true));
                streamer.sendMessage(
                        Text.literal("@" + payload.viewerName() + " made the streamer glow!")
                                .formatted(Formatting.GRAY), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.FireworkRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = context.server();
                UUID uuid = player.getUuid();

                clientModPlayers.add(uuid);
                ServerPlayerEntity streamer = findStreamerPlayer(server, uuid);
                if (streamer == null) return;

                World world = streamer.getEntityWorld();
                int color = streamer.getRandom().nextInt(0xFFFFFF);
                var colors = new IntArrayList();
                colors.add(color);
                var fades = new IntArrayList();
                FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                        FireworkExplosionComponent.Type.BURST, colors, fades, false, false);
                FireworksComponent fireworks = new FireworksComponent(1, List.of(explosion));
                ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
                stack.set(DataComponentTypes.FIREWORKS, fireworks);

                Vec3d pos = streamer.getEntityPos();
                world.spawnEntity(new FireworkRocketEntity(world, pos.x, pos.y, pos.z, stack));
                streamer.sendMessage(
                        Text.literal("@" + payload.viewerName() + " launched a firework!")
                                .formatted(Formatting.GRAY), false);
            });
        });

        // ---------- New settings handlers ----------

        // Set Icon
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetIconPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                String iconId = payload.iconId();

                // Validate (reuse IconGlyphs logic from server, assuming it's accessible)
                if (!dev.codexbat.streamerutils.IconGlyphs.isValid(iconId)) return;
                if (dev.codexbat.streamerutils.IconGlyphs.isDeveloperOnly(iconId) && !player.getName().getString().equals("Codex_bat")) return;

                PlayerSettings s = SettingsStore.get(uuid);
                if (s.iconId().equals(iconId)) return;
                SettingsStore.set(uuid, s.withIcon(iconId));
                NameplateManager.apply(context.server(), player);

                // Send updated settings back
                sendSyncSettings(player);
            });
        });

        // Set Color
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetColorPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                int rgb = payload.rgb();
                PlayerSettings s = SettingsStore.get(uuid);
                if (s.colorRgb() == rgb) return;
                SettingsStore.set(uuid, s.withColor(rgb));
                NameplateManager.apply(context.server(), player);
                sendSyncSettings(player);
            });
        });

        // Set Prefix
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetPrefixPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                boolean shortPrefix = payload.shortPrefix();
                PlayerSettings s = SettingsStore.get(uuid);
                if (s.shortPrefix() == shortPrefix) return;
                SettingsStore.set(uuid, s.withShortPrefix(shortPrefix));
                sendSyncSettings(player);
            });
        });

        // Set Welcome
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetWelcomePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                boolean enabled = payload.enabled();
                PlayerSettings s = SettingsStore.get(uuid);
                if (s.joinMessageEnabled() == enabled) return;
                SettingsStore.set(uuid, s.withJoinMessageEnabled(enabled));
                sendSyncSettings(player);
            });
        });

        // Set Alert Mode
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetAlertModePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                String modeStr = payload.mode();
                FollowAlertMode mode;
                try {
                    mode = FollowAlertMode.valueOf(modeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return;
                }
                PlayerSettings s = SettingsStore.get(uuid);
                if (s.followAlertMode() == mode) return;
                SettingsStore.set(uuid, s.withFollowAlertMode(mode));
                sendSyncSettings(player);
            });
        });

        // Set Stream Live (toggle from client)
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetStreamLivePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                boolean live = payload.live();
                PlayerSettings s = SettingsStore.get(uuid);
                if (s.streamerLive() == live) return;

                SettingsStore.set(uuid, s.withStreamerLive(live));
                NameplateManager.apply(context.server(), player);
                // If going live and Twitch enabled, try connecting? Not necessary from client side.
                sendSyncSettings(player);
            });
        });

        // Reset requests
        ServerPlayNetworking.registerGlobalReceiver(Payloads.RequestResetPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                UUID uuid = player.getUuid();
                String target = payload.target();
                PlayerSettings current = SettingsStore.get(uuid);
                PlayerSettings defaults = PlayerSettings.defaults();
                boolean changed = false;

                switch (target) {
                    case "icon":
                        if (!current.iconId().equals(defaults.iconId())) {
                            SettingsStore.set(uuid, current.withIcon(defaults.iconId()));
                            changed = true;
                        }
                        break;
                    case "color":
                        if (current.colorRgb() != defaults.colorRgb()) {
                            SettingsStore.set(uuid, current.withColor(defaults.colorRgb()));
                            changed = true;
                        }
                        break;
                    case "stream":
                        if (current.streamerLive() != defaults.streamerLive()) {
                            SettingsStore.set(uuid, current.withStreamerLive(defaults.streamerLive()));
                            changed = true;
                        }
                        break;
                    case "follow":
                        if (current.followAlertMode() != defaults.followAlertMode()) {
                            SettingsStore.set(uuid, current.withFollowAlertMode(defaults.followAlertMode()));
                            changed = true;
                        }
                        break;
                    case "all":
                        if (!current.equals(defaults)) {
                            SettingsStore.set(uuid, defaults);
                            changed = true;
                        }
                        break;
                    default:
                        return;
                }
                if (changed) {
                    NameplateManager.apply(context.server(), player);
                    sendSyncSettings(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(
                Payloads.PlayerSettingsPayload.ID, (payload, context) ->
                        context.server().execute(() -> {
                            ServerPlayerEntity player = context.player();
                            UUID uuid = player.getUuid();
                            PlayerSettings current = SettingsStore.get(uuid);

                            FollowAlertMode mode;
                            try { mode = FollowAlertMode.valueOf(payload.followAlertMode()); }
                            catch (IllegalArgumentException e) { mode = FollowAlertMode.BOTH; }

                            // Validate icon — reject unknown or developer-only values silently.
                            String icon = IconGlyphs.isValid(payload.iconId())
                                    && !IconGlyphs.isDeveloperOnly(payload.iconId())
                                    ? payload.iconId() : current.iconId();

                            PlayerSettings updated = new PlayerSettings(
                                    icon,
                                    payload.colorRgb(),
                                    payload.streamerLive(),
                                    payload.shortPrefix(),
                                    payload.joinMessageEnabled(),
                                    mode,
                                    current.twitchSetup(),   // server-authoritative, not overwritten
                                    payload.separateChatIconFont()
                            );
                            SettingsStore.set(uuid, updated);
                            NameplateManager.apply(context.server(), player);
                        })
        );
    }

    /** Helper to send the current PlayerSettings as a SyncSettingsPayload to the player. */
    private static void sendSyncSettings(ServerPlayerEntity player) {
        PlayerSettings s = SettingsStore.get(player.getUuid());
        ServerPlayNetworking.send(player, new Payloads.SyncSettingsPayload(
                s.iconId(),
                s.colorRgb(),
                s.streamerLive(),
                s.followAlertMode().name(),
                s.shortPrefix(),
                s.joinMessageEnabled(),
                s.separateChatIconFont()
        ));
    }

    /** Emojis ^^ */
    private static Payloads.SyncEmojiRegistryPayload buildEmojiSyncPayload() {
        List<Payloads.EmojiEntry> entries = EmojiRegistry.all().stream()
                .map(def -> new Payloads.EmojiEntry(
                        def.name(),
                        def.glyph(),
                        new ArrayList<>(def.aliases())
                ))
                .toList();
        return new Payloads.SyncEmojiRegistryPayload(entries);
    }

    public static boolean hasClientMod(UUID uuid) {
        return clientModPlayers.contains(uuid);
    }

    public static void onPlayerLeave(UUID uuid) {
        clientModPlayers.remove(uuid);
        mirroredState.remove(uuid);
        TwitchIntegration.unloadForPlayer(uuid);
    }

    public static ClientMirrorState getMirroredState(UUID uuid) {
        return mirroredState.get(uuid);
    }

    public static boolean isClientManaged(UUID uuid) {
        return clientModPlayers.contains(uuid);
    }

    private static ServerPlayerEntity findStreamerPlayer(MinecraftServer server, UUID preferred) {
        if (preferred != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(preferred);
            if (player != null && SettingsStore.get(player.getUuid()).streamerLive()) {
                return player;
            }
        }
        return server.getPlayerManager().getPlayerList().stream()
                .filter(p -> SettingsStore.get(p.getUuid()).streamerLive())
                .findFirst().orElse(null);
    }

    private ConfigPacketServer() {}
}