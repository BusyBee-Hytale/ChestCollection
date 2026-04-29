package com.busybee.chestcollector.database;

import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.Level;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.JDBC;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DatabaseManager {

    private final ChestCollectorPlugin plugin;
    private HikariDataSource dataSource;
    private ConnectionSource connectionSource;
    private Dao<CollectorData, String> collectorDao;

    public DatabaseManager(ChestCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        if (!plugin.getConfig().getBoolean("database.debug", false)) {
            Logger.setGlobalLogLevel(Level.WARNING);
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();

            if (type.equals("mysql") || type.equals("mariadb")) {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String database = plugin.getConfig().getString("database.mysql.database", "chestcollector");
                String username = plugin.getConfig().getString("database.mysql.username", "root");
                String password = plugin.getConfig().getString("database.mysql.password", "");

                String protocol = type.equals("mariadb") ? "mariadb" : "mysql";
                hikariConfig.setJdbcUrl("jdbc:" + protocol + "://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);

                hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            } else {
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                File dbFile = new File(dataFolder, "collectors.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setDriverClassName(JDBC.class.getName());
            }

            hikariConfig.setMinimumIdle(2);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(600000);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
            hikariConfig.setPoolName("ChestCollector-Pool");

            dataSource = new HikariDataSource(hikariConfig);
            connectionSource = new DataSourceConnectionSource(dataSource, dataSource.getJdbcUrl());

            TableUtils.createTableIfNotExists(connectionSource, CollectorData.class);

            collectorDao = DaoManager.createDao(connectionSource, CollectorData.class);

            ChestCollectorPlugin.LOGGER.atInfo().log("Database connection established (" + type + ").");
            return true;
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Failed to initialize database");
            return false;
        }
    }

    public void shutdown() {
        if (!plugin.getConfig().getBoolean("database.debug", false)) {
            Logger.setGlobalLogLevel(Level.WARNING);
        }

        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (dataSource != null) {
                dataSource.close();
            }
            ChestCollectorPlugin.LOGGER.atInfo().log("Database connection closed.");
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error closing database connection");
        }
    }

    public Dao<CollectorData, String> getCollectorDao() {
        return collectorDao;
    }

    public void saveCollectorsBatch(java.util.Collection<CollectorData> collectors) {
        if (!plugin.getConfig().getBoolean("database.debug", false)) {
            Logger.setGlobalLogLevel(Level.WARNING);
        }

        try {
            collectorDao.callBatchTasks(() -> {
                for (CollectorData collector : collectors) {
                    collector.preSave();
                    collectorDao.createOrUpdate(collector);
                }
                return null;
            });
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atSevere().withCause(e).log("Failed to save collectors in batch");
        }
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable);
    }
}
