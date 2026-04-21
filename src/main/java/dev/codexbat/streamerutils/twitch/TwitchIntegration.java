// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.eventsub.condition.ChannelFollowV2Condition;
import com.github.twitch4j.eventsub.events.ChannelFollowEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.Stream;
import dev.codexbat.streamerutils.NameplateManager;
import dev.codexbat.streamerutils.PlayerSettings;
import dev.codexbat.streamerutils.SettingsStore;
import dev.codexbat.streamerutils.StreamerUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TwitchIntegration {
    static MinecraftServer server;

    // Per‑player clients and configs
    private static final Map<UUID, PlayerTwitchContext> playerContexts = new ConcurrentHashMap<>();

    // Global fallback client (legacy / server‑wide)
    private static TwitchClient globalClient;
    private static TwitchConfig globalConfig;
    private static String globalChannelId;
    private static long globalStreamStartTime = 0;
    private static String globalLastFollower = "";
    private static final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    private static String globalLastKnownFollower = "";
    private static ScheduledExecutorService globalScheduler;
    private static final long OFFLINE_GRACE_PERIOD_MS = TimeUnit.MINUTES.toMillis(15);

    // ------------------------------------------------------------------------
    // Player context holder
    // ------------------------------------------------------------------------
    private static class PlayerTwitchContext {
        final TwitchClient client;
        final TwitchConfig config;
        final String channelId;
        long streamStartTime = 0;
        String lastFollower = "";
        final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
        String lastKnownFollower = "";
        ScheduledExecutorService scheduler;
        volatile boolean wasOnline = false;
        volatile long offlineSince = 0;

        PlayerTwitchContext(TwitchClient client, TwitchConfig config, String channelId) {
            this.client = client;
            this.config = config;
            this.channelId = channelId;
        }
    }

    // ------------------------------------------------------------------------
    // Initialization (called on server start)
    // ------------------------------------------------------------------------
    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        globalConfig = TwitchConfig.loadGlobal();
        if (globalConfig.enabled) {
            startGlobalClient();
        }
        StreamerUtils.LOGGER.info("Twitch integration initialized. Global config enabled: {}", globalConfig.enabled);
    }

    private static void startGlobalClient() {
        try {
            globalClient = buildClient(globalConfig);
            var users = globalClient.getHelix().getUsers(null, null, List.of(globalConfig.twitchChannelName)).execute().getUsers();
            if (users.isEmpty()) {
                StreamerUtils.LOGGER.error("Global Twitch channel not found: {}", globalConfig.twitchChannelName);
                globalConfig.enabled = false;
                return;
            }
            globalChannelId = users.get(0).getId();
            globalClient.getChat().joinChannel(globalConfig.twitchChannelName);

            registerEvents(globalClient, globalConfig, globalChannelId, null); // null uuid = global
            startSchedulerForContext(null);

            StreamerUtils.LOGGER.info("Global Twitch client started for channel: {}", globalConfig.twitchChannelName);
        } catch (Exception e) {
            StreamerUtils.LOGGER.error("Failed to start global Twitch client", e);
            globalConfig.enabled = false;
        }
    }

    // ------------------------------------------------------------------------
    // Per‑player client management
    // ------------------------------------------------------------------------
    public static void reloadForPlayer(UUID uuid, TwitchConfig config) {
        unloadForPlayer(uuid); // Clean up any existing client

        if (!config.enabled || config.oauthToken.isEmpty()) {
            StreamerUtils.LOGGER.info("Player {} Twitch config disabled or empty, using global fallback.", uuid);
            return;
        }

        try {
            TwitchClient client = buildClient(config);
            var users = client.getHelix().getUsers(null, null, List.of(config.twitchChannelName)).execute().getUsers();
            if (users.isEmpty()) {
                StreamerUtils.LOGGER.error("Player {} Twitch channel not found: {}", uuid, config.twitchChannelName);
                return;
            }
            String channelId = users.get(0).getId();
            client.getChat().joinChannel(config.twitchChannelName);

            PlayerTwitchContext ctx = new PlayerTwitchContext(client, config, channelId);
            playerContexts.put(uuid, ctx);

            registerEvents(client, config, channelId, uuid);
            startSchedulerForContext(uuid);

            StreamerUtils.LOGGER.info("Player {} Twitch client started for channel: {}", uuid, config.twitchChannelName);
        } catch (Exception e) {
            StreamerUtils.LOGGER.error("Failed to start Twitch client for player {}", uuid, e);
        }
    }

    public static void unloadForPlayer(UUID uuid) {
        PlayerTwitchContext ctx = playerContexts.remove(uuid);
        if (ctx != null) {
            if (ctx.scheduler != null) ctx.scheduler.shutdownNow();
            ctx.client.close();
            StreamerUtils.LOGGER.info("Player {} Twitch client unloaded.", uuid);
        }
    }

    public static boolean ensureClientForPlayer(UUID uuid, TwitchConfig config) {
        if (playerContexts.containsKey(uuid)) {
            return true; // already running
        }
        try {
            reloadForPlayer(uuid, config);
            return playerContexts.containsKey(uuid);
        } catch (Exception e) {
            StreamerUtils.LOGGER.error("Failed to start client for player {}", uuid, e);
            return false;
        }
    }

    private static TwitchClient buildClient(TwitchConfig cfg) {
        return TwitchClientBuilder.builder()
                .withClientId(cfg.clientId)
                .withEnableChat(true)
                .withEnableHelix(true)
                .withEnableEventSocket(true)
                .withDefaultAuthToken(new OAuth2Credential("twitch", cfg.oauthToken))
                .withChatAccount(new OAuth2Credential("twitch", cfg.oauthToken))
                .build();
    }

    // ------------------------------------------------------------------------
    // Event registration (works for both global and per‑player)
    // ------------------------------------------------------------------------
    @SuppressWarnings("deprecation")
    private static void registerEvents(TwitchClient client, TwitchConfig cfg, String channelId, UUID playerUuid) {
        // Chat message handling
        client.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
            handleChatMessage(event, playerUuid);
        });

        // Follow events (keep deprecated method as requested)
        String botUserId = client.getChat().getChannelNameToChannelId().get(cfg.botUsername);
        if (botUserId == null) {
            // Try to fetch via Helix
            var botUsers = client.getHelix().getUsers(null, null, List.of(cfg.botUsername)).execute().getUsers();
            if (!botUsers.isEmpty()) botUserId = botUsers.get(0).getId();
        }
        if (botUserId != null) {
            ChannelFollowV2Condition condition = ChannelFollowV2Condition.builder()
                    .broadcasterUserId(channelId)
                    .moderatorUserId(botUserId)
                    .build();
            client.getEventSocket().register(SubscriptionTypes.CHANNEL_FOLLOW_V2, condition);
            // pls, i know it's a fatal error, but I literally couldn't find a way around it. I'll just never ever update twitchj4 ig :')
        } else {
            StreamerUtils.LOGGER.warn("Could not determine bot user ID for follow events");
        }

        client.getEventManager().onEvent(ChannelFollowEvent.class, event -> {
            handleFollowEvent(event, playerUuid);
        });

        // go‑live event
        client.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> {
            if (event.getChannel().getId().equals(channelId)) {
                setStreamStartTime(playerUuid, System.currentTimeMillis());
            }
        });

        // Debug Subscription events
        client.getEventManager().onEvent(
                com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent.class,
                e -> System.out.println("SUB FAILED: " + e)
        );
        client.getEventManager().onEvent(
                com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent.class,
                e -> System.out.println("SUB OK: " + e)
        );
    }

    private static void startSchedulerForContext(UUID playerUuid) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            updateStreamInfo(playerUuid);
            checkNewFollowers(playerUuid);
        }, 0, 60, TimeUnit.SECONDS);

        if (playerUuid == null) {
            globalScheduler = scheduler;
        } else {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) ctx.scheduler = scheduler;
        }
    }

    // ------------------------------------------------------------------------
    // Event Handlers
    // ------------------------------------------------------------------------
    private static void handleChatMessage(ChannelMessageEvent event, UUID playerUuid) {
        String message = event.getMessage();
        String userDisplay = event.getUser().getName();
        Map<String, Long> cooldowns = getCooldowns(playerUuid);

        if (cooldowns.getOrDefault(userDisplay, 0L) > System.currentTimeMillis()) return;
        cooldowns.put(userDisplay, System.currentTimeMillis() + 5000);

        if (message.equalsIgnoreCase("!highlight")) {
            handleHighlight(playerUuid, userDisplay);
        } else if (message.equalsIgnoreCase("!firework")) {
            handleFirework(playerUuid, userDisplay);
        }
    }

    private static void handleFollowEvent(ChannelFollowEvent event, UUID broadcasterUuid) {
        String followerName = event.getUserName();
        setLastFollower(broadcasterUuid, followerName);
        SoundAlertManager.playFollowSound(server, followerName, broadcasterUuid);
    }

    private static void handleHighlight(UUID playerUuid, String viewer) {
        ServerPlayerEntity streamer = findStreamerPlayer(playerUuid);
        if (streamer == null) return;

        streamer.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING, 200, 0, false, false, true
        ));

        TwitchClient client = getClient(playerUuid);
        String channel = getChannelName(playerUuid);
        if (client != null && channel != null) {
            client.getChat().sendMessage(channel, "@" + viewer + " made the streamer glow!");
        }
    }

    private static void handleFirework(UUID playerUuid, String viewer) {
        ServerPlayerEntity streamer = findStreamerPlayer(playerUuid);
        if (streamer == null) return;

        World world = streamer.getEntityWorld();
        int color = streamer.getRandom().nextInt(0xFFFFFF);
        IntList colors = new IntArrayList();
        colors.add(color);
        IntList fades = new IntArrayList();

        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.BURST, colors, fades, false, false
        );
        FireworksComponent fireworks = new FireworksComponent(1, List.of(explosion));
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponentTypes.FIREWORKS, fireworks);

        Vec3d pos = streamer.getEntityPos();
        FireworkRocketEntity rocket = new FireworkRocketEntity(world, pos.x, pos.y, pos.z, stack);
        world.spawnEntity(rocket);

        TwitchClient client = getClient(playerUuid);
        String channel = getChannelName(playerUuid);
        if (client != null && channel != null) {
            client.getChat().sendMessage(channel, "@" + viewer + " launched a firework!");
        }
    }

    // ------------------------------------------------------------------------
    // Scheduled tasks
    // ------------------------------------------------------------------------
    private static void updateStreamInfo(UUID playerUuid) {
        TwitchClient client = getClient(playerUuid);
        String channelId = getChannelId(playerUuid);
        if (client == null || channelId == null) return;

        try {
            List<Stream> streams = client.getHelix().getStreams(
                    null, null, null, null, null, null, List.of(channelId), null
            ).execute().getStreams();

            boolean currentlyOnline = !streams.isEmpty();
            long now = System.currentTimeMillis();
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx == null) return;

            if (currentlyOnline) {
                Stream stream = streams.get(0);
                setStreamStartTime(playerUuid, stream.getStartedAtInstant().toEpochMilli());
                ctx.wasOnline = true;
                ctx.offlineSince = 0;
            } else {
                setStreamStartTime(playerUuid, 0);
                if (ctx.wasOnline) {
                    // Just went offline – record the time
                    ctx.offlineSince = now;
                    ctx.wasOnline = false;
                }

                // check if we should auto‑disable streamer mode
                if (ctx.offlineSince > 0 && (now - ctx.offlineSince) > OFFLINE_GRACE_PERIOD_MS) {
                    autoDisableStreamerMode(playerUuid);
                    ctx.offlineSince = 0; // prevent repeated attempts
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void autoDisableStreamerMode(UUID playerUuid) {
        // Only act if the player is still marked as live
        PlayerSettings settings = SettingsStore.get(playerUuid);
        if (!settings.streamerLive()) return;

        // Update settings and refresh nameplate
        SettingsStore.set(playerUuid, settings.withStreamerLive(false));
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player != null) {
            NameplateManager.apply(server, player);
            // Optionally notify the player
            player.sendMessage(Text.literal("Your stream appears to be offline. Stream Mode has been turned off.")
                    .formatted(Formatting.GRAY), false);
        }

        StreamerUtils.LOGGER.info("Auto‑disabled streamer mode for player {} (Twitch offline for >15 min)", playerUuid);
    }

    private static void checkNewFollowers(UUID playerUuid) {
        TwitchClient client = getClient(playerUuid);
        String channelId = getChannelId(playerUuid);
        if (client == null || channelId == null) return;

        try {
            var follows = client.getHelix().getChannelFollowers(
                    null, channelId, channelId, null, "1"
            ).execute();
            var result = follows.getFollows();
            if (!result.isEmpty()) {
                String newest = result.get(0).getUserName();
                String lastKnown = getLastKnownFollower(playerUuid);
                if (!newest.equals(lastKnown)) {
                    setLastKnownFollower(playerUuid, newest);
                    setLastFollower(playerUuid, newest);
                    // Pass the broadcaster UUID (playerUuid) as third argument
                    SoundAlertManager.playFollowSound(server, newest, playerUuid);
                    // No broadcast here – SoundAlertManager handles it per player settings
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // Helper methods to Get the correct context
    // ------------------------------------------------------------------------
    private static TwitchClient getClient(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.client;
        }
        return globalClient;
    }

    private static String getChannelId(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.channelId;
        }
        return globalChannelId;
    }

    private static String getChannelName(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.config.twitchChannelName;
        }
        return globalConfig != null ? globalConfig.twitchChannelName : null;
    }

    private static Map<String, Long> getCooldowns(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.cooldowns;
        }
        return globalCooldowns;
    }

    private static void setStreamStartTime(UUID playerUuid, long time) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) ctx.streamStartTime = time;
        } else {
            globalStreamStartTime = time;
        }
    }

    private static long getStreamStartTime(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.streamStartTime;
        }
        return globalStreamStartTime;
    }

    private static void setLastFollower(UUID playerUuid, String name) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) ctx.lastFollower = name;
        } else {
            globalLastFollower = name;
        }
    }

    private static String getLastFollower(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.lastFollower;
        }
        return globalLastFollower;
    }

    private static void setLastKnownFollower(UUID playerUuid, String name) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) ctx.lastKnownFollower = name;
        } else {
            globalLastKnownFollower = name;
        }
    }

    private static String getLastKnownFollower(UUID playerUuid) {
        if (playerUuid != null) {
            PlayerTwitchContext ctx = playerContexts.get(playerUuid);
            if (ctx != null) return ctx.lastKnownFollower;
        }
        return globalLastKnownFollower;
    }

    // ------------------------------------------------------------------------
    // Find the streamer player
    // ------------------------------------------------------------------------
    private static ServerPlayerEntity findStreamerPlayer(UUID specificUuid) {
        if (specificUuid != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(specificUuid);
            if (player != null && SettingsStore.get(player.getUuid()).streamerLive()) {
                return player;
            }
        }
        // Fallback: first player with streamerLive = true
        return server.getPlayerManager().getPlayerList().stream()
                .filter(p -> SettingsStore.get(p.getUuid()).streamerLive())
                .findFirst()
                .orElse(null);
    }

    // ------------------------------------------------------------------------
    // Public API (used by commands)
    // ------------------------------------------------------------------------
    public static String getStreamInfo() {
        // Find the active streamer
        ServerPlayerEntity streamerPlayer = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> SettingsStore.get(p.getUuid()).streamerLive())
                .findFirst()
                .orElse(null);

        TwitchConfig cfg = null;
        TwitchClient client = null;
        String channelId = null;
        long streamStart = 0;
        String lastFollower = "";

        if (streamerPlayer != null) {
            UUID uuid = streamerPlayer.getUuid();
            cfg = TwitchConfig.getEffectiveConfig(uuid); // personal if valid, else global

            if (cfg.enabled) {
                PlayerTwitchContext ctx = playerContexts.get(uuid);
                if (ctx == null) {
                    // Try to start the client now (lazy)
                    StreamerUtils.LOGGER.info("Streamer {} requested info but client not running. Attempting to start.", uuid);
                    ensureClientForPlayer(uuid, cfg);
                    ctx = playerContexts.get(uuid);
                }
                if (ctx != null) {
                    client = ctx.client;
                    channelId = ctx.channelId;
                    streamStart = ctx.streamStartTime;
                    lastFollower = ctx.lastFollower;
                } else {
                    // Client failed to start – report clearly
                    return "Twitch client failed to start. Check logs or use /su twitch status.";
                }
            } else {
                return "Twitch not configured. Use /su twitch setup.";
            }
        } else {
            // No active streamer – show global or nothing
            cfg = globalConfig;
            client = globalClient;
            channelId = globalChannelId;
            streamStart = globalStreamStartTime;
            lastFollower = globalLastFollower;
        }

        if (cfg == null || !cfg.enabled) return "Twitch integration disabled.";
        if (client == null) return "Twitch client not initialized.";

        try {
            long viewerCount = 0;
            String uptime = "Offline";
            if (streamStart > 0) {
                long duration = System.currentTimeMillis() - streamStart;
                long hours = duration / 3600000;
                long minutes = (duration % 3600000) / 60000;
                uptime = hours + "h " + minutes + "m";
                List<Stream> streams = client.getHelix().getStreams(
                        null, null, null, null, null, null, List.of(channelId), null
                ).execute().getStreams();
                if (!streams.isEmpty()) viewerCount = streams.get(0).getViewerCount();
            }
            String followerInfo = lastFollower.isEmpty() ? "No recent followers" : "Latest: " + lastFollower;
            return String.format("Stream: %s | Viewers: %d | Uptime: %s | %s",
                    cfg.twitchChannelName, viewerCount, uptime, followerInfo);
        } catch (Exception e) {
            return "Error fetching stream info.";
        }
    }

    public static void shutdown() {
        playerContexts.values().forEach(ctx -> {
            if (ctx.scheduler != null) ctx.scheduler.shutdownNow();
            ctx.client.close();
        });
        playerContexts.clear();
        if (globalScheduler != null) globalScheduler.shutdownNow();
        if (globalClient != null) globalClient.close();
        StreamerUtils.LOGGER.info("Twitch integration shut down.");
    }
}