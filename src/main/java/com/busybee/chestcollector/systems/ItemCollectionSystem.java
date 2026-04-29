package com.busybee.chestcollector.systems;

import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.util.NotificationUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

public class ItemCollectionSystem extends DelayedEntitySystem<EntityStore> {

    public ItemCollectionSystem() {
        super(1.0f);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> itemRef = archetypeChunk.getReferenceTo(index);
        ItemComponent itemComponent = archetypeChunk.getComponent(index, ItemComponent.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());

        if (itemComponent == null || transform == null) return;

        ItemStack itemStack = itemComponent.getItemStack();
        if (ItemStack.isEmpty(itemStack)) return;

        Vector3d itemPosition = transform.getPosition();
        World itemWorld = store.getExternalData().getWorld();
        String itemWorldId = itemWorld.getName();

        for (CollectorData collector : ChestCollectorPlugin.getInstance().getCollectors()) {
            if (!collector.isEnabled()) continue;
            if (!collector.getWorldId().equals(itemWorldId)) continue;
            if (!collector.isInRange(itemPosition)) continue;

            if (collector.isFilterEnabled()) {
                boolean matches = collector.matchesFilter(itemStack.getItemId());
                boolean shouldCollect = collector.isWhitelist() == matches;
                if (!shouldCollect) continue;
            }

            if (tryCollectItem(collector, itemStack, itemWorld, itemRef, store)) {
                break;
            }
        }
    }

    private boolean tryCollectItem(CollectorData collector, ItemStack itemStack, World world, Ref<EntityStore> itemRef, Store<EntityStore> entityStore) {
        Vector3d pos = collector.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        long chunkId = ChunkUtil.indexChunkFromBlock(blockX, blockZ);

        Holder<ChunkStore> holder = world.getBlockComponentHolder(blockX, blockY, blockZ);
        if (holder == null) {
            ChestCollectorPlugin.LOGGER.atInfo().log("No holder at {} {} {}", blockX, blockY, blockZ);
            return false;
        }

        ItemContainerBlock containerBlock = holder.getComponent(ItemContainerBlock.getComponentType());
        if (containerBlock == null) {
            ChestCollectorPlugin.LOGGER.atInfo().log("No container block at {} {} {}", blockX, blockY, blockZ);
            return false;
        }

        SimpleItemContainer inventory = containerBlock.getItemContainer();
        if (inventory == null || !inventory.canAddItemStack(itemStack, false, true)) {
            return false;
        }

        String itemId = itemStack.getItemId();
        int quantity = itemStack.getQuantity();
        BsonDocument metadata = itemStack.getMetadata();
        java.util.UUID ownerId = collector.getOwnerId();
        String notifType = collector.getNotificationType();

        world.execute(() -> {
            if (!itemRef.isValid()) return;

            WorldChunk c = world.getChunk(chunkId);
            if (c == null) {
                ChestCollectorPlugin.LOGGER.atWarning().log("Chunk {} not found for collection at {} {} {}", chunkId, blockX, blockY, blockZ);
                return;
            }

            // Calculate relative coordinates explicitly using the chunk index
            long index = c.getIndex();
            int chunkX = (int) (index >> 32);
            int chunkZ = (int) index;
            int relX = blockX - (chunkX << 4);
            int relZ = blockZ - (chunkZ << 4);

            Ref<ChunkStore> ref = BlockModule.ensureBlockEntity(c, relX, blockY, relZ);
            if (ref == null || !ref.isValid()) {
                ChestCollectorPlugin.LOGGER.atWarning().log("Block entity ref null or invalid for {} {} {} in chunk {}", blockX, blockY, blockZ, chunkId);
                return;
            }

            Store<ChunkStore> store = ref.getStore();
            ItemContainerBlock cb = store.getComponent(ref, ItemContainerBlock.getComponentType());
            if (cb == null) return;

            SimpleItemContainer inv = cb.getItemContainer();
            if (inv == null) return;

            ItemStack transactionStack = new ItemStack(itemId, quantity, metadata);
            
            // Re-verify capacity inside world thread
            if (!inv.canAddItemStack(transactionStack, false, true)) {
                return;
            }

            // allOrNothing = false, filter = true, notify = true (default)
            ItemStackTransaction transaction = inv.addItemStack(transactionStack, false, true, true);

            if (transaction.succeeded()) {
                cb.setItemContainer(inv);
                store.replaceComponent(ref, ItemContainerBlock.getComponentType(), cb);

                BlockModule.BlockStateInfo info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                if (info != null) {
                    info.markNeedsSaving(store);
                }

                collector.incrementItemsCollected();
                ChestCollectorPlugin.getInstance().addCollector(collector); // Save progress to DB

                if (itemRef.isValid()) {
                    entityStore.removeEntity(itemRef, RemoveReason.REMOVE);
                }

                PlayerRef ownerRef = Universe.get().getPlayer(ownerId);
                if (ownerRef != null && ownerRef.isValid()) {
                    NotificationUtil.sendCollectionNotification(ownerRef, quantity, notifType);
                }
            }
        });

        return true;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());
    }
}
