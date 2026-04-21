// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.messaging;

import dev.codexbat.streamerutils.IconGlyphs;
import dev.codexbat.streamerutils.PlayerSettings;
import dev.codexbat.streamerutils.SettingsStore;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.util.function.Supplier;

import static dev.codexbat.streamerutils.IconGlyphs.CHAT_FONT;
import static dev.codexbat.streamerutils.IconGlyphs.DEFAULT_FONT;

public final class MessageSender {

    static URI modrinthURL = URI.create("https://modrinth.com/mod/streamerutils");

    private MessageSender() {}

    /**
     * Sends a feedback message to the command source, automatically prefixed
     * according to the player's settings (if the source is a player).
     * If the source is not a player (e.g., console), uses the full prefix.
     */
    public static void sendFeedback(ServerCommandSource source, Supplier<Text> messageSupplier) {
        Text prefixed = getPrefixedMessage(source, messageSupplier.get());
        source.sendFeedback(() -> prefixed, false);
    }

    /**
     * Sends a feedback message with a static Text (not supplier).
     */
    public static void sendFeedback(ServerCommandSource source, Text message) {
        Text prefixed = getPrefixedMessage(source, message);
        source.sendFeedback(() -> prefixed, false);
    }

    private static Text getPrefixedMessage(ServerCommandSource source, Text original) {
        MutableText prefix = createPrefix(source);
        return prefix.append(original);
    }

    public static Text getPrefixForPlayer(ServerPlayerEntity player) {
        boolean useShort = false;
        try {
            PlayerSettings settings = SettingsStore.get(player.getUuid());
            useShort = settings.shortPrefix();
        } catch (Exception ignored) {}
        String prefixText = useShort ? "SU" : "StreamerUtils";
        return Text.literal("[")
                .formatted(MessageStyles.PREFIX_BRACKET_COLOR)
                .append(Text.literal(prefixText).formatted(MessageStyles.PREFIX_TEXT_COLOR))
                .append(Text.literal("] ").formatted(MessageStyles.PREFIX_BRACKET_COLOR));
    }

    private static MutableText createPrefix(ServerCommandSource source) {
        PlayerSettings settings = null;
        boolean useShort = false;

        try {
            var player = source.getPlayerOrThrow();
            settings = SettingsStore.get(player.getUuid());
            useShort = settings.shortPrefix();
        } catch (Exception ignored) {}

        String prefixText = useShort ? "SU" : "StreamerUtils";

        StyleSpriteSource iconFont =
                (settings != null)
                        ? IconGlyphs.resolveFont(settings, true)   // treat prefix as CHAT context
                        : IconGlyphs.TAB_FONT;

        MutableText icon = Text.literal(IconGlyphs.MOD_DARK_GREY)
                .formatted(MessageStyles.PREFIX_TEXT_COLOR)
                .styled(s -> s.withFont(iconFont));

        MutableText separator = Text.literal(" - ")
                .styled(s -> s.withFont(IconGlyphs.DEFAULT_FONT));

        MutableText label = Text.literal(prefixText)
                .formatted(MessageStyles.PREFIX_TEXT_COLOR)
                .styled(style -> style
                        .withFont(IconGlyphs.DEFAULT_FONT)
                        .withClickEvent(new ClickEvent.OpenUrl(modrinthURL))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Open StreamerUtils homepage")
                        )));

        return Text.literal("[")
                .formatted(MessageStyles.PREFIX_BRACKET_COLOR)
                .append(icon)
                .append(separator)
                .append(label)
                .append(Text.literal("] ").formatted(MessageStyles.PREFIX_BRACKET_COLOR));
    }
}