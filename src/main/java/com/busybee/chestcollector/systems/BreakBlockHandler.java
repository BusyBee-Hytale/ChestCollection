package com.busybee.chestcollector.systems;

import ai.kodari.hylib.commons.message.Messenger;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class BreakBlockHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public BreakBlockHandler() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event
    ) {
        try {
            // Don't process if event is already cancelled
            if (event.isCancelled()) return;

            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) return;
            UUID playerUUID = uuidComponent.getUuid();

            Vector3i blockPos = event.getTargetBlock();
            if (blockPos == null) return;

            String worldId = store.getExternalData().getWorld().getName();
            if (worldId == null) return;

        // Check if a collector exists at this position
        CollectorData collectorToRemove = null;
        for (CollectorData collector : ChestCollectorPlugin.getInstance().getCollectors()) {
            if (!collector.getWorldId().equals(worldId)) continue;

            Vector3d collectorPos = collector.getPosition();
            int collectorX = (int) Math.floor(collectorPos.x);
            int collectorY = (int) Math.floor(collectorPos.y);
            int collectorZ = (int) Math.floor(collectorPos.z);

            if (collectorX == blockPos.x && collectorY == blockPos.y && collectorZ == blockPos.z) {
                collectorToRemove = collector;
                break;
            }
        }

        // Only process if we found a collector at this position
        if (collectorToRemove == null) return;

        // Verify the block being broken is actually a chest before removing the collector
        BlockType blockType = event.getBlockType();
        if (blockType == null || !blockType.getId().equals("Furniture_Crude_Chest_Small")) return;

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) return;

            ChestCollectorPlugin.getInstance().removeCollector(collectorToRemove);

            String message = MessageUtil.get("commands.collector.removed");
            if (message != null && !message.isEmpty()) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + message);
            }
        } catch (Exception e) {
            // Silently catch any errors to prevent interfering with other plugins
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error in BreakBlockHandler");
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
