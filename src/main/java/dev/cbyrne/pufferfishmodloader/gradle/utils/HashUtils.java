package dev.cbyrne.pufferfishmodloader.gradle.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private static final int BUFFER_SIZE = 8192;

    public static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream stream = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    int read = stream.read(buffer, 0, buffer.length);
                    if (read < 0) {
                        break;
                    }
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (byte b : bytes) {
            hexStr.append(String.format("%02x", 0xFF & b));
        }
        return hexStr.toString();
    }
}
