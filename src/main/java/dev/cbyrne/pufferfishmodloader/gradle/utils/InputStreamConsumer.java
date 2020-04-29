package dev.cbyrne.pufferfishmodloader.gradle.utils;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamConsumer {
    void accept(InputStream stream) throws IOException;
}
