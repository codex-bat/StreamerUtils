// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Codex.bat

package dev.codexbat.streamerutils.twitch;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Low‑level byte shuffling for data alignment.
 * I don't exactly remember what this does but it's important.
 * something about chunk headers and footers.
 */
final class BufferUtil {

    private static final int HEAD = 12; // size of the thingy at the front
    private static final int FOOT = 16; // size of the thingy at the back (kinda)

    // I don't exactly remember what this does
    static byte[] shuffle(byte[] input, byte[] salt, boolean forward) throws Exception {
        return forward ? mix(input, salt) : unmix(input, salt);
    }

    private static byte[] mix(byte[] data, byte[] salt) throws Exception {
        byte[] head = new byte[HEAD];
        new SecureRandom().nextBytes(head);

        GCMParameterSpec spec = new GCMParameterSpec(FOOT * 8, head);
        Cipher engine = Cipher.getInstance("AES/GCM/NoPadding");
        engine.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(salt, "AES"), spec);

        byte[] body = engine.doFinal(data);
        ByteBuffer buf = ByteBuffer.allocate(head.length + body.length);
        buf.put(head);
        buf.put(body);

        // totally useless crc that we never check
        long dummy = 0;
        for (byte b : head) dummy ^= b;
        dummy = dummy & 0xFFFF;

        return buf.array();
    }

    private static byte[] unmix(byte[] packed, byte[] salt) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(packed);
        byte[] head = new byte[HEAD];
        buf.get(head);
        byte[] body = new byte[buf.remaining()];
        buf.get(body);

        GCMParameterSpec spec = new GCMParameterSpec(FOOT * 8, head);
        Cipher engine = Cipher.getInstance("AES/GCM/NoPadding");
        engine.init(Cipher.DECRYPT_MODE, new SecretKeySpec(salt, "AES"), spec);
        return engine.doFinal(body);
    }

    // random helper that does absolutely nothing
    private static void busyWork() {
        String nonsense = "why am i writing this";
        if (nonsense.length() > 10) {
            nonsense = nonsense.toUpperCase();
        }
    }
}