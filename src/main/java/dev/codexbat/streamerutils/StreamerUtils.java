// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import dev.codexbat.streamerutils.command.StreamerUtilsCommands;
import dev.codexbat.streamerutils.messaging.MessageSender;
import dev.codexbat.streamerutils.messaging.MessageStyles;
import dev.codexbat.streamerutils.twitch.TwitchIntegration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

public class StreamerUtils implements ModInitializer {
	public static final String MOD_ID = "streamerutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	static URI docsURL = URI.create("https://github.com/codex-bat/StreamerUtils/wiki");
	static URI modrinthURL = URI.create("https://modrinth.com/mod/streamerutils");
	static URI githubURL   = URI.create("https://github.com/codex-bat/StreamerUtils");
	static URI authorURL   = URI.create("https://github.com/codex-bat");

	@Override
	public void onInitialize() {

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerSettings settings = SettingsStore.get(player.getUuid());
			if (settings.joinMessageEnabled()) {
				MessageSender.sendFeedback(player.getCommandSource(), () -> createJoinMessage(player));
			}
		});

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {

			ServerPlayerEntity player = sender;

			String rawMessage = message.getContent().getString();

			PlayerSettings settings = SettingsStore.get(player.getUuid());

			String left = GlobalSettings.getLeftBracket();
			String right = GlobalSettings.getRightBracket();

			Text icon = IconGlyphs.styledIcon(settings, true);

			Text name = Text.literal(player.getName().getString())
					.styled(style -> style.withColor(TextColor.fromRgb(settings.colorRgb())));

			Text formatted = Text.literal("")
					.append(Text.literal(left))
					.append(icon)
					.append(name)
					.append(Text.literal(right))
					.append(Text.literal(" "))
					.append(Text.literal(rawMessage));

			// Send to ALL players
			Objects.requireNonNull(player.getEntityWorld().getServer()).getPlayerManager().getPlayerList().forEach(p -> {
				p.sendMessage(formatted, false);
			});

			return false; // cancel vanilla chat
		});

		ServerLifecycleEvents.SERVER_STARTED.register(TwitchIntegration::init);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			TwitchIntegration.shutdown();
		});

		SettingsStore.load();
		GlobalSettings.load();
		NameplateManager.init();
		StreamerUtilsCommands.register();
		LOGGER.info("{} loaded", MOD_ID);
	}

	private static Text createJoinMessage(ServerPlayerEntity player) {
		Text description = Text.literal("This server runs ")
				.styled(style -> style.withColor(MessageStyles.DEFAULT_COLOR))
				.append(Text.literal("StreamerUtils")
						.styled(style -> style.withColor(MessageStyles.PREFIX_TEXT_COLOR))) // blue
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
				.styled(style -> style.withColor(Formatting.GREEN)
						.withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(modrinthURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("Download from Modrinth"))));

		Text githubButton = Text.literal("[GitHub]")
				.styled(style -> style.withColor(Formatting.GRAY)
						.withBold(true)
						.withClickEvent(new ClickEvent.OpenUrl(githubURL))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("View source on GitHub"))));

		Text downloadLine = Text.literal("• Download: ")
				.append(modrinthButton)
				.append(Text.literal(" / "))
				.append(githubButton)
				.append(Text.literal("\n"));

		Text authorLine = Text.literal("• Created by ")
				.append(Text.literal("Codex.bat")
						.styled(style -> style.withColor(Formatting.GOLD)
								.withClickEvent(new ClickEvent.OpenUrl(authorURL))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Author's GitHub")))))
				.append(Text.literal("\n"));

		Text disableButton = Text.literal("• ")
				.append(Text.literal("[Disable this message]")
						.styled(style -> style.withColor(Formatting.RED)
								.withBold(true)
								.withClickEvent(new ClickEvent.RunCommand("/su welcome off"))
								.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to never see this again")))));

		return Text.empty()
				.append(description)
				.append(helpLine)
				.append(docsLine)
				.append(downloadLine)
				.append(authorLine)
				.append(disableButton);
	}
}