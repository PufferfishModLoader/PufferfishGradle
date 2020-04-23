package dev.cbyrne.pufferfishmodloader.gradle.utils;

import java.util.function.Function;
import java.util.function.Supplier;

public class NullUtils {
    public static <O, T> T ifNonNullElse(O object, Function<O, T> nonNull, Supplier<T> ifNull) {
        if (object != null) {
            return nonNull.apply(object);
        } else {
            return ifNull.get();
        }
    }
}
