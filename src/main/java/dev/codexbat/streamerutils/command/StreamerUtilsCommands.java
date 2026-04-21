// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.codexbat.streamerutils.*;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import dev.codexbat.streamerutils.messaging.MessageSender;
import dev.codexbat.streamerutils.messaging.MessageStyles;
import dev.codexbat.streamerutils.twitch.TwitchConfig;
import dev.codexbat.streamerutils.twitch.TwitchIntegration;
import dev.codexbat.streamerutils.twitch.TwitchValidator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.*;
import java.util.function.BiFunction;

import static dev.codexbat.streamerutils.IconGlyphs.CHAT_FONT;
import static dev.codexbat.streamerutils.IconGlyphs.DEFAULT_FONT;

public final class StreamerUtilsCommands {
    private static final SimpleCommandExceptionType INVALID_COLOR =
            new SimpleCommandExceptionType(Text.literal("Use #RRGGBB or a color name (red, blue, gold, etc.)"));
    private static final SimpleCommandExceptionType INVALID_ICON =
            new SimpleCommandExceptionType(Text.literal("Unknown icon. Use /su icon list."));

    // Aliases for user input (case‑insensitive)
    private static final Map<String, Integer> ALIASES = new HashMap<>();

    // Official Minecraft color names (the 16 Formatting colors)
    private static final Map<String, Integer> OFFICIAL_COLORS = new HashMap<>();
    // Reverse map: RGB → official name (lowercase with underscores)
    private static final Map<Integer, String> REVERSE_OFFICIAL = new HashMap<>();

    private static final Set<String> DISALLOWED_ICONS = Set.of("live", "crown_tab");

    static {
        // 16 official Minecraft colors (as used by Formatting)
        OFFICIAL_COLORS.put("black", 0x000000);
        OFFICIAL_COLORS.put("dark_blue", 0x0000AA);
        OFFICIAL_COLORS.put("dark_green", 0x00AA00);
        OFFICIAL_COLORS.put("dark_aqua", 0x00AAAA);
        OFFICIAL_COLORS.put("dark_red", 0xAA0000);
        OFFICIAL_COLORS.put("dark_purple", 0xAA00AA);
        OFFICIAL_COLORS.put("gold", 0xFFAA00);
        OFFICIAL_COLORS.put("gray", 0xAAAAAA);
        OFFICIAL_COLORS.put("dark_gray", 0x555555);
        OFFICIAL_COLORS.put("blue", 0x5555FF);
        OFFICIAL_COLORS.put("green", 0x55FF55);
        OFFICIAL_COLORS.put("aqua", 0x55FFFF);
        OFFICIAL_COLORS.put("red", 0xFF5555);
        OFFICIAL_COLORS.put("light_purple", 0xFF55FF);
        OFFICIAL_COLORS.put("yellow", 0xFFFF55);
        OFFICIAL_COLORS.put("white", 0xFFFFFF);

        // Build reverse map
        for (Map.Entry<String, Integer> entry : OFFICIAL_COLORS.entrySet()) {
            REVERSE_OFFICIAL.put(entry.getValue(), entry.getKey());
        }

        // Aliases (all map to the RGB of the official color)
        ALIASES.put("cyan", OFFICIAL_COLORS.get("aqua"));
        ALIASES.put("magenta", OFFICIAL_COLORS.get("light_purple"));
        ALIASES.put("lightpurple", OFFICIAL_COLORS.get("light_purple"));
        ALIASES.put("grey", OFFICIAL_COLORS.get("gray"));
        ALIASES.put("darkgrey", OFFICIAL_COLORS.get("dark_gray"));
        ALIASES.put("darkgray", OFFICIAL_COLORS.get("dark_gray"));
        ALIASES.put("dred", OFFICIAL_COLORS.get("dark_red"));
        ALIASES.put("dblue", OFFICIAL_COLORS.get("dark_blue"));
        ALIASES.put("dgreen", OFFICIAL_COLORS.get("dark_green"));
        ALIASES.put("daqua", OFFICIAL_COLORS.get("dark_aqua"));
        ALIASES.put("dpurple", OFFICIAL_COLORS.get("dark_purple"));
        ALIASES.put("dgrey", OFFICIAL_COLORS.get("dark_gray"));
        ALIASES.put("dgray", OFFICIAL_COLORS.get("dark_gray"));
        ALIASES.put("darkred", OFFICIAL_COLORS.get("dark_red"));
        ALIASES.put("darkblue", OFFICIAL_COLORS.get("dark_blue"));
        ALIASES.put("darkgreen", OFFICIAL_COLORS.get("dark_green"));
        ALIASES.put("darkaqua", OFFICIAL_COLORS.get("dark_aqua"));
        ALIASES.put("darkpurple", OFFICIAL_COLORS.get("dark_purple"));
    }

