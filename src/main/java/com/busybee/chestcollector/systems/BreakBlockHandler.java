package com.busybee.chestcollector.systems;

import ai.kodari.hylib.commons.message.Messenger;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;

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
            if (event.isCancelled()) return;

            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) return;
            UUID playerUUID = uuidComponent.getUuid();

            Vector3i blockPos = event.getTargetBlock();
            if (blockPos == null) return;

            String worldId = store.getExternalData().getWorld().getName();
            if (worldId == null) return;

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

        if (collectorToRemove == null) return;

        BlockType blockType = event.getBlockType();
        if (blockType == null || !blockType.getId().equals("Furniture_Crude_Chest_Small")) return;

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) return;

            // Cancel the default block drop to prevent normal chest from dropping
            event.setCancelled(true);

            // Remove the collector data
            ChestCollectorPlugin.getInstance().removeCollector(collectorToRemove);

            // Give the player back a chest with collector_chest metadata
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                BsonDocument metadata = new BsonDocument();
                metadata.put("collector_chest", new BsonBoolean(true));
                ItemStack collectorChest = new ItemStack("Furniture_Crude_Chest_Small", 1, metadata);

                World world = store.getExternalData().getWorld();
                world.execute(() -> {
                    ItemStackTransaction transaction = player.getInventory()
                        .getCombinedHotbarFirst()
                        .addItemStack(collectorChest);

                    ItemStack remainder = transaction.getRemainder();
                    if (remainder != null && !remainder.isEmpty()) {
                        // Spawn the chest as an item entity at the block location
                        spawnItemEntity(world, store, remainder, blockPos);
                    }
                });
            }

            String message = MessageUtil.get("commands.collector.removed");
            if (message != null && !message.isEmpty()) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + message);
            }
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error in BreakBlockHandler");
        }
    }

    private void spawnItemEntity(World world, Store<EntityStore> store, ItemStack itemStack, Vector3i blockPos) {
        try {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

            // Set the position at the center of the block, slightly above
            Vector3d position = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
            holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(position, new Vector3f(0, 0, 0)));

            // Add the item component with the chest ItemStack
            holder.addComponent(ItemComponent.getComponentType(),
                new ItemComponent(itemStack));

            // Add UUID component
            holder.ensureComponent(UUIDComponent.getComponentType());

            // Spawn the item entity in the world
            store.addEntity(holder, AddReason.SPAWN);
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error spawning item entity");
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
