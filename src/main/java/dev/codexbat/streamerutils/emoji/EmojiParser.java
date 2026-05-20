// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.emoji;

import dev.codexbat.streamerutils.emoji.smart.SmartEmojiResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmojiParser {
    private EmojiParser() {}

    private static final Pattern TOKEN = Pattern.compile(":([a-zA-Z0-9_+-]+):?");

    public static MutableText parse(ServerPlayerEntity player, String input) {
        MutableText out = Text.empty();
        Matcher matcher = TOKEN.matcher(input);
        int last = 0;

        while (matcher.find()) {
            int tokenStart = matcher.start();

            if (isEscaped(input, tokenStart)) {
                // Append text before the escape backslash
                if (tokenStart - 1 > last) {
                    out.append(Text.literal(input.substring(last, tokenStart - 1)));
                }

                // Append the token literally, without the backslash
                out.append(Text.literal(input.substring(tokenStart, matcher.end())));

                last = matcher.end();
                continue;
            }

            if (tokenStart > last) {
                out.append(Text.literal(input.substring(last, tokenStart)));
            }

            String token = matcher.group(1);

            SmartEmojiResolver.Result result = SmartEmojiResolver.resolve(player, token);

            if (result.confidence() == SmartEmojiResolver.Confidence.NONE) {
                out.append(Text.literal(matcher.group(0)));
            } else {
                out.append(Text.literal(result.glyph()).styled(style ->
                        style.withFont(EmojiFont.EMOJI_FONT)
                                .withColor(TextColor.fromFormatting(Formatting.WHITE))));

                if (result.confidence() == SmartEmojiResolver.Confidence.SUGGEST) {
                    out.append(Text.literal("{" + result.resolvedName() + "}")
                            .formatted(Formatting.DARK_GRAY));
                }
            }

            last = matcher.end();
        }

        if (last < input.length()) {
            out.append(Text.literal(input.substring(last)));
        }

        return out;
    }

    private static boolean isEscaped(String input, int tokenStart) {
        return tokenStart > 0 && input.charAt(tokenStart - 1) == '\\';
    }
}