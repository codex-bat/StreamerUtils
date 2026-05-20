// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import dev.codexbat.streamerutils.command.StreamerUtilsCommands;
import dev.codexbat.streamerutils.emoji.EmojiBootstrap;
import dev.codexbat.streamerutils.emoji.EmojiChatInterceptor;
import dev.codexbat.streamerutils.messaging.MessageSender;
import dev.codexbat.streamerutils.messaging.MessageStyles;
import dev.codexbat.streamerutils.network.ConfigPacketServer;
import dev.codexbat.streamerutils.twitch.TwitchIntegration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;    // NEW
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StreamerUtils implements ModInitializer {
	public static final String MOD_ID = "streamerutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// ── NEW: tracks players who have joined but haven't had their welcome sent yet.
	//         Value = server tick at which they joined (for the fallback timer).
	private static final ConcurrentHashMap<UUID, Long> pendingWelcome = new ConcurrentHashMap<>();

	// How many ticks to wait before giving up on a ClientHelloPayload and sending
	// the regular welcome instead.  60 ticks = 3 seconds.
	private static final long WELCOME_FALLBACK_MS = 320L; // 0.5 real-time seconds

	// The fake client-side command your client mod intercepts.
	// It is NOT registered on the server, so it produces no server output.
	public static final String CLIENT_OPEN_COMMAND = "/su_open";

	static URI docsURL     = URI.create("https://github.com/codex-bat/StreamerUtils/wiki");
	static URI modrinthURL = URI.create("https://modrinth.com/mod/streamerutils");
	static URI githubURL   = URI.create("https://github.com/codex-bat/StreamerUtils");
	static URI authorURL   = URI.create("https://github.com/codex-bat");

	private static final Set<String> GAME_MESSAGE_KEYS = Set.of(
			"chat.type.advancement.task",
			"chat.type.advancement.challenge",
			"chat.type.advancement.goal",
			"chat.type.admin",
			"chat.type.announcement"
	);

	private static final Set<String> COMMAND_FEEDBACK_KEYS = Set.of(
			"commands.kill.success.single",
			"commands.spawnpoint.success.single",
			"commands.give.success.single",
			"commands.gamemode.success.other",
			"commands.teleport.success.entity.single"
	);

	@Override
	public void onInitialize() {

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerSettings settings = SettingsStore.get(player.getUuid());

			// Broadcast styled join message to all viewers
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				PlayerSettings viewerSettings = SettingsStore.get(viewer.getUuid());
				boolean useChatFont = viewerSettings.separateChatIconFont();
				Text styledName = styledPlayerDisplay(player, false, useChatFont);
				Text msg = Text.translatable("multiplayer.player.joined", styledName);
				viewer.sendMessage(msg, false);
			}

			// ── NEW: defer the welcome message; ConfigPacketServer will claim it
			//         when ClientHelloPayload arrives.  If no hello comes within
			//         WELCOME_FALLBACK_TICKS the tick listener sends the regular one.
			if (settings.joinMessageEnabled()) {
				pendingWelcome.put(player.getUuid(), System.currentTimeMillis()); // single put, wall-clock ms
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			pendingWelcome.remove(player.getUuid());   // NEW: clean up on early disconnect
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				PlayerSettings viewerSettings = SettingsStore.get(viewer.getUuid());
				boolean useChatFont = viewerSettings.separateChatIconFont();
				Text styledName = styledPlayerDisplay(player, false, useChatFont);
				Text msg = Text.translatable("multiplayer.player.left", styledName);
				viewer.sendMessage(msg, false);
			}
		});

		// ── NEW: fallback tick listener — sends the plain welcome to any player
		//         whose ClientHelloPayload never arrived within the grace period.
		ServerTickEvents.END_SERVER_TICK.register(server -> {     // END_SERVER_TICK, not END
			if (pendingWelcome.isEmpty()) return;
			long now = System.currentTimeMillis();
			pendingWelcome.forEach((uuid, joinMs) -> {
				if (now - joinMs >= WELCOME_FALLBACK_MS) {
					pendingWelcome.remove(uuid);
					ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
					if (player != null) {
						MessageSender.sendFeedback(
								player.getCommandSource(),
								() -> createJoinMessage(player));
					}
				}
			});
		});

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			ServerPlayerEntity senderPlayer = sender;
			String rawMessage = message.getContent().getString();
			MinecraftServer server = Objects.requireNonNull(senderPlayer.getEntityWorld().getServer());
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				PlayerSettings viewerSettings = SettingsStore.get(viewer.getUuid());
				boolean useChatFont = viewerSettings.separateChatIconFont();
				Text formatted = EmojiChatInterceptor.buildChatLine(senderPlayer, rawMessage, useChatFont);
				viewer.sendMessage(formatted, false);
			}
			return false;
		});

		ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
			if (overlay) return true;
			if (!(message.getContent() instanceof TranslatableTextContent content)) return true;
			String key = content.getKey();
			if ("multiplayer.player.joined".equals(key) || "multiplayer.player.left".equals(key)) return false;
			if (!shouldRewriteGameMessage(key)) return true;
			Object[] args = content.getArgs();
			if (args.length == 0) return true;
			ServerPlayerEntity subject = resolvePlayer(server, args[0]);
			if (subject == null) return true;
			for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
				PlayerSettings viewerSettings = SettingsStore.get(viewer.getUuid());
				boolean useChatFont = viewerSettings.separateChatIconFont();
				Text rewritten = rewriteGameMessage(key, args, subject, useChatFont);
				viewer.sendMessage(rewritten, false);
			}
			return false;
		});

		ServerLifecycleEvents.SERVER_STARTED.register(TwitchIntegration::init);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TwitchIntegration.shutdown());

		SettingsStore.load();
		GlobalSettings.load();
		NameplateManager.init();
		EmojiBootstrap.init();
		StreamerUtilsCommands.register();
		ConfigPacketServer.register();

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				ConfigPacketServer.onPlayerLeave(handler.getPlayer().getUuid()));

		LOGGER.info("{} loaded", MOD_ID);
	}

	// ── NEW public API ────────────────────────────────────────────────────────
	/**
	 * Called by {@link ConfigPacketServer} when a {@code ClientHelloPayload} arrives.
	 * Claims the pending welcome slot and sends the client-aware version instead.
	 * Returns {@code true} if a welcome was actually sent (slot was still pending).
	 */
	public static boolean trySendClientWelcome(ServerPlayerEntity player) {
		PlayerSettings settings = SettingsStore.get(player.getUuid());
		if (!settings.joinMessageEnabled()) {
			pendingWelcome.remove(player.getUuid());
			return false;
		}
		Long joinTick = pendingWelcome.remove(player.getUuid());
		if (joinTick == null) return false; // already sent or welcome disabled
		MessageSender.sendFeedback(player.getCommandSource(), () -> createClientJoinMessage(player));
		return true;
	}
	// ─────────────────────────────────────────────────────────────────────────

	private static boolean shouldRewriteGameMessage(String key) {
		return key.startsWith("death.")
				|| GAME_MESSAGE_KEYS.contains(key)
				|| COMMAND_FEEDBACK_KEYS.contains(key);
	}

	private static final Set<String> ICON_PREFIX_STRINGS = Set.of(
			IconGlyphs.LIVE, IconGlyphs.HEART, IconGlyphs.CROWN_TAB,
			IconGlyphs.CROWN_BIG, IconGlyphs.YOUTUBE, IconGlyphs.TWITCH,
			IconGlyphs.KICK, IconGlyphs.CODEX, IconGlyphs.MOD_CYAN,
			IconGlyphs.MOD_GREY, IconGlyphs.MOD_BLUE, IconGlyphs.MOD_DARK_GREY,
			IconGlyphs.MOD_CYAN_BG, IconGlyphs.MOD_GREY_DEPTH, IconGlyphs.MOD_BLUE_DEPTH
	);

	private static String stripIconPrefix(String candidate) {
		boolean changed;
		do {
			changed = false;
			for (String icon : ICON_PREFIX_STRINGS) {
				if (candidate.startsWith(icon)) {
					candidate = candidate.substring(icon.length());
					changed = true;
					break;
				}
			}
			if (candidate.startsWith(" ")) { candidate = candidate.substring(1); changed = true; }
		} while (changed);
		return candidate;
	}

	private static ServerPlayerEntity resolvePlayer(MinecraftServer server, Object firstArg) {
		String candidate = null;
		if (firstArg instanceof Text text) candidate = text.getString();
		else if (firstArg instanceof String s) candidate = s;
		if (candidate == null || candidate.isBlank()) return null;
		String clean = stripIconPrefix(candidate).trim();
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(clean);
		if (player != null) return player;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (p.getName().getString().equalsIgnoreCase(clean)) return p;
		}
		return null;
	}

	private static Text rewriteGameMessage(String key, Object[] args,
										   ServerPlayerEntity subjectPlayer,
										   boolean viewerSeparateChatIconFont) {
		Object[] rewrittenArgs = args.clone();
		if (key.startsWith("death.")) {
			for (int i = 0; i < rewrittenArgs.length; i++) {
				ServerPlayerEntity player = resolvePlayer(
						Objects.requireNonNull(subjectPlayer.getEntityWorld().getServer()),
						rewrittenArgs[i]);
				if (player != null)
					rewrittenArgs[i] = styledPlayerDisplay(player, false, viewerSeparateChatIconFont);
			}
		} else {
			if (rewrittenArgs.length > 0)
				rewrittenArgs[0] = styledPlayerDisplay(subjectPlayer, false, viewerSeparateChatIconFont);
		}
		return Text.translatable(key, rewrittenArgs);
	}

	private static Text styledPlayerDisplay(ServerPlayerEntity player,
											boolean includeBrackets,
											boolean viewerUseChatFont) {
		PlayerSettings settings = SettingsStore.get(player.getUuid());
		Text icon = IconGlyphs.styledIconForViewer(settings, viewerUseChatFont);
		Text name = Text.literal(player.getName().getString())
				.styled(style -> style.withColor(TextColor.fromRgb(settings.colorRgb())));
		MutableText result = Text.empty();
		if (includeBrackets) {
			String left = GlobalSettings.getLeftBracket();
			if (!left.isEmpty()) result = result.append(Text.literal(left));
		}
		result = result.append(icon).append(name);
		if (includeBrackets) {
			String right = GlobalSettings.getRightBracket();
			if (!right.isEmpty()) result = result.append(Text.literal(right));
		}
		return result;
	}

	// ── Unchanged: welcome for vanilla / no-client-mod players ───────────────
	private static Text createJoinMessage(ServerPlayerEntity player) {
		Text description = Text.literal("This server runs ")
				.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR))
				.append(Text.literal("StreamerUtils")
						.styled(style -> style.withColor(MessageStyles.PREFIX_TEXT_COLOR)))
				.append(Text.literal(" – enhances chat with icons, colors, streamer status, and more.\n")
						.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR)));

		Text helpLine = Text.literal("• Use ")
				.append(Text.literal("/su help")
						.styled(style -> style.withColor(Formatting.AQUA)
								.withClickEvent(new ClickEvent.RunCommand("/su help"))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to see all commands")))))
				.append(Text.literal(" to see all commands.\n"));

		Text docsLine = Text.literal("• Documentation: ")
				.append(Text.literal("click here")
						.styled(style -> style.withColor(Formatting.GREEN)
								.withUnderline(true)
								.withClickEvent(new ClickEvent.OpenUrl(docsURL))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Open documentation")))))
				.append(Text.literal("\n"));

		Text modrinthButton = Text.literal("[Modrinth]")
				.styled(style -> style.withColor(Formatting.GREEN).withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(modrinthURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("Download from Modrinth"))));
		Text githubButton = Text.literal("[GitHub]")
				.styled(style -> style.withColor(Formatting.GRAY).withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(githubURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("View source on GitHub"))));

		Text downloadLine = Text.literal("• Download: ")
				.append(modrinthButton).append(Text.literal(" / ")).append(githubButton)
				.append(Text.literal("\n"));

		Text authorLine = Text.literal("• Created by ")
				.append(Text.literal("Codex.bat")
						.styled(style -> style.withColor(Formatting.GOLD)
								.withClickEvent(new ClickEvent.OpenUrl(authorURL))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Author's GitHub")))))
				.append(Text.literal("\n"));

		Text disableButton = Text.literal("• ")
				.append(Text.literal("[Disable this message]")
						.styled(style -> style.withColor(Formatting.RED).withBold(true)
								.withClickEvent(new ClickEvent.RunCommand("/su welcome off"))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to never see this again")))));

		return Text.empty()
				.append(description).append(helpLine).append(docsLine)
				.append(downloadLine).append(authorLine).append(disableButton);
	}

	// ── Welcome for players who have the client mod installed ─────────────────
	private static Text createClientJoinMessage(ServerPlayerEntity player) {
		Text description = Text.literal("This server runs ")
				.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR))
				.append(Text.literal("StreamerUtils")
						.styled(style -> style.withColor(MessageStyles.PREFIX_TEXT_COLOR)))
				.append(Text.literal(" – enhances chat with icons, colors, streamer status, and more.\n")
						.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR)));

		// Client-specific line — opens the settings GUI via the fake intercepted command.
		// The button sends CLIENT_OPEN_COMMAND (/su_open); your client mod intercepts
		// this command before it reaches the server and opens the settings screen instead.
		Text openButton = Text.literal("[Open Settings]")
				.styled(style -> style.withColor(Formatting.AQUA).withBold(true)
						.withClickEvent(new ClickEvent.RunCommand(CLIENT_OPEN_COMMAND))
						.withHoverEvent(new HoverEvent.ShowText(
								Text.literal("Click to open the StreamerConfigs menu"))));

		Text hotkeyHint = Text.literal(" or press ")
				.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR))
				.append(Text.literal("F8")
						.styled(style -> style.withColor(Formatting.YELLOW).withBold(true)))
				.append(Text.literal(" at any time")
						.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR)));

		Text settingsLine = Text.literal("• ")
				.append(openButton)
				.append(hotkeyHint)
				.append(Text.literal("\n"));

		Text helpLine = Text.literal("• Use ")
				.append(Text.literal("/su help")
						.styled(style -> style.withColor(Formatting.AQUA)
								.withClickEvent(new ClickEvent.RunCommand("/su help"))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to see all commands")))))
				.append(Text.literal(" to see all commands.\n"));

		Text docsLine = Text.literal("• Documentation: ")
				.append(Text.literal("click here")
						.styled(style -> style.withColor(Formatting.GREEN)
								.withUnderline(true)
								.withClickEvent(new ClickEvent.OpenUrl(docsURL))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Open documentation")))))
				.append(Text.literal("\n"));

		Text modrinthButton = Text.literal("[Modrinth]")
				.styled(style -> style.withColor(Formatting.GREEN).withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(modrinthURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("Download from Modrinth"))));
		Text githubButton = Text.literal("[GitHub]")
				.styled(style -> style.withColor(Formatting.GRAY).withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(githubURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("View source on GitHub"))));

		Text downloadLine = Text.literal("• Download: ")
				.append(modrinthButton).append(Text.literal(" / ")).append(githubButton)
				.append(Text.literal("\n"));

		Text authorLine = Text.literal("• Created by ")
				.append(Text.literal("Codex.bat")
						.styled(style -> style.withColor(Formatting.GOLD)
								.withClickEvent(new ClickEvent.OpenUrl(authorURL))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Author's GitHub")))))
				.append(Text.literal("\n"));

		Text disableButton = Text.literal("• ")
				.append(Text.literal("[Disable this message]")
						.styled(style -> style.withColor(Formatting.RED).withBold(true)
								.withClickEvent(new ClickEvent.RunCommand("/su welcome off"))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to never see this again")))));

		return Text.empty()
				.append(description)
				.append(settingsLine)   // ← client-specific row
				.append(helpLine)
				.append(docsLine)
				.append(downloadLine)
				.append(authorLine)
				.append(disableButton);
	}
}