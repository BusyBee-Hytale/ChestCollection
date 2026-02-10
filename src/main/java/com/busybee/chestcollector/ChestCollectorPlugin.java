package com.busybee.chestcollector;

import ai.kodari.hylib.commons.scheduler.Scheduler;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.chestcollector.commands.CollectorCommand;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.systems.ItemCollectionSystem;
import com.busybee.chestcollector.systems.PlaceBlockHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.*;

public class ChestCollectorPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ChestCollectorPlugin instance;
    private YamlConfig config;
    private YamlConfig messages;
    private YamlConfig collectorsData;

    private Map<UUID, CollectorData> collectors;

    public ChestCollectorPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ChestCollectorPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        YamlConfig.init(this);

        this.config = new YamlConfig("config.yml");
        this.messages = new YamlConfig("messages.yml");
        this.collectorsData = new YamlConfig("collectors.yml");

        com.busybee.chestcollector.util.MessageUtil.init();

        this.collectors = new HashMap<>();
        loadCollectors();

        // Register commands
        getCommandRegistry().registerCommand(new CollectorCommand());

        // Register systems
        getEntityStoreRegistry().registerSystem(new PlaceBlockHandler());
        getEntityStoreRegistry().registerSystem(new com.busybee.chestcollector.systems.BreakBlockHandler());
        getEntityStoreRegistry().registerSystem(new ItemCollectionSystem());

        LOGGER.atInfo().log("ChestCollector enabled!");
    }

    @Override
    protected void shutdown() {
        saveCollectors();
        Scheduler.shutdown();
        LOGGER.atInfo().log("ChestCollector disabled!");
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
        saveCollectors();
    }

    public void removeCollector(CollectorData collector) {
        collectors.remove(collector.getId());
        saveCollectors();
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

    private void saveCollectors() {
        List<Map<String, Object>> collectorsList = new ArrayList<>();
        for (CollectorData collector : collectors.values()) {
            collectorsList.add(collector.serialize());
        }
        collectorsData.set("collectors", collectorsList);
        collectorsData.save();
    }
}
