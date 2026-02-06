package com.busybee.chestcollector.data;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CollectorData {
    private UUID id;
    private final UUID ownerId;
    private final Vector3d position;
    private String worldId;
    private int radius;
    private List<String> filterItems;
    private boolean filterEnabled;
    private boolean whitelist;
    private boolean enabled;
    private long itemsCollected;
    private String notificationType;

    public CollectorData(UUID ownerId, Vector3d position, String worldId) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.position = position;
        this.worldId = worldId;
        this.radius = 10;
        this.filterItems = new ArrayList<>();
        this.filterItems.add("all");
        this.filterEnabled = false;
        this.whitelist = true;
        this.enabled = true;
        this.itemsCollected = 0;
        this.notificationType = "NOTIFICATION";
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
        this.radius = radius;
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