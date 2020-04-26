package dev.cbyrne.pufferfishmodloader.gradle.utils;

import org.gradle.api.GradleException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamingScheme {
    private static final Pattern VALID_MODID_PATTERN = Pattern.compile("^[0-9a-z_]+$");
    private static final Pattern UNDERSCORE_LETTER_MATCHER = Pattern.compile("_([a-z0-9])");

    public static boolean isValidModid(String modid) {
        return VALID_MODID_PATTERN.matcher(modid).matches();
    }

    public static String getCamelCaseModid(String modid) {
        if (!isValidModid(modid)) throw new GradleException("Invalid mod ID " + modid);
        Matcher matcher = UNDERSCORE_LETTER_MATCHER.matcher(modid);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase(Locale.ENGLISH));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