    private StreamerUtilsCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(buildRoot("su"));
            dispatcher.register(buildRoot("streamerutils"));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRoot(String root) {
        return CommandManager.literal(root)
                .executes(context -> showStatus(context.getSource()))
                .then(CommandManager.literal("icon")
                        .then(CommandManager.literal("tinytakeover")
                                .executes(context -> chatIconsMenu(context.getSource()))
                        )
                        .then(CommandManager.literal("chaticons")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setChatIcons(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "value")
                                        ))
                                )
                        )
                        .then(CommandManager.literal("list")
                                .executes(context -> listIcons(context.getSource())))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("icon", StringArgumentType.word())
                                        .executes(context -> setIcon(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "icon")
                                        )))))
                .then(CommandManager.literal("color")
                        .then(CommandManager.literal("list")
                                .executes(context -> listColors(context.getSource())))
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .executes(context -> setColor(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "color")
                                ))))
                .then(CommandManager.literal("stream")
                        .then(CommandManager.literal("info")
                                .executes(context -> showStreamInfo(context.getSource())))
                        .then(CommandManager.literal("start")
                                .executes(context -> setStreamerLive(context.getSource(), true)))
                        .then(CommandManager.literal("stop")
                                .executes(context -> setStreamerLive(context.getSource(), false))))
                // Reset commands
                .then(CommandManager.literal("reset")
                        .then(CommandManager.literal("color")
                                .executes(context -> resetColor(context.getSource())))
                        .then(CommandManager.literal("icon")
                                .executes(context -> resetIcon(context.getSource())))
                        .then(CommandManager.literal("stream")
                                .executes(context -> resetStream(context.getSource())))
                        .then(CommandManager.literal("follow")
                                .executes(context -> resetFollow(context.getSource())))
                        .then(CommandManager.literal("all")
                                .executes(context -> resetAll(context.getSource()))))
                .then(CommandManager.literal("prefix")
                        .executes(context -> togglePrefix(context.getSource())))
                .then(CommandManager.literal("welcome")
                        .then(CommandManager.literal("on")
                                .executes(context -> setWelcome(context.getSource(), true)))
                        .then(CommandManager.literal("off")
                                .executes(context -> setWelcome(context.getSource(), false))))
                .then(CommandManager.literal("twitch")
                        .then(CommandManager.literal("setup")
                                .then(CommandManager.argument("channel", StringArgumentType.word())
                                        .then(CommandManager.argument("bot_username", StringArgumentType.word())
                                                .then(CommandManager.argument("oauth", StringArgumentType.string())
                                                        .then(CommandManager.argument("client_id", StringArgumentType.string())
                                                                .executes(context -> setupTwitch(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "channel"),
                                                                        StringArgumentType.getString(context, "bot_username"),
                                                                        StringArgumentType.getString(context, "oauth"),
                                                                        StringArgumentType.getString(context, "client_id")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("channel")
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .executes(context -> setTwitchField(
                                                        context.getSource(),
                                                        "channel",
                                                        StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("bot_username")
                                        .then(CommandManager.argument("value", StringArgumentType.word())
                                                .executes(context -> setTwitchField(
                                                        context.getSource(),
                                                        "bot_username",
                                                        StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("oauth")
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .executes(context -> setTwitchField(
                                                        context.getSource(),
                                                        "oauth",
                                                        StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                                .then(CommandManager.literal("client_id")
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .executes(context -> setTwitchField(
                                                        context.getSource(),
                                                        "client_id",
                                                        StringArgumentType.getString(context, "value")
                                                ))
                                        )
                                )
                        )
                        .then(CommandManager.literal("reset")
                                .executes(context -> resetTwitch(context.getSource()))
                        )
                        .then(CommandManager.literal("status")
                                .executes(context -> showTwitchStatus(context.getSource()))
                        )
                        .then(CommandManager.literal("help")
                                .executes(context -> twitchHelp(context.getSource()))
                        )
                )
                .then(CommandManager.literal("help")
                        .executes(context -> help(context.getSource())))
                .then(CommandManager.literal("soundalert")
                        .executes(context -> soundalertHelp(context.getSource()))
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .executes(context -> setFollowAlertMode(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "mode")
                                ))
                        )
                        .then(CommandManager.literal("status")
                                .executes(context -> showFollowAlertStatus(context.getSource()))
                        )
                )
                .then(CommandManager.literal("bracket")
                        .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("left", StringArgumentType.string())
                                        .executes(context -> setBracket(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "left"),
                                                null
                                        ))
                                        .then(CommandManager.argument("right", StringArgumentType.string())
                                                .executes(context -> setBracket(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "left"),
                                                        StringArgumentType.getString(context, "right")
                                                )))))
                        .then(CommandManager.literal("reset")
                                .executes(context -> resetBracket(context.getSource()))));
    }

    private static int help(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        MutableText message = Text.literal("=== StreamerUtils Commands ===\n")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true));

        record CommandHelp(String usage, String description) {}

        // Always shown commands
        List<CommandHelp> commands = new ArrayList<>(List.of(
                new CommandHelp("/su", "Show current settings"),
                new CommandHelp("/su icon list", "List all available icons"),
                new CommandHelp("/su icon set <icon>", "Set your chat icon"),
                new CommandHelp("/su icon tinytakeover", "Toggle between separate and shared chat icons"),
                new CommandHelp("/su icon chaticons <true/false>", "Set chat icon font mode directly"),
                new CommandHelp("/su color list", "List all colors"),
                new CommandHelp("/su color <color>", "Set your name color (name or #RRGGBB)"),
                new CommandHelp("/su stream start/stop", "Toggle streamer live status"),
                new CommandHelp("/su stream info", "Show stream stats (your own or the active streamer)"),
                new CommandHelp("/su soundalert mode <mode>", "Configure follow alert preferences"),
                new CommandHelp("/su soundalert status", "Show current alert mode"),
                new CommandHelp("/su prefix", "Toggle short/long prefix"),
                new CommandHelp("/su welcome on/off", "Enable/disable the welcome message"),
                new CommandHelp("/su reset [icon|color|stream|follow|all]", "Reset specific or all cosmetic settings"),
                new CommandHelp("/su twitch setup <channel> <bot> <oauth> <client_id>", "Full Twitch setup (store credentials)"),
                new CommandHelp("/su twitch help", "Show all Twitch subcommands (set, status, reset)"),
                new CommandHelp("/su help", "Show this help page")
        ));

        // OP-only commands
        PermissionSourcePredicate<PermissionSource> gamemasterPerm =
                CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK);
        if (gamemasterPerm.test(source)) {
            commands.add(new CommandHelp("/su bracket set <left> [right]", "Set global chat brackets (OP only)"));
            commands.add(new CommandHelp("/su bracket reset", "Reset global brackets to default (OP only)"));
        }

        for (CommandHelp cmd : commands) {
            Text commandText = Text.literal(cmd.usage)
                    .styled(style -> style.withColor(Formatting.AQUA)
                            .withClickEvent(new ClickEvent.SuggestCommand(cmd.usage))
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to suggest command"))));
            MutableText line = Text.literal("• ")
                    .append(commandText)
                    .append(Text.literal(" – "))
                    .append(Text.literal(cmd.description).styled(style -> style.withColor(Formatting.GRAY)));
            message.append(line).append(Text.literal("\n"));
        }

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int soundalertHelp(ServerCommandSource source) {
        MutableText msg = Text.literal("Sound Alert Modes\n").formatted(Formatting.GOLD, Formatting.BOLD);
        msg.append(Text.literal("Choose who you want to receive follow alerts from:\n\n").formatted(Formatting.GRAY));

        for (FollowAlertMode mode : FollowAlertMode.values()) {
            MutableText line = Text.literal("• ")
                    .append(Text.literal(mode.getDisplayName())
                            .styled(s -> s.withColor(Formatting.AQUA)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/su soundalert " + mode.name().toLowerCase()))
                                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to set mode to " + mode.getDisplayName())))))
                    .append(Text.literal(" – "))
                    .append(Text.literal(mode.getDescription()).formatted(Formatting.GRAY));
            msg.append(line).append("\n");
        }

        msg.append(Text.literal("\nUsage: /su soundalert <mode>").formatted(Formatting.DARK_GRAY));
        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static int twitchHelp(ServerCommandSource source) {
        MutableText msg = Text.literal("Twitch Integration Help\n").formatted(Formatting.AQUA, Formatting.BOLD);

        record CommandHelp(String usage, String description) {}
        List<CommandHelp> commands = List.of(
                new CommandHelp("/su twitch setup <channel> <bot> <oauth> <client_id>", "Complete initial setup"),
                new CommandHelp("/su twitch set channel <value>", "Set channel name"),
                new CommandHelp("/su twitch set bot_username <value>", "Set bot username"),
                new CommandHelp("/su twitch set oauth <value>", "Set OAuth token"),
                new CommandHelp("/su twitch set client_id <value>", "Set client ID"),
                new CommandHelp("/su twitch status", "View current configuration"),
                new CommandHelp("/su twitch reset", "Remove personal config")
        );

        for (CommandHelp cmd : commands) {
            Text commandText = Text.literal(cmd.usage)
                    .styled(style -> style.withColor(Formatting.AQUA)
                            .withClickEvent(new ClickEvent.SuggestCommand(cmd.usage))
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to suggest command"))));
            MutableText line = Text.literal("• ")
                    .append(commandText)
                    .append(Text.literal(" – "))
                    .append(Text.literal(cmd.description).styled(style -> style.withColor(Formatting.GRAY)));
            msg.append(line).append("\n");
        }

        msg.append(Text.literal("\nTip: Use ")
                        .formatted(Formatting.DARK_GRAY))
                .append(Text.literal("/su twitch set oauth ")
                        .styled(s -> s.withColor(Formatting.GRAY)
                                .withClickEvent(new ClickEvent.SuggestCommand("/su twitch set oauth "))))
                .append(Text.literal(" and paste your token from Twitch.").formatted(Formatting.DARK_GRAY));

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static int setWelcome(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());
        if (current.joinMessageEnabled() == enabled) {
            MessageSender.sendFeedback(source, Text.literal("Welcome message is already " + (enabled ? "ON" : "OFF"))
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }
        SettingsStore.set(player.getUuid(), current.withJoinMessageEnabled(enabled));
        MessageSender.sendFeedback(source, Text.literal("Welcome message turned " + (enabled ? "ON" : "OFF"))
                .formatted(MessageStyles.DEFAULT_COLOR));
        return 1;
    }

    private static int showStatus(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings s = SettingsStore.get(player.getUuid());

        MutableText msg = Text.literal("Your Settings\n")
                .formatted(MessageStyles.HEADER_COLOR, Formatting.BOLD);

        // Icon (click to list)
        msg.append(Text.literal("\n• Icon: ").formatted(MessageStyles.LABEL_COLOR)
                .append(styledIconText(source, s.iconId(), true))
                .append(Text.literal(" [Change]")
                        .styled(style -> style.withColor(Formatting.AQUA)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su icon list"))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to view available icons"))))));

        // Color (click to list)
        msg.append(Text.literal("\n• Color: ").formatted(MessageStyles.LABEL_COLOR)
                .append(styledColorText(s.colorRgb()))
                .append(Text.literal(" [Change]")
                        .styled(style -> style.withColor(Formatting.AQUA)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su color list"))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to view available colors"))))));

        // Stream Live (click to toggle)
        msg.append(Text.literal("\n• Stream Live: ").formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(s.streamerLive() ? "ON" : "OFF")
                        .formatted(s.streamerLive() ? Formatting.GREEN : Formatting.GRAY))
                .append(Text.literal(" [Toggle]")
                        .styled(style -> style.withColor(Formatting.AQUA)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su stream " + (s.streamerLive() ? "stop" : "start")))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to toggle"))))));

        // Follow Alerts (click to open soundalert help)
        msg.append(Text.literal("\n• Follow Alerts: ").formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(s.followAlertMode().getDisplayName()).formatted(Formatting.YELLOW))
                .append(Text.literal(" [Change]")
                        .styled(style -> style.withColor(Formatting.AQUA)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su soundalert"))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to change alert mode"))))));

        // Prefix Mode (click to toggle)
        msg.append(Text.literal("\n• Prefix Mode: ").formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(s.shortPrefix() ? "SHORT" : "FULL")
                        .formatted(MessageStyles.PREFIX_TEXT_COLOR))
                .append(Text.literal(" [Toggle]")
                        .styled(style -> style.withColor(Formatting.AQUA)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su prefix"))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to toggle"))))));

        MessageSender.sendFeedback(source, msg);
        return 1;
        // As you can tell, I love clicks :p
    }

    private static int showStreamInfo(ServerCommandSource source) {
        String info = TwitchIntegration.getStreamInfo();
        MessageSender.sendFeedback(source, Text.literal(info).formatted(Formatting.AQUA));
        return 1;
    }

    // ---------- Interactive Icon List ----------
    private static int listIcons(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings settings = SettingsStore.get(player.getUuid());

        MutableText message = Text.literal("Available Icons:\n")
                .styled(style -> style.withBold(true).withColor(Formatting.GOLD));

        String[] icons = {"none", "youtube", "twitch", "kick", "heart", "crown"};

        for (String id : icons) {
            String glyph = IconGlyphs.resolve(id);
            String displayName = formatLabel(id);

            MutableText iconPart = Text.literal(glyph)
                    .styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withFont(IconGlyphs.resolveFont(settings, true)));

            MutableText spacePart = Text.literal(" ")
                    .styled(style -> style.withFont(DEFAULT_FONT));

            Formatting nameColor = getIconColor(id);

            MutableText namePart = Text.literal(displayName)
                    .styled(style -> style
                            .withFont(DEFAULT_FONT)
                            .withColor(nameColor)
                            .withBold(true)
                            .withClickEvent(new ClickEvent.RunCommand("/su icon set " + id))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Text.literal("Set icon to " + displayName))));

            MutableText line = Text.literal("• ")
                    .append(iconPart)
                    .append(spacePart)
                    .append(namePart);

            message.append(line).append(Text.literal("\n"));
        }

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int listColors(ServerCommandSource source) {
        MutableText message = Text.literal("Colors:\n")
                .styled(style -> style.withBold(true).withColor(Formatting.GOLD));

        for (Map.Entry<String, Integer> entry : OFFICIAL_COLORS.entrySet()) {
            String name = entry.getKey();
            int rgb = entry.getValue();
            String displayName = formatLabel(name);

            MutableText line = Text.literal("• ")
                    .append(Text.literal("■ ")
                            .styled(style -> style.withColor(TextColor.fromRgb(rgb))))
                    .append(Text.literal(displayName)
                            .styled(style -> style
                                    .withColor(TextColor.fromRgb(rgb))
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/su color " + name))
                                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to set color to " + displayName)))));

            message.append(line).append(Text.literal("\n"));
        }

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static Formatting getIconColor(String id) {
        return switch (id) {
            case "youtube" -> Formatting.RED;
            case "kick" -> Formatting.GREEN;
            case "twitch" -> Formatting.LIGHT_PURPLE;
            case "crown" -> Formatting.GOLD;
            case "heart" -> Formatting.LIGHT_PURPLE;
            default -> Formatting.GRAY;
        };
    }

    private static String formatLabel(String raw) {
        String[] parts = raw.split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i > 0) out.append(' ');
            out.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) out.append(parts[i].substring(1));
        }
        return out.toString();
    }

    // ---------- Icon Setter ----------
    private static final SimpleCommandExceptionType DEV_ONLY_ICON_EXCEPTION =
            new SimpleCommandExceptionType(Text.literal("This icon is reserved for the developer."));

    private static int setIcon(ServerCommandSource source, String iconId) throws CommandSyntaxException {
        // First validate the icon exists
        if (!IconGlyphs.isValid(iconId)) {
            throw INVALID_ICON.create();
        }

        var player = source.getPlayerOrThrow();

        // Developer‑only icons may only be set by Codex_bat
        if (IconGlyphs.isDeveloperOnly(iconId) && !player.getName().getString().equals("Codex_bat")) {
            throw DEV_ONLY_ICON_EXCEPTION.create();
        }

        // Optional: additional disallowed icons
        if (DISALLOWED_ICONS.contains(iconId)) {
            throw INVALID_ICON.create();
        }

        PlayerSettings current = SettingsStore.get(player.getUuid());

        if (current.iconId().equals(iconId)) {
            MutableText alreadyMsg = Text.literal("Icon is already set to ")
                    .formatted(MessageStyles.DEFAULT_COLOR)
                    .append(styledIconText(source, iconId, true));
            MessageSender.sendFeedback(source, alreadyMsg);
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withIcon(iconId));
        NameplateManager.apply(source.getServer(), player);

        MutableText successMsg = Text.literal("Set icon to ")
                .formatted(MessageStyles.DEFAULT_COLOR)
                .append(styledIconText(source, iconId, true));
        MessageSender.sendFeedback(source, successMsg);
        return 1;
    }

    private static int chatIconsMenu(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings settings = SettingsStore.get(player.getUuid());

        boolean enabled = settings.separateChatIconFont();

        MutableText message = Text.literal("Chat Icon Font Mode\n")
                .styled(s -> s.withColor(Formatting.GOLD).withBold(true));

        message.append(Text.literal("Choose how icons render in chat:\n\n")
                .styled(s -> s.withColor(Formatting.GRAY)));

        // ON button
        MutableText on = Text.literal("[ Separate Chat Font ]")
                .styled(s -> s
                        .withColor(enabled ? Formatting.GREEN : Formatting.DARK_GRAY)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/su icon chaticons true"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Use dedicated chat icon font")
                        )));

        // OFF button
        MutableText off = Text.literal("[ Use Tab Font ]")
                .styled(s -> s
                        .withColor(!enabled ? Formatting.RED : Formatting.DARK_GRAY)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/su icon chaticons false"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Use same icons as tab list")
                        )));

        message.append(on)
                .append(Text.literal(" "))
                .append(off);

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int setChatIcons(ServerCommandSource source, boolean value) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();

        PlayerSettings current = SettingsStore.get(player.getUuid());

        if (current.separateChatIconFont() == value) {
            MutableText alreadyMsg = Text.literal("Chat icon font is already ")
                    .formatted(MessageStyles.DEFAULT_COLOR)
                    .append(Text.literal(value ? "SEPARATE" : "SHARED")
                            .styled(s -> s.withColor(value ? Formatting.GREEN : Formatting.RED)));

            MessageSender.sendFeedback(source, alreadyMsg);
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withSeparateChatIconFont(value));

        MutableText feedback = Text.literal("Chat icon font: ")
                .formatted(MessageStyles.DEFAULT_COLOR)
                .append(Text.literal(value ? "SEPARATE" : "SHARED")
                        .styled(s -> s.withColor(value ? Formatting.GREEN : Formatting.RED)));

        MessageSender.sendFeedback(source, feedback);
        return 1;
    }

    // ---------- Color Setter ----------
    private static int setColor(ServerCommandSource source, String color) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        int rgb = parseRgb(color);
        PlayerSettings current = SettingsStore.get(player.getUuid());

        if (current.colorRgb() == rgb) {
            MutableText alreadyMsg = Text.literal("Color is already set to ")
                    .formatted(MessageStyles.DEFAULT_COLOR)
                    .append(styledColorText(rgb));
            MessageSender.sendFeedback(source, alreadyMsg);
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withColor(rgb));
        NameplateManager.apply(source.getServer(), player);

        MutableText successMsg = Text.literal("Color successfully changed to ")
                .formatted(MessageStyles.DEFAULT_COLOR)
                .append(styledColorText(rgb))
                .append(Text.literal(String.format(" (#%06X)", rgb)).formatted(MessageStyles.DEFAULT_COLOR));
        MessageSender.sendFeedback(source, successMsg);
        return 1;
    }

    private static String getDisplayNameForRgb(int rgb) {
        String official = REVERSE_OFFICIAL.get(rgb);
        if (official != null) return official;
        return String.format("#%06X", rgb);
    }

    // ---------- Streamer live ----------
    private static int setupTwitch(ServerCommandSource source, String channel, String botUsername, String oauth, String clientId) throws CommandSyntaxException {
        // Validate each field
        String error = TwitchValidator.getValidationError("channel", channel);
        if (error != null) throw new SimpleCommandExceptionType(Text.literal(error)).create();
        error = TwitchValidator.getValidationError("bot_username", botUsername);
        if (error != null) throw new SimpleCommandExceptionType(Text.literal(error)).create();
        error = TwitchValidator.getValidationError("oauth", oauth);
        if (error != null) throw new SimpleCommandExceptionType(Text.literal(error)).create();
        error = TwitchValidator.getValidationError("client_id", clientId);
        if (error != null) throw new SimpleCommandExceptionType(Text.literal(error)).create();

        var player = source.getPlayerOrThrow();
        UUID uuid = player.getUuid();

        // Load current effective config (personal if exists, else global fallback)
        TwitchConfig currentCfg = TwitchConfig.getEffectiveConfig(uuid);
        boolean hasPersonal = TwitchConfig.hasPersonalConfig(uuid);

        // Track what actually changed
        List<String> changedFields = new ArrayList<>();
        boolean channelChanged = !channel.equals(currentCfg.twitchChannelName);
        boolean botChanged = !botUsername.equals(currentCfg.botUsername);
        boolean oauthChanged = !oauth.equals(currentCfg.oauthToken);
        boolean clientIdChanged = !clientId.equals(currentCfg.clientId);

        if (channelChanged) changedFields.add("channel");
        if (botChanged) changedFields.add("bot username");
        if (oauthChanged) changedFields.add("OAuth token");
        if (clientIdChanged) changedFields.add("client ID");

        // If nothing changed, inform and exit
        if (changedFields.isEmpty() && hasPersonal) {
            MessageSender.sendFeedback(source, Text.literal("Your Twitch configuration is already up to date.")
                    .formatted(Formatting.YELLOW));
            return 1;
        }

        // Create or update personal config
        TwitchConfig config = hasPersonal ? TwitchConfig.loadForPlayer(uuid) : new TwitchConfig();
        config.twitchChannelName = channel;
        config.botUsername = botUsername;
        config.oauthToken = oauth;
        config.clientId = clientId;
        config.enabled = true;
        config.saveForPlayer(uuid);

        // Reload Twitch client if any credential changed (skip if only enabled flag changed)
        if (channelChanged || botChanged || oauthChanged || clientIdChanged) {
            TwitchIntegration.reloadForPlayer(uuid, config);
        }

        // Auto-assign twitch icon if still default
        PlayerSettings settings = SettingsStore.get(uuid);
        if ("none".equals(settings.iconId())) {
            SettingsStore.set(uuid, settings.withIcon("twitch").withTwitchSetup(true));
            NameplateManager.apply(source.getServer(), player);
        } else {
            SettingsStore.set(uuid, settings.withTwitchSetup(true));
        }

        // Build feedback message
        MutableText msg = Text.literal("✅ Twitch configuration updated!\n")
                .formatted(Formatting.GREEN);

        // All this part was hard ngl
        if (!changedFields.isEmpty()) {
            msg.append(Text.literal("Changed: ").formatted(Formatting.GRAY))
                    .append(Text.literal(String.join(", ", changedFields)).formatted(Formatting.WHITE))
                    .append("\n");
        }

        msg.append(Text.literal("⚠️ IMPORTANT: Your OAuth token is stored in plain text on this server.\n")
                        .formatted(Formatting.YELLOW))
                .append(Text.literal("Only use this command on a server you trust completely.\n")
                        .formatted(Formatting.GRAY))
                .append(Text.literal("To edit individual fields, use ")
                        .formatted(Formatting.GRAY))
                .append(Text.literal("/su twitch set <field> <value>")
                        .styled(s -> s.withColor(Formatting.AQUA)
                                .withClickEvent(new ClickEvent.SuggestCommand("/su twitch set "))))
                .append("\n")
                .append(Text.literal("To remove your config: "))
                .append(Text.literal("/su twitch reset")
                        .styled(s -> s.withColor(Formatting.AQUA)
                                .withClickEvent(new ClickEvent.SuggestCommand("/su twitch reset"))));

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static int resetTwitch(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        UUID uuid = player.getUuid();
        TwitchConfig.deleteForPlayer(uuid);
        TwitchIntegration.unloadForPlayer(uuid);
        MessageSender.sendFeedback(source, Text.literal("Your personal Twitch config has been removed. The server's global config will now be used (if any).")
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int showTwitchStatus(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        UUID uuid = player.getUuid();
        boolean hasPersonal = TwitchConfig.hasPersonalConfig(uuid);
        TwitchConfig cfg = TwitchConfig.getEffectiveConfig(uuid);

        MutableText msg = Text.literal("Your Twitch Configuration\n").formatted(Formatting.AQUA, Formatting.BOLD);
        msg.append(Text.literal("Source: ").formatted(Formatting.GRAY))
                .append(Text.literal(hasPersonal ? "Personal" : "Global Fallback")
                        .formatted(hasPersonal ? Formatting.GREEN : Formatting.YELLOW))
                .append("\n\n");

        // Helper to create editable line
        BiFunction<String, String, MutableText> editableLine = (label, field) -> {
            String value = switch (field) {
                case "channel" -> cfg.twitchChannelName;
                case "bot_username" -> cfg.botUsername;
                case "oauth" -> cfg.oauthToken.isEmpty() ? "(not set)" : "[HIDDEN]";
                case "client_id" -> cfg.clientId;
                default -> "";
            };
            MutableText line = Text.literal("• " + label + ": ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(value).formatted(Formatting.WHITE))
                    .append(Text.literal(" [Edit]")
                            .styled(s -> s.withColor(Formatting.AQUA)
                                    .withUnderline(true)
                                    .withClickEvent(new ClickEvent.SuggestCommand("/su twitch set " + field + " "))
                                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to change " + label)))));
            return line;
        };

        msg.append(editableLine.apply("Channel", "channel")).append("\n");
        msg.append(editableLine.apply("Bot Username", "bot_username")).append("\n");
        msg.append(editableLine.apply("OAuth Token", "oauth")).append("\n");
        msg.append(editableLine.apply("Client ID", "client_id")).append("\n");

        msg.append(Text.literal("• Enabled: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(cfg.enabled)).formatted(cfg.enabled ? Formatting.GREEN : Formatting.RED))
                .append("\n\n");

        // Quick actions
        msg.append(Text.literal("[Full Setup]")
                        .styled(s -> s.withColor(Formatting.GOLD)
                                .withClickEvent(new ClickEvent.SuggestCommand("/su twitch setup "))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Run full setup wizard")))))
                .append(Text.literal("  "))
                .append(Text.literal("[Reset]")
                        .styled(s -> s.withColor(Formatting.RED)
                                .withClickEvent(new ClickEvent.RunCommand("/su twitch reset"))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Remove your personal config")))));

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    // Helper to get or create personal config for the player
    private static TwitchConfig getOrCreatePersonalConfig(UUID uuid) {
        TwitchConfig cfg = TwitchConfig.loadForPlayer(uuid);
        if (!cfg.enabled && cfg.oauthToken.isEmpty()) {
            // Initialize with empty enabled config
            cfg = new TwitchConfig();
            cfg.enabled = false;
        }
        return cfg;
    }

    private static int setTwitchField(ServerCommandSource source, String field, String value) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        UUID uuid = player.getUuid();

        // Validate
        String error = TwitchValidator.getValidationError(field, value);
        if (error != null) {
            throw new SimpleCommandExceptionType(Text.literal(error)).create();
        }

        TwitchConfig cfg = getOrCreatePersonalConfig(uuid);
        String oldValue = switch (field) {
            case "channel" -> cfg.twitchChannelName;
            case "bot_username" -> cfg.botUsername;
            case "oauth" -> cfg.oauthToken;
            case "client_id" -> cfg.clientId;
            default -> "";
        };

        // Check if value actually changed
        if (value.equals(oldValue)) {
            MessageSender.sendFeedback(source, Text.literal("ℹ️ " + formatFieldName(field) + " is already set to that value.")
                    .formatted(Formatting.YELLOW));
            return 1;
        }

        // Update field
        switch (field) {
            case "channel" -> cfg.twitchChannelName = value;
            case "bot_username" -> cfg.botUsername = value;
            case "oauth" -> cfg.oauthToken = value;
            case "client_id" -> cfg.clientId = value;
        }
        cfg.saveForPlayer(uuid);

        // If all required fields are now filled, auto-enable
        if (!cfg.enabled &&
                !cfg.twitchChannelName.isEmpty() &&
                !cfg.botUsername.isEmpty() &&
                !cfg.oauthToken.isEmpty() &&
                !cfg.clientId.isEmpty()) {
            cfg.enabled = true;
            cfg.saveForPlayer(uuid);
        }

        // Reload client if enabled and a credential field changed
        boolean credentialChanged = field.equals("channel") || field.equals("bot_username") ||
                field.equals("oauth") || field.equals("client_id");
        if (cfg.enabled && credentialChanged) {
            TwitchIntegration.reloadForPlayer(uuid, cfg);
        }

        // Auto-assign twitch icon if not set and now twitch is configured
        PlayerSettings settings = SettingsStore.get(uuid);
        if ("none".equals(settings.iconId()) && cfg.enabled) {
            SettingsStore.set(uuid, settings.withIcon("twitch").withTwitchSetup(true));
            NameplateManager.apply(source.getServer(), player);
        } else {
            SettingsStore.set(uuid, settings.withTwitchSetup(cfg.enabled));
        }

        MutableText msg = Text.literal("✅ ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(formatFieldName(field) + " set to: "))
                .append(Text.literal(field.equals("oauth") ? "[HIDDEN]" : value).formatted(Formatting.WHITE));
        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static String formatFieldName(String field) {
        return switch (field) {
            case "channel" -> "Channel";
            case "bot_username" -> "Bot Username";
            case "oauth" -> "OAuth Token";
            case "client_id" -> "Client ID";
            default -> field;
        };
    }

    private static int setStreamerLive(ServerCommandSource source, boolean live) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        UUID uuid = player.getUuid();
        PlayerSettings current = SettingsStore.get(uuid);

        if (current.streamerLive() == live) {
            MessageSender.sendFeedback(source, Text.literal("Streamer live is already " + (live ? "ON" : "OFF"))
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(uuid, current.withStreamerLive(live));
        NameplateManager.apply(source.getServer(), player);

        if (live) {
            TwitchConfig cfg = TwitchConfig.getEffectiveConfig(uuid);
            if (cfg.enabled && !cfg.oauthToken.isEmpty()) {
                boolean started = TwitchIntegration.ensureClientForPlayer(uuid, cfg);
                if (started) {
                    MessageSender.sendFeedback(source, Text.literal("✅ Twitch bot connected.")
                            .formatted(Formatting.GREEN));
                } else {
                    MessageSender.sendFeedback(source, Text.literal("⚠️ Could not connect Twitch bot. Check /su twitch status or logs.")
                            .formatted(Formatting.RED));
                }
            } else {
                MessageSender.sendFeedback(source, Text.literal("ℹ️ You don't have Twitch configured yet. Use /su twitch setup to enable alerts.")
                        .formatted(Formatting.YELLOW));
            }
        }

        MessageSender.sendFeedback(source, Text.literal("Streamer live: " + live)
                .formatted(MessageStyles.DEFAULT_COLOR));
        return 1;
    }

    // ---------- Follow alerts ----------
    private static int setFollowAlertMode(ServerCommandSource source, String modeStr) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        FollowAlertMode mode;
        try {
            mode = FollowAlertMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(
                    Text.literal("Invalid mode. Use: none, personal, global, or both")
            ).create();
        }

        PlayerSettings current = SettingsStore.get(player.getUuid());
        if (current.followAlertMode() == mode) {
            MessageSender.sendFeedback(source, Text.literal("Follow alert mode is already " + mode.getDisplayName())
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withFollowAlertMode(mode));
        MessageSender.sendFeedback(source, Text.literal("Follow alert mode set to: " + mode.getDisplayName())
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int showFollowAlertStatus(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        FollowAlertMode mode = SettingsStore.get(player.getUuid()).followAlertMode();
        MutableText msg = Text.literal("Follow Alert Mode: ")
                .formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(mode.getDisplayName()).formatted(Formatting.AQUA))
                .append("\n")
                .append(Text.literal(mode.getDescription()).formatted(Formatting.GRAY));
        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    // ---------- Reset helpers ----------
    private static MutableText undoButton(String command, String hover) {
        return Text.literal("[Undo]")
                .styled(style -> style
                        .withColor(MessageStyles.UNDO_COLOR)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal(hover))));
    }

    private static int resetColor(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());

        int oldRgb = current.colorRgb();
        int defaultRgb = PlayerSettings.defaults().colorRgb();

        if (oldRgb == defaultRgb) {
            MessageSender.sendFeedback(source, Text.literal("Color is already default.")
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withColor(defaultRgb));
        NameplateManager.apply(source.getServer(), player);

        MutableText message = Text.literal("Reset Changes\n")
                .styled(style -> style.withColor(MessageStyles.HEADER_COLOR).withBold(true));

        MutableText line = Text.literal("• Color: ")
                .formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(getDisplayNameForRgb(oldRgb))
                        .styled(style -> style.withColor(TextColor.fromRgb(oldRgb))))
                .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                .append(Text.literal(getDisplayNameForRgb(defaultRgb))
                        .styled(style -> style.withColor(TextColor.fromRgb(defaultRgb))))
                .append(Text.literal("  "))
                .append(Text.literal("[Undo]")
                        .styled(style -> style
                                .withColor(MessageStyles.UNDO_COLOR)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su color " + String.format("%06X", oldRgb)))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Restore previous color")))));

        message.append(line);

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int resetIcon(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());
        String oldIcon = current.iconId();
        String defaultIcon = PlayerSettings.defaults().iconId();

        if (oldIcon.equals(defaultIcon)) {
            MessageSender.sendFeedback(source, Text.literal("Icon is already default.")
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withIcon(defaultIcon));
        NameplateManager.apply(source.getServer(), player);

        MutableText message = Text.literal("Reset Changes\n")
                .styled(style -> style.withColor(MessageStyles.HEADER_COLOR).withBold(true));

        MutableText line = Text.literal("• Icon: ")
                .formatted(MessageStyles.LABEL_COLOR)
                .append(styledIconText(source, oldIcon, true))
                .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                .append(styledIconText(source, defaultIcon, true))
                .append(Text.literal("  "))
                .append(Text.literal("[Undo]")
                        .styled(style -> style
                                .withColor(MessageStyles.UNDO_COLOR)
                                .withUnderline(true)
                                .withClickEvent(new ClickEvent.RunCommand("/su icon set " + oldIcon))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Restore previous icon")))));

        message.append(line);

        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int resetStream(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());

        boolean old = current.streamerLive();
        boolean def = PlayerSettings.defaults().streamerLive();

        if (old == def) {
            MessageSender.sendFeedback(source, Text.literal("Stream status already default.")
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withStreamerLive(def));
        NameplateManager.apply(source.getServer(), player);

        MutableText msg = Text.literal("Reset Changes\n")
                .formatted(MessageStyles.HEADER_COLOR, Formatting.BOLD);

        msg.append(
                Text.literal("• Stream: ").formatted(MessageStyles.LABEL_COLOR)
                        .append(Text.literal(old ? "LIVE" : "OFF").formatted(old ? Formatting.RED : Formatting.GRAY))
                        .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                        .append(Text.literal(def ? "LIVE" : "OFF").formatted(def ? Formatting.RED : Formatting.GRAY))
                        .append(Text.literal("  "))
                        .append(undoButton("/su stream live " + (old ? "on" : "off"), "Restore previous stream state"))
        );

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static int resetFollow(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());

        FollowAlertMode old = current.followAlertMode();
        FollowAlertMode def = PlayerSettings.defaults().followAlertMode();

        if (old == def) {
            MessageSender.sendFeedback(source, Text.literal("Follow alert mode is already default (" + def.getDisplayName() + ")")
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), current.withFollowAlertMode(def));
        NameplateManager.apply(source.getServer(), player);

        MutableText msg = Text.literal("Reset Changes\n")
                .formatted(MessageStyles.HEADER_COLOR, Formatting.BOLD);

        msg.append(Text.literal("• Follow Alerts: ").formatted(MessageStyles.LABEL_COLOR)
                .append(Text.literal(old.getDisplayName()).formatted(Formatting.GRAY))
                .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                .append(Text.literal(def.getDisplayName()).formatted(Formatting.GREEN))
                .append(Text.literal("  "))
                .append(undoButton("/su soundalert " + old.name().toLowerCase(), "Restore previous mode")));

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static int resetAll(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();

        PlayerSettings current = SettingsStore.get(player.getUuid());
        PlayerSettings def = PlayerSettings.defaults();

        if (current.equals(def)) {
            MessageSender.sendFeedback(source, Text.literal("All settings already default.")
                    .formatted(MessageStyles.DEFAULT_COLOR));
            return 1;
        }

        SettingsStore.set(player.getUuid(), def);
        NameplateManager.apply(source.getServer(), player);

        MutableText msg = Text.literal("Reset All Settings")
                .formatted(MessageStyles.HEADER_COLOR, Formatting.BOLD);

        boolean first = true;

        // --- ICON ---
        if (!current.iconId().equals(def.iconId())) {
            if (first) {
                msg.append(Text.literal("\n"));
                first = false;
            }
            msg.append(Text.literal("\n• Icon: ").formatted(MessageStyles.LABEL_COLOR)
                    .append(styledIconText(source, current.iconId(), true))
                    .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                    .append(styledIconText(source, def.iconId(), true))
                    .append(Text.literal("  "))
                    .append(undoButton("/su icon set " + current.iconId(), "Restore previous icon")));
        }

        // --- COLOR ---
        if (current.colorRgb() != def.colorRgb()) {
            if (first) {
                msg.append(Text.literal("\n"));
                first = false;
            } else {
                msg.append(Text.literal("\n"));
            }
            msg.append(Text.literal("• Color: ").formatted(MessageStyles.LABEL_COLOR)
                    .append(Text.literal(getDisplayNameForRgb(current.colorRgb()))
                            .styled(s -> s.withColor(TextColor.fromRgb(current.colorRgb()))))
                    .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                    .append(Text.literal(getDisplayNameForRgb(def.colorRgb()))
                            .styled(s -> s.withColor(TextColor.fromRgb(def.colorRgb()))))
                    .append(Text.literal("  "))
                    .append(undoButton("/su color " + String.format("%06X", current.colorRgb()), "Restore previous color")));
        }

        // --- STREAM ---
        if (current.streamerLive() != def.streamerLive()) {
            if (first) {
                msg.append(Text.literal("\n"));
                first = false;
            } else {
                msg.append(Text.literal("\n"));
            }
            msg.append(Text.literal("• Stream: ").formatted(MessageStyles.LABEL_COLOR)
                    .append(Text.literal(current.streamerLive() ? "LIVE" : "OFF")
                            .formatted(current.streamerLive() ? Formatting.RED : Formatting.GRAY))
                    .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                    .append(Text.literal(def.streamerLive() ? "LIVE" : "OFF")
                            .formatted(def.streamerLive() ? Formatting.RED : Formatting.GRAY))
                    .append(Text.literal("  "))
                    .append(undoButton("/su stream live " + (current.streamerLive() ? "on" : "off"), "Restore stream state")));
        }

        // --- SOUND ALERT ---
        if (current.followAlertMode() != def.followAlertMode()) {
            msg.append(Text.literal("\n• Sound Alert: ").formatted(MessageStyles.LABEL_COLOR)
                    .append(Text.literal(current.followAlertMode().getDisplayName())
                            .formatted(Formatting.GRAY))
                    .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                    .append(Text.literal(def.followAlertMode().getDisplayName())
                            .formatted(Formatting.GREEN))
                    .append(Text.literal("  "))
                    .append(undoButton("/su soundalert " + current.followAlertMode().name().toLowerCase(), "Restore previous mode")));
        }

        // --- TINY TAKEOVER :3 ---
        if (current.separateChatIconFont() != def.separateChatIconFont()) {
            if (first) {
                msg.append(Text.literal("\n"));
                first = false;
            } else {
                msg.append(Text.literal("\n"));
            }

            msg.append(Text.literal("• Icon Font Mode: ")
                    .formatted(MessageStyles.LABEL_COLOR)
                    .append(Text.literal(current.separateChatIconFont() ? "SEPARATE" : "SHARED")
                            .formatted(current.separateChatIconFont() ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal(" → ").formatted(MessageStyles.ARROW_COLOR))
                    .append(Text.literal(def.separateChatIconFont() ? "SEPARATE" : "SHARED")
                            .formatted(def.separateChatIconFont() ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("  "))
                    .append(undoButton(
                            "/su icon chaticons_internal " + current.separateChatIconFont(),
                            "Restore previous icon font mode"
                    )));
        }

        MessageSender.sendFeedback(source, msg);
        return 1;
    }

    private static MutableText styledIconText(ServerCommandSource source, String iconId, boolean isChat) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings settings = SettingsStore.get(player.getUuid());
        return styledIconText(settings, iconId, isChat);
    }

    // Builds a rich text component for an icon (glyph + formatted name)
    private static MutableText styledIconText(PlayerSettings settings, String iconId, boolean isChat) {
        String glyph = IconGlyphs.resolve(iconId);
        String displayName = formatLabel(iconId);
        Formatting nameColor = getIconColor(iconId);

        StyleSpriteSource font = IconGlyphs.resolveFont(settings, isChat);

        MutableText iconPart = Text.literal(glyph)
                .styled(style -> style
                        .withColor(Formatting.WHITE)
                        .withFont(font));

        MutableText namePart = Text.literal(displayName)
                .styled(style -> style
                        .withFont(IconGlyphs.DEFAULT_FONT)
                        .withColor(nameColor)
                        .withBold(true));

        MutableText spacing = Text.literal(" ").styled(style -> style.withFont(DEFAULT_FONT));

        return iconPart.append(spacing).append(namePart);
    }

    // Builds a rich text component for a color (colored square + colored name)
    private static MutableText styledColorText(int rgb) {
        String displayName = getDisplayNameForRgb(rgb);
        MutableText square = Text.literal("■ ")
                .styled(style -> style.withColor(TextColor.fromRgb(rgb)));
        MutableText name = Text.literal(displayName)
                .styled(style -> style.withColor(TextColor.fromRgb(rgb)).withBold(true));
        return square.append(name);
    }

    // ---------- RGB parser (supports aliases + hex) ----------
    private static int parseRgb(String input) throws CommandSyntaxException {
        String normalized = input.toLowerCase();

        if (ALIASES.containsKey(normalized)) {
            return ALIASES.get(normalized);
        }

        if (OFFICIAL_COLORS.containsKey(normalized)) {
            return OFFICIAL_COLORS.get(normalized);
        }

        String hex = normalized.startsWith("#") ? normalized.substring(1) : normalized;
        if (hex.length() != 6) throw INVALID_COLOR.create();
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw INVALID_COLOR.create();
        }
    }

    private static int togglePrefix(ServerCommandSource source) throws CommandSyntaxException {
        var player = source.getPlayerOrThrow();
        PlayerSettings current = SettingsStore.get(player.getUuid());
        boolean newValue = !current.shortPrefix();
        SettingsStore.set(player.getUuid(), current.withShortPrefix(newValue));

        String mode = newValue ? "short (SU)" : "full (StreamerUtils)";
        Text message = Text.literal("Prefix mode changed to ")
                .append(Text.literal(mode).formatted(MessageStyles.PREFIX_TEXT_COLOR));
        MessageSender.sendFeedback(source, message);
        return 1;
    }

    private static int setBracket(ServerCommandSource source, String left, String right) throws CommandSyntaxException {
        if (right == null) {
            // symmetric: use left for both sides
            GlobalSettings.setBrackets(left, left);
        } else {
            GlobalSettings.setBrackets(left, right);
        }
        NameplateManager.refreshAll(source.getServer());
        MessageSender.sendFeedback(source, Text.literal("Brackets set to: " + left + " … " + right)
                .formatted(MessageStyles.DEFAULT_COLOR));
        return 1;
    }

    private static int resetBracket(ServerCommandSource source) {
        GlobalSettings.resetBrackets();
        NameplateManager.refreshAll(source.getServer());
        MessageSender.sendFeedback(source, Text.literal("Brackets reset to default: < >")
                .formatted(MessageStyles.DEFAULT_COLOR));
        return 1;
    }
}