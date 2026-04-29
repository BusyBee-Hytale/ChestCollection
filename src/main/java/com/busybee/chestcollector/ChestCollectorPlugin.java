package com.busybee.chestcollector;

import ai.kodari.hylib.commons.scheduler.Scheduler;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.chestcollector.commands.CollectorCommand;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.database.DatabaseManager;
import com.busybee.chestcollector.systems.ItemCollectionSystem;
import com.busybee.chestcollector.systems.PlaceBlockHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;
import java.sql.SQLException;
import com.hypixel.hytale.server.core.HytaleServer;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestCollectorPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ChestCollectorPlugin instance;
    private YamlConfig config;
    private YamlConfig messages;
    private YamlConfig collectorsData;
    private DatabaseManager databaseManager;
    private Map<UUID, CollectorData> collectors;
    private Map<String, Long> breakingCollectors;

    public ChestCollectorPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ChestCollectorPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        YamlConfig.init(this);

        this.config = new YamlConfig("config.yml");
        this.messages = new YamlConfig("messages.yml");
        this.collectorsData = new YamlConfig("collectors.yml");

        com.busybee.chestcollector.util.MessageUtil.init();

        this.collectors = new ConcurrentHashMap<>();
        this.breakingCollectors = new ConcurrentHashMap<>();

        this.databaseManager = new DatabaseManager(this);
        if (this.databaseManager.initialize()) {
            loadCollectorsFromDatabase();
            checkMigration();
        } else {
            loadCollectors();
        }

        boolean verboseLogging = this.config.getBoolean("hstats.verbose-logging", false);
        new HStats("b44725bc-666c-4f23-a9a6-0eba1c191a91", "2026.4.0", verboseLogging);

        getCommandRegistry().registerCommand(new CollectorCommand());
        getEntityStoreRegistry().registerSystem(new PlaceBlockHandler());
        getEntityStoreRegistry().registerSystem(new com.busybee.chestcollector.systems.BreakBlockHandler());
        getEntityStoreRegistry().registerSystem(new com.busybee.chestcollector.systems.ItemPickupHandler());
        getEntityStoreRegistry().registerSystem(new ItemCollectionSystem());

        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::saveAllCollectors, 5, 5, TimeUnit.MINUTES);
        LOGGER.atInfo().log("ChestCollector enabled!");
    }

    @Override
    protected void shutdown() {
        saveAllCollectorsSync();
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        Scheduler.shutdown();
        LOGGER.atInfo().log("ChestCollector disabled!");
    }

    public void saveAllCollectors() {
        databaseManager.runAsync(this::saveAllCollectorsSync);
    }

    public void saveAllCollectorsSync() {
        LOGGER.atInfo().log("Saving all collectors to database...");
        if (databaseManager != null && !collectors.isEmpty()) {
            databaseManager.saveCollectorsBatch(collectors.values());
        }
    }

    public YamlConfig getConfig() {
        return config;
    }
    public YamlConfig getMessages() {
        return messages;
    }
    public Collection<CollectorData> getCollectors() {
        return collectors.values();
    }
    public Map<UUID, CollectorData> getAllCollectors() {
        return new HashMap<>(collectors);
    }
    public CollectorData getCollector(UUID id) {
        return collectors.get(id);
    }

    public long getCollectorCountForPlayer(UUID playerId) {
        return collectors.values().stream()
            .filter(c -> c.getOwnerId().equals(playerId))
            .count();
    }

    public void addCollector(CollectorData collector) {
        collectors.put(collector.getId(), collector);
        saveCollectorAsync(collector);
    }

    public void removeCollector(CollectorData collector) {
        collectors.remove(collector.getId());
        deleteCollectorAsync(collector);
    }

    private void loadCollectorsFromDatabase() {
        try {
            List<CollectorData> fromDb = databaseManager.getCollectorDao().queryForAll();
            for (CollectorData collector : fromDb) {
                collector.postLoad();
                collectors.put(collector.getId(), collector);
            }
            LOGGER.atInfo().log("Loaded %d collectors from database", collectors.size());
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load collectors from database");
        }
    }

    private void checkMigration() {
        if (!collectorsData.contains("collectors")) return;

        List<Map<?, ?>> collectorsList = (List<Map<?, ?>>) collectorsData.get("collectors");
        if (collectorsList == null || collectorsList.isEmpty()) return;

        LOGGER.atInfo().log("Found legacy collectors.yml, starting migration...");
        int count = 0;
        for (Map<?, ?> map : collectorsList) {
            try {
                CollectorData collector = CollectorData.deserialize(map);
                if (!collectors.containsKey(collector.getId())) {
                    collectors.put(collector.getId(), collector);
                    saveCollectorAsync(collector);
                    count++;
                }
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to migrate collector data");
            }
        }
        LOGGER.atInfo().log("Migrated %d collectors to database", count);
        
        // Optionally clear or rename legacy file
        // collectorsData.set("collectors", new ArrayList<>());
        // collectorsData.save();

        LOGGER.atWarning().log("Legacy collectors.yml migration complete. You may now delete collectors.yml.");
    }

    public void saveCollectorAsync(CollectorData collector) {
        databaseManager.runAsync(() -> {
            try {
                collector.preSave();
                databaseManager.getCollectorDao().createOrUpdate(collector);
            } catch (SQLException e) {
                LOGGER.atSevere().withCause(e).log("Failed to save collector to database");
            }
        });
    }

    public void deleteCollectorAsync(CollectorData collector) {
        databaseManager.runAsync(() -> {
            try {
                databaseManager.getCollectorDao().delete(collector);
            } catch (SQLException e) {
                LOGGER.atSevere().withCause(e).log("Failed to delete collector from database");
            }
        });
    }

    private void loadCollectors() {
        List<Map<?, ?>> collectorsList = (List<Map<?, ?>>) collectorsData.get("collectors");
        if (collectorsList != null) {
            for (Map<?, ?> map : collectorsList) {
                try {
                    CollectorData collector = CollectorData.deserialize(map);
                    collectors.put(collector.getId(), collector);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load collector data");
                }
            }
        }
    }

    public File getDataFolder() {
        return new File(System.getProperty("user.dir"), "mods/ChestCollector/data");
    }
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void trackCollectorBreak(String worldId, int x, int y, int z) {
        String key = worldId + ":" + x + ":" + y + ":" + z;
        breakingCollectors.put(key, System.currentTimeMillis());
        LOGGER.atInfo().log("Tracking collector break at %s", key);
    }

    public boolean isCollectorBeingBroken(String worldId, int x, int y, int z) {
        String key = worldId + ":" + x + ":" + y + ":" + z;
        Long timestamp = breakingCollectors.get(key);
        if (timestamp == null) return false;

        long elapsed = System.currentTimeMillis() - timestamp;
        if (elapsed > 3000) {
            breakingCollectors.remove(key);
            return false;
        }
        return true;
    }

    public void removeCollectorBreakTracking(String worldId, int x, int y, int z) {
        String key = worldId + ":" + x + ":" + y + ":" + z;
        breakingCollectors.remove(key);
        LOGGER.atInfo().log("Removed collector break tracking for %s", key);
    }
}

