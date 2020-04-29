package dev.cbyrne.pufferfishmodloader.gradle.sideannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface OnlyIn {
    Side value();
}
