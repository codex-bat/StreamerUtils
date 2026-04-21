// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NameplateManager {
    private NameplateManager() {}

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            apply(server, handler.player);
        });
    }

    public static void apply(MinecraftServer server, ServerPlayerEntity player) {
        PlayerSettings settings = SettingsStore.get(player.getUuid());
        Scoreboard scoreboard = server.getScoreboard();
        Team team = getOrCreateTeam(scoreboard, player);

        Text iconPrefix = IconGlyphs.styledIcon(settings, false);

        team.setPrefix((iconPrefix));
        team.setColor(rgbToFormatting(settings.colorRgb()));
        team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        team.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS);

        scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
        scoreboard.updateScoreboardTeamAndPlayers(team);
    }

    public static void refreshAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            apply(server, player);
        }
    }

    private static Team getOrCreateTeam(Scoreboard scoreboard, ServerPlayerEntity player) {
        String teamName = teamName(player);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
        }
        return team;
    }

    private static String teamName(ServerPlayerEntity player) {
        String uuid = player.getUuidAsString().replace("-", "");
        return "su_" + uuid.substring(0, Math.min(12, uuid.length()));
    }

    private static Formatting rgbToFormatting(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        Formatting best = Formatting.WHITE;
        double bestDistance = Double.MAX_VALUE;

        for (Formatting f : Formatting.values()) {
            if (!f.isColor()) continue;

            Integer color = f.getColorValue();
            if (color == null) continue;

            int fr = (color >> 16) & 0xFF;
            int fg = (color >> 8) & 0xFF;
            int fb = color & 0xFF;

            double dist = Math.pow(r - fr, 2) + Math.pow(g - fg, 2) + Math.pow(b - fb, 2);

            if (dist < bestDistance) {
                bestDistance = dist;
                best = f;
            }
        }

        return best;
    }
}