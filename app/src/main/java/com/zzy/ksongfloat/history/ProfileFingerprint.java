package com.zzy.ksongfloat.history;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ProfileFingerprint {
    public static String create(String nickname, String bio, List<String> songs, String publicId) {
        StringBuilder sb = new StringBuilder();
        sb.append(norm(nickname)).append('|').append(norm(bio)).append('|').append(norm(publicId)).append('|');
        if (songs != null) {
            for (String s : songs) sb.append(norm(s)).append(';');
        }
        return sha256(sb.toString());
    }

    public static String create(String nickname, String bio, String songsText, String publicId) {
        return sha256(norm(nickname) + "|" + norm(bio) + "|" + norm(songsText) + "|" + norm(publicId));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte x : b) out.append(String.format("%02x", x));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
