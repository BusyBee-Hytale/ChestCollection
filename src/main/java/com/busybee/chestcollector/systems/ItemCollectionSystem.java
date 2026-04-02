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
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
                boolean shouldCollect = collector.isWhitelist() ? matches : !matches;
                if (!shouldCollect) continue;
            }
            
            if (tryCollectItem(collector, itemStack, itemWorld)) {
                collector.incrementItemsCollected();
                
                commandBuffer.removeEntity(itemRef, RemoveReason.REMOVE);
                
                PlayerRef ownerRef = Universe.get().getPlayer(collector.getOwnerId());
                if (ownerRef != null && ownerRef.isValid()) {
                    String notifType = collector.getNotificationType();
                    NotificationUtil.sendCollectionNotification(ownerRef, itemStack.getQuantity(), notifType);
                }
                
                break;
            }
        }
    }
    
    private boolean tryCollectItem(CollectorData collector, ItemStack itemStack, World world) {
        Vector3d pos = collector.getPosition();
        
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);
        
        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return false;
        
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(blockX, blockY, blockZ);
        if (holder == null) return false;
        
        ItemContainerBlock containerBlock = holder.getComponent(ItemContainerBlock.getComponentType());
        if (containerBlock == null) return false;
        
        ItemContainer inventory = containerBlock.getItemContainer();
        
        ItemStackTransaction transaction = inventory.addItemStack(itemStack);
        
        if (transaction.succeeded()) {
            ItemStack remainder = transaction.getRemainder();
            return remainder == null || remainder.isEmpty();
        }
        
        return false;
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());
    }
}
