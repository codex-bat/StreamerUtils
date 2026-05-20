// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji;

import dev.codexbat.streamerutils.GlobalSettings;
import dev.codexbat.streamerutils.IconGlyphs;
import dev.codexbat.streamerutils.messaging.MessageStyles;
import dev.codexbat.streamerutils.PlayerSettings;
import dev.codexbat.streamerutils.SettingsStore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class EmojiChatInterceptor {
    private EmojiChatInterceptor() {}

    public static Text buildChatLine(ServerPlayerEntity player, String rawMessage, boolean useChatFont) {
        PlayerSettings settings = SettingsStore.get(player.getUuid());

        String left = GlobalSettings.getLeftBracket();
        String right = GlobalSettings.getRightBracket();

        Text icon = IconGlyphs.styledIconForViewer(settings, useChatFont);

        Text name = Text.literal(player.getName().getString())
                .styled(style -> style.withColor(TextColor.fromRgb(settings.colorRgb())));

        Text message = EmojiParser.parse(player, rawMessage)
                .styled(style -> style.withColor(MessageStyles.CHAT_COLOR));

        return Text.literal("")
                .append(Text.literal(left))
                .append(icon)
                .append(name)
                .append(Text.literal(right))
                .append(Text.literal(" "))
                .append(message);
    }
}