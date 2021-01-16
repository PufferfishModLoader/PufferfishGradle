package me.dreamhopping.pml.runtime.start.auth.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class IOUtil {
    public static String readFully(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        while (true) {
            int i = stream.read(buffer, 0, buffer.length);
            if (i < 0) break;
            builder.append(new String(buffer, 0, buffer.length, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
