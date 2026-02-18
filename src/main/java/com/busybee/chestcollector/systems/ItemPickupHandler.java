package com.busybee.chestcollector.systems;

import com.busybee.chestcollector.ChestCollectorPlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

public class ItemPickupHandler extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    public ItemPickupHandler() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractivelyPickupItemEvent event
    ) {
        try {
            if (event.isCancelled()) return;

            ItemStack itemStack = event.getItemStack();
            if (itemStack == null) return;

            if (!"Furniture_Crude_Chest_Small".equals(itemStack.getItemId())) return;

            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;

            Vector3d position = transform.getPosition();
            String worldId = store.getExternalData().getWorld().getName();

            int blockX = (int) Math.floor(position.x);
            int blockY = (int) Math.floor(position.y);
            int blockZ = (int) Math.floor(position.z);

            if (ChestCollectorPlugin.getInstance().isCollectorBeingBroken(worldId, blockX, blockY, blockZ)) {
                ChestCollectorPlugin.LOGGER.atInfo().log("Converting picked up chest to collector at {}, {}, {}", blockX, blockY, blockZ);
                BsonDocument metadata = itemStack.getMetadata();
                if (metadata == null) {
                    metadata = new BsonDocument();
                }
                metadata.put("collector_chest", new BsonBoolean(true));
                ItemStack collectorChest = new ItemStack(
                    itemStack.getItemId(),
                    itemStack.getQuantity(),
                    metadata
                );

                event.setItemStack(collectorChest);

                ChestCollectorPlugin.getInstance().removeCollectorBreakTracking(worldId, blockX, blockY, blockZ);
            }
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error in ItemPickupHandler");
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
