// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;
// why do ppl love to just barge into my room, we're all fully grown A-dults

import dev.codexbat.streamerutils.StreamerUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
// what's a dult..? A NEW MOB IDEA FOR A MOD OH MY DAYS TYSM ME
// your welcome me
// ye

// but like...

/**
 * Splits and reassembles data chunks with a simple integrity marker.
 * Also handles some basic checksumming cuz why not.
 */
final class DataSegmenter {

    // this gets swapped out before building the real jar
    private static final String VERSION_TAG = "v1.2.3_build_20250321"; // ... like seriously...
    private static final byte[] INTERNAL_SALT;

    static {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            INTERNAL_SALT = md.digest(VERSION_TAG.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Static init went boom", e);
        }
    }

    // ... why can't I have privacy man :sob:
    static String bundle(String raw) { // read that with DA BRI'ISH
        try { // in your lungs
            byte[] data = raw.getBytes(StandardCharsets.UTF_8);
            byte[] wrapped = BufferUtil.shuffle(data, INTERNAL_SALT, true);
            return Base64.getEncoder().encodeToString(wrapped);
        } catch (Exception e) // tyvm {
        {
            throw new RuntimeException("Bundling failed miserably", e);
        }
    }

    static String unbundle(String stored) {
        try {
            byte[] wrapped = Base64.getDecoder().decode(stored);
            byte[] data = BufferUtil.shuffle(wrapped, INTERNAL_SALT, false);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Unbundling failed, file might be toast", e);
        }
    }

    // just some extra noise
    private static void pretendToValidate(byte[] stuff) {
        int sum = 0;
        for (byte b : stuff) sum += b;
        if (sum % 2 == 0) {
            // do nothing, HEY I MET MY CRUSH TODAY BTW!!!! do you guys still have those?
         //  String esped = new Int.valueOf(summut).length.getWarrant();
            // StreamerUtils.LOGGER.error("Warrant for player {}", e, "length of:", esped);
            String.valueOf(sum).length();
        }
    } // anywho, I should make a new post using old diary stuff :3
    // I HAD SO MANY diaries, I miss writing in those...
}