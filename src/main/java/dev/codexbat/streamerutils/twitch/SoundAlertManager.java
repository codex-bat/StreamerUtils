// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import dev.codexbat.streamerutils.FollowAlertMode;
import dev.codexbat.streamerutils.SettingsStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.UUID;

public class SoundAlertManager {
    public static void playFollowSound(MinecraftServer server, String followerName, UUID broadcasterUuid) {
        server.execute(() -> {
            // Get broadcaster's name for the "both" message
            String broadcasterName = null;
            if (broadcasterUuid != null) {
                ServerPlayerEntity broadcaster = server.getPlayerManager().getPlayer(broadcasterUuid);
                if (broadcaster != null) {
                    broadcasterName = broadcaster.getName().getString();
                }
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                FollowAlertMode mode = SettingsStore.get(player.getUuid()).followAlertMode();
                if (mode == FollowAlertMode.NONE) continue;

                boolean isOwnChannel = (broadcasterUuid != null && broadcasterUuid.equals(player.getUuid()));
                boolean shouldAlert = switch (mode) {
                    case PERSONAL -> isOwnChannel;
                    case GLOBAL -> !isOwnChannel;
                    case BOTH -> true;
                    default -> false;
                };

                if (!shouldAlert) continue;

                // Play sound
                World world = player.getEntityWorld();
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                        SoundCategory.MASTER, 2.0f, 1.2f);

                // Build message
                MutableText message;
                if (mode == FollowAlertMode.BOTH && !isOwnChannel && broadcasterName != null) {
                    message = Text.literal(followerName + " followed " + broadcasterName + "!")
                            .formatted(Formatting.YELLOW);
                } else {
                    message = Text.literal(followerName + " followed!")
                            .formatted(Formatting.YELLOW);
                }

                player.sendMessage(message, true);
            }
        });
    }
}