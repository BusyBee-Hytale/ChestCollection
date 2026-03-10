package com.busybee.chestcollector.util;

import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

public final class Permissions {

    private Permissions() {}

    public static boolean hasPermission(PermissionHolder holder, String permission, boolean defaultValue) {
        if (holder == null) return defaultValue;
        return holder.hasPermission(permission, defaultValue);
    }

    public static boolean hasPermission(PlayerRef playerRef, String permission, boolean defaultValue) {
        if (playerRef == null) return defaultValue;
        return hasPermission(playerRef.getUuid(), permission, defaultValue);
    }

    public static boolean hasPermission(UUID uuid, String permission, boolean defaultValue) {
        return PermissionsModule.get().hasPermission(uuid, permission, defaultValue);
    }
}
