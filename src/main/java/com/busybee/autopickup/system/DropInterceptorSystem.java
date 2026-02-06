package com.busybee.autopickup.system;

import com.busybee.autopickup.utils.AutoPickupUtil;
import com.busybee.autopickup.manager.AutoPickupManager;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class DropInterceptorSystem extends RefSystem<EntityStore> {

    private final AutoPickupManager manager;
    private final Map<Vector3i, BreakEntry> breakCache;
    private final Map<UUID, PlayerRef> onlinePlayers;
    private final ScheduledExecutorService scheduler;

    public DropInterceptorSystem() {
        this.manager = AutoPickupManager.getInstance();
        this.breakCache = new ConcurrentHashMap<>();
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(this::cleanupOldEntries, 1, 1, TimeUnit.SECONDS);
    }

    public void trackPlayer(PlayerRef playerRef) {
        onlinePlayers.put(playerRef.getUuid(), playerRef);
    }
    public void untrackPlayer(UUID playerId) {
        onlinePlayers.remove(playerId);
    }
    public void markBreak(Vector3i position, UUID playerUuid) {
        breakCache.put(position, new BreakEntry(playerUuid, System.currentTimeMillis()));
    }

    public void markVolume(Vector3i center, UUID playerUuid, int horizontalRadius, int verticalRadius) {
        long timestamp = System.currentTimeMillis();
        BreakEntry entry = new BreakEntry(playerUuid, timestamp);

        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = 0; y <= verticalRadius; y++) {
                    Vector3i pos = new Vector3i(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );
                    breakCache.put(pos, entry);
                }
            }
        }
    }

    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        breakCache.entrySet().removeIf(entry ->
                now - entry.getValue().timestamp > 5000
        );
    }

    public void shutdown() {
        scheduler.shutdown();
        onlinePlayers.clear();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                ItemComponent.getComponentType(),
                TransformComponent.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (reason != AddReason.SPAWN) {
            return;
        }

        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d position = transform.getPosition();
        Vector3i blockPos = new Vector3i(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );

        BreakEntry breakEntry = breakCache.get(blockPos);
        if (breakEntry == null) return;

        PlayerRef playerRef = onlinePlayers.get(breakEntry.playerUuid);
        if (playerRef == null) return;

        ItemComponent itemComponent = commandBuffer.getComponent(ref, ItemComponent.getComponentType());
        if (itemComponent == null || itemComponent.getItemStack() == null) return;

        ItemStack itemStack = itemComponent.getItemStack();
        String itemId = itemStack.getItemId();

        if (!manager.isBlockAllowed(itemId)) {
            return;
        }

        Player player = AutoPickupUtil.getPlayer(playerRef);
        if (player == null) return;

        int originalQuantity = itemStack.getQuantity();
        ItemStack remainder = AutoPickupUtil.giveItem(player, itemStack);

        if (remainder == null || remainder.isEmpty()) {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
        } else if (remainder.getQuantity() < originalQuantity) {
            itemComponent.setItemStack(remainder);
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
    }

    private static class BreakEntry {
        final UUID playerUuid;
        final long timestamp;

        BreakEntry(UUID playerUuid, long timestamp) {
            this.playerUuid = playerUuid;
            this.timestamp = timestamp;
        }
    }
}
