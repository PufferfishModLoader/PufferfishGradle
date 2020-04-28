package dev.cbyrne.pufferfishmodloader.gradle.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    public static void copy(InputStream stream, OutputStream output) throws IOException {
        byte[] buffer = new byte[HashUtils.BUFFER_SIZE];
        while (true) {
            int bytesRead = stream.read(buffer, 0, buffer.length);
            if (bytesRead < 0) {
                break;
            }
            output.write(buffer, 0, bytesRead);
        }
    }
}
