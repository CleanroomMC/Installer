package com.cleanroommc.installer.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class Hashes {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String sha1(Path path) throws IOException {
        return hash(path, "SHA-1");
    }

    public static String sha256(Path path) throws IOException {
        return hash(path, "SHA-256");
    }

    public static String hash(Path path, String algorithm) throws IOException {
        MessageDigest digest = digest(algorithm);
        byte[] buffer = new byte[8192];
        try (InputStream in = Files.newInputStream(path)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    public static boolean corrupt(Path path, String expectedSha1, long expectedSize) throws IOException {
        if (!Files.isRegularFile(path)) {
            return true;
        }
        if (expectedSize > 0 && Files.size(path) != expectedSize) {
            return true;
        }
        if (expectedSha1 == null || expectedSha1.trim().isEmpty()) {
            return false;
        }
        return !expectedSha1.trim().equalsIgnoreCase(sha1(path));
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is required by every Java runtime", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
        }
        return builder.toString();
    }

    private Hashes() { }

}
