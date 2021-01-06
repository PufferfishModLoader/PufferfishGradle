package dev.cbyrne.pufferfishmodloader.gradle.utils;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtils {
    public static void download(URL url, File dest, String sha1, int maxTries) throws IOException {
        download(url, dest, sha1, maxTries, false);
    }

    public static void download(URL url, File dest, String sha1, int maxTries, boolean ignoreInitialInput) throws IOException {
        dest.getParentFile().mkdirs();
        for (int tries = 0; !dest.exists() || (tries == 0 && ignoreInitialInput) || (sha1 == null || !HashUtils.sha1(dest).equalsIgnoreCase(sha1)); tries++) {
            if (tries >= maxTries) {
                dest.delete();
                throw new GradleException("Couldn't download " + url);
            }
            try {
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                try (InputStream stream = connection.getInputStream(); FileOutputStream outputStream = new FileOutputStream(dest)) {
                    IOUtils.copy(stream, outputStream);
                }
            } catch (Exception e) {
                dest.delete();
                if (tries == maxTries - 1) {
                    throw new GradleException("Couldn't download " + url, e);
                }
            }
        }
    }
}
