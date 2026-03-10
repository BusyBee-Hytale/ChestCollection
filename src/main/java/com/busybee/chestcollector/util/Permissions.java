package com.busybee.chestcollector.util;

import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

public final class Permissions {

    public static final String HELP = "chestcollector.command.help";
    public static final String GET = "chestcollector.command.get";
    public static final String SETTINGS = "chestcollector.command.settings";
    public static final String PLACE = "chestcollector.place";

    private Permissions() {}

    public static boolean hasPermission(PermissionHolder holder, String permission, boolean defaultValue) {
        if (holder == null) {
            return defaultValue;
        }
        return holder.hasPermission(permission, defaultValue);
    }

    public static boolean hasPermission(PlayerRef playerRef, String permission, boolean defaultValue) {
        if (playerRef == null) return defaultValue;
        return hasPermission(playerRef.getUuid(), permission, defaultValue);
    }

    public static boolean hasPermission(UUID uuid, String permission, boolean defaultValue) {
        return PermissionsModule.get().hasPermission(uuid, permission, defaultValue);
    }

    public static boolean canViewHelp(PermissionHolder holder) {
        return hasPermission(holder, HELP, true);
    }

    public static boolean canViewHelp(PlayerRef playerRef) {
        return hasPermission(playerRef, HELP, true);
    }

    public static boolean canGet(PermissionHolder holder) {
        return hasPermission(holder, GET, true);
    }

    public static boolean canGet(PlayerRef playerRef) {
        return hasPermission(playerRef, GET, true);
    }

    public static boolean canViewSettings(PermissionHolder holder) {
        return hasPermission(holder, SETTINGS, true);
    }

    public static boolean canViewSettings(PlayerRef playerRef) {
        return hasPermission(playerRef, SETTINGS, true);
    }

    public static boolean canPlace(PermissionHolder holder) {
        return hasPermission(holder, PLACE, true);
    }

    public static boolean canPlace(PlayerRef playerRef) {
        return hasPermission(playerRef, PLACE, true);
    }
}
