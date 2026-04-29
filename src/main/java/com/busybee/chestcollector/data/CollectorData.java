package com.busybee.chestcollector.data;

import com.hypixel.hytale.math.vector.Vector3d;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "collectors")
public class CollectorData {
    @DatabaseField(id = true, columnName = "id")
    private String idString;

    private UUID id;

    @DatabaseField(columnName = "owner_id")
    private String ownerIdString;

    private UUID ownerId;

    @DatabaseField(columnName = "world_id")
    private String worldId;

    @DatabaseField(columnName = "x")
    private double x;
    @DatabaseField(columnName = "y")
    private double y;
    @DatabaseField(columnName = "z")
    private double z;

    private Vector3d position;

    @DatabaseField(columnName = "radius")
    private int radius;

    @DatabaseField(columnName = "filter_items")
    private String filterItemsString;

    private List<String> filterItems;

    @DatabaseField(columnName = "filter_enabled")
    private boolean filterEnabled;

    @DatabaseField(columnName = "whitelist")
    private boolean whitelist;

    @DatabaseField(columnName = "enabled")
    private boolean enabled;

    @DatabaseField(columnName = "items_collected")
    private long itemsCollected;

    @DatabaseField(columnName = "notification_type")
    private String notificationType;

    public CollectorData() {
        // Required by ORMLite
    }

    public CollectorData(UUID ownerId, Vector3d position, String worldId) {
        this.id = UUID.randomUUID();
        this.idString = this.id.toString();
        this.ownerId = ownerId;
        this.ownerIdString = this.ownerId.toString();
        this.position = position;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.worldId = worldId;
        this.radius = 10;
        this.filterItems = new ArrayList<>();
        this.filterItems.add("all");
        this.updateFilterItemsString();
        this.filterEnabled = false;
        this.whitelist = true;
        this.enabled = true;
        this.itemsCollected = 0;
        this.notificationType = "NOTIFICATION";
    }

    /**
     * Called after loading from database to sync fields.
     */
    public void postLoad() {
        if (idString != null) this.id = UUID.fromString(idString);
        if (ownerIdString != null) this.ownerId = UUID.fromString(ownerIdString);
        this.position = new Vector3d(x, y, z);
        if (filterItemsString != null && !filterItemsString.isEmpty()) {
            this.filterItems = new ArrayList<>(Arrays.asList(filterItemsString.split(",")));
        } else {
            this.filterItems = new ArrayList<>();
            this.filterItems.add("all");
        }
    }

    /**
     * Called before saving to database to sync fields.
     */
    public void preSave() {
        this.idString = id.toString();
        this.ownerIdString = ownerId.toString();
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.updateFilterItemsString();
    }

    private void updateFilterItemsString() {
        if (filterItems == null || filterItems.isEmpty()) {
            this.filterItemsString = "all";
        } else {
            this.filterItemsString = String.join(",", filterItems);
        }
    }

    public boolean isInRange(Vector3d itemPosition) {
        double dx = position.x - itemPosition.x;
        double dy = position.y - itemPosition.y;
        double dz = position.z - itemPosition.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= radius;
    }

    public boolean matchesFilter(String itemId) {
        if (filterItems.contains("all")) {
            return true;
        }
        return filterItems.stream().anyMatch(filter -> 
            itemId.toLowerCase().contains(filter.toLowerCase())
        );
    }

    public UUID getId() {
        return id;
    }
    public UUID getOwnerId() {
        return ownerId;
    }
    public Vector3d getPosition() {
        return position;
    }
    public String getWorldId() {
        return worldId;
    }
    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }
    public int getRadius() {
        return radius;
    }
    public void setRadius(int radius) {
        this.radius = Math.max(1, Math.min(10, radius));
    }
    public List<String> getItemFilters() {
        return filterItems;
    }
    public void setItemFilters(List<String> itemFilters) {
        this.filterItems = itemFilters;
    }
    public List<String> getFilterItems() {
        return filterItems;
    }
    public boolean isFilterEnabled() {
        return filterEnabled;
    }
    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }
    public boolean isWhitelist() {
        return whitelist;
    }
    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public long getItemsCollected() {
        return itemsCollected;
    }
    public void incrementItemsCollected() {
        this.itemsCollected++;
    }
    public String getNotificationType() {
        return notificationType;
    }
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("ownerId", ownerId.toString());
        map.put("worldId", worldId);
        map.put("x", position.x);
        map.put("y", position.y);
        map.put("z", position.z);
        map.put("radius", radius);
        map.put("filterItems", new ArrayList<>(filterItems));
        map.put("filterEnabled", filterEnabled);
        map.put("whitelist", whitelist);
        map.put("enabled", enabled);
        map.put("itemsCollected", itemsCollected);
        map.put("notificationType", notificationType);
        return map;
    }

    public static CollectorData deserialize(Map<?, ?> map) {
        UUID id = UUID.fromString((String) map.get("id"));
        UUID ownerId = UUID.fromString((String) map.get("ownerId"));
        String worldId = (String) map.get("worldId");
        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        Vector3d position = new Vector3d(x, y, z);

        CollectorData collector = new CollectorData(ownerId, position, worldId);
        collector.id = id;
        collector.radius = ((Number) map.get("radius")).intValue();
        collector.filterItems = new ArrayList<>((List<String>) map.get("filterItems"));
        
        Object filterEnabled = map.get("filterEnabled");
        collector.filterEnabled = filterEnabled != null ? (Boolean) filterEnabled : false;
        
        Object whitelist = map.get("whitelist");
        collector.whitelist = whitelist != null ? (Boolean) whitelist : true;

        collector.enabled = (Boolean) map.get("enabled");
        collector.itemsCollected = ((Number) map.get("itemsCollected")).longValue();
        collector.notificationType = (String) map.get("notificationType");
        return collector;
    }
}
