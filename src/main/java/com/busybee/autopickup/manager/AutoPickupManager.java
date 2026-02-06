package com.busybee.autopickup.manager;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.chestcollector.ChestCollectorPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoPickupManager {

    private static AutoPickupManager instance;
    private final Map<UUID, Boolean> playerStates;
    private final Map<UUID, Long> permissionCache;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    public AutoPickupManager() {
        instance = this;
        this.playerStates = new ConcurrentHashMap<>();
        this.permissionCache = new ConcurrentHashMap<>();
        loadPlayerStates();
    }

    public static AutoPickupManager getInstance() {
        return instance;
    }
    public boolean isEnabled(UUID playerId) {
        return playerStates.getOrDefault(playerId, getDefaultState());
    }

    public void toggleAutopickup(UUID playerId) {
        boolean currentState = isEnabled(playerId);
        boolean newState = !currentState;
        playerStates.put(playerId, newState);
        savePlayerStates();
    }

    public void setEnabled(UUID playerId, boolean enabled) {
        playerStates.put(playerId, enabled);
        savePlayerStates();
    }

    public boolean hasPermission(UUID playerId) {
        Long cacheTime = permissionCache.get(playerId);
        if (cacheTime != null && System.currentTimeMillis() - cacheTime < CACHE_DURATION) {
            return true;
        }

        boolean hasPermission = true;

        if (hasPermission) {
            permissionCache.put(playerId, System.currentTimeMillis());
        }

        return hasPermission;
    }

    public void clearPermissionCache(UUID playerId) {
        permissionCache.remove(playerId);
    }

    private boolean getDefaultState() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getBoolean("autopickup.default-enabled", false);
    }

    public boolean isGloballyEnabled() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getBoolean("autopickup.enabled", true);
    }

    public boolean isBlockAllowed(String blockId) {
        if (blockId == null) return false;

        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();

        boolean whitelistEnabled = config.getBoolean("autopickup.whitelist-enabled", false);
        boolean blacklistEnabled = config.getBoolean("autopickup.blacklist-enabled", false);

        String lowerBlockId = blockId.toLowerCase();

        if (whitelistEnabled) {
            List<String> whitelist = config.getStringList("autopickup.whitelist");
            if (whitelist == null || whitelist.isEmpty()) {
                return false;
            }

            for (String pattern : whitelist) {
                if (lowerBlockId.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        if (blacklistEnabled) {
            List<String> blacklist = config.getStringList("autopickup.blacklist");
            if (blacklist == null || blacklist.isEmpty()) {
                return true;
            }

            for (String pattern : blacklist) {
                if (lowerBlockId.contains(pattern.toLowerCase())) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

    public int getPickupDelay() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getInt("autopickup.pickup-delay-ticks", 0);
    }

    public boolean isDisabledInCreative() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getBoolean("autopickup.disable-in-creative", true);
    }

    public String getNotificationType() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getString("autopickup.notification-type", "NOTIFICATION");
    }

    public String getToggleNotificationType() {
        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        return config.getString("autopickup.toggle-notification-type", "TITLE");
    }

    private void loadPlayerStates() {
        YamlConfig data = getDataFile();
        Map<String, Object> states = data.getConfigurationSection("player-states");

        if (states != null) {
            for (Map.Entry<String, Object> entry : states.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    boolean enabled = (Boolean) entry.getValue();
                    playerStates.put(playerId, enabled);
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    private void savePlayerStates() {
        YamlConfig data = getDataFile();
        Map<String, Boolean> states = new HashMap<>();

        for (Map.Entry<UUID, Boolean> entry : playerStates.entrySet()) {
            states.put(entry.getKey().toString(), entry.getValue());
        }

        data.set("player-states", states);
        data.save();
    }

    private YamlConfig getDataFile() {
        return new YamlConfig("autopickup-data.yml");
    }
}
