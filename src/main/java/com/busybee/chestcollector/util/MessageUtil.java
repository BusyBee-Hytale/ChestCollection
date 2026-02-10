package com.busybee.chestcollector.util;

import ai.kodari.hylib.config.YamlConfig;

public class MessageUtil {

    private static YamlConfig messages;

    public static void init() {
        messages = new YamlConfig("messages.yml");
    }

    public static String get(String path) {
        return messages.getString(path, path);
    }

    public static String get(String path, String defaultValue) {
        return messages.getString(path, defaultValue);
    }

    public static String format(String path, Object... replacements) {
        String message = get(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String key = replacements[i].toString();
                String value = replacements[i + 1].toString();
                message = message.replace(key, value);
            }
        }
        return message;
    }
}
