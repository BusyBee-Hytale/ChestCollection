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

        if (world.getChunk(chunkId) == null) {
            return false;
        }

        Holder<ChunkStore> holder = world.getBlockComponentHolder(blockX, blockY, blockZ);
        if (holder == null) {
            ChestCollectorPlugin.LOGGER.atWarning().log("No holder at %d %d %d - removing invalid collector", blockX, blockY, blockZ);
            ChestCollectorPlugin.getInstance().removeCollector(collector);
            return false;
        }

        ItemContainerBlock containerBlock = holder.getComponent(ItemContainerBlock.getComponentType());
        if (containerBlock == null) {
            ChestCollectorPlugin.LOGGER.atWarning().log("No container block at %d %d %d - removing invalid collector", blockX, blockY, blockZ);
            ChestCollectorPlugin.getInstance().removeCollector(collector);
            return false;
        }

        SimpleItemContainer inventory = containerBlock.getItemContainer();
        if (inventory == null || !inventory.canAddItemStack(itemStack, true, true)) {
            return false;
        }

        String itemId = itemStack.getItemId();
        int quantity = itemStack.getQuantity();
        final BsonDocument metadata = itemStack.getMetadata();
        java.util.UUID ownerId = collector.getOwnerId();
        String notifType = collector.getNotificationType();

        world.execute(() -> {
            if (!itemRef.isValid()) return;

            WorldChunk c = world.getChunk(chunkId);
            if (c == null) {
                ChestCollectorPlugin.LOGGER.atWarning().log("Chunk %d not found for collection at %d %d %d", chunkId, blockX, blockY, blockZ);
                return;
            }

            // Use & 31 for 32x32 chunk relative coordinates
            int relX = blockX & 31;
            int relZ = blockZ & 31;

            Ref<ChunkStore> ref = BlockModule.ensureBlockEntity(c, relX, blockY, relZ);
            if (ref == null || !ref.isValid()) {
                ChestCollectorPlugin.LOGGER.atWarning().log("Block entity ref null or invalid for %d %d %d in chunk %d - removing invalid collector", blockX, blockY, blockZ, chunkId);
                ChestCollectorPlugin.getInstance().removeCollector(collector);
                return;
            }

            Store<ChunkStore> store = ref.getStore();
            ItemContainerBlock cb = store.getComponent(ref, ItemContainerBlock.getComponentType());
            if (cb == null) return;

            SimpleItemContainer inv = cb.getItemContainer();
            if (inv == null) return;

            ItemStack transactionStack = new ItemStack(itemId, quantity, metadata);

            if (!inv.canAddItemStack(transactionStack, true, true)) {
                return;
            }

            ItemStackTransaction transaction = inv.addItemStack(transactionStack);

            if (transaction.succeeded()) {
                cb.setItemContainer(inv);
                store.replaceComponent(ref, ItemContainerBlock.getComponentType(), cb);

                BlockModule.BlockStateInfo info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                if (info != null) {
                    info.markNeedsSaving(store);
                }

                collector.incrementItemsCollected();
                // Count updated in memory, saved periodically

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



