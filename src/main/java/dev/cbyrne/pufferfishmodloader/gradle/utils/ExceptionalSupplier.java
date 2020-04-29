package dev.cbyrne.pufferfishmodloader.gradle.utils;

public interface ExceptionalSupplier<T, E extends Throwable> {
    T get() throws E;
}
