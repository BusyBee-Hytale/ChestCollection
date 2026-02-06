package com.busybee.autopickup.listeners;

import com.busybee.autopickup.manager.AutoPickupManager;
import com.busybee.autopickup.system.DropInterceptorSystem;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class AutoPickupListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final AutoPickupManager manager;
    private final DropInterceptorSystem dropInterceptor;

    public AutoPickupListener(DropInterceptorSystem dropInterceptor) {
        super(BreakBlockEvent.class);
        this.manager = AutoPickupManager.getInstance();
        this.dropInterceptor = dropInterceptor;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event
    ) {
        if (event.isCancelled()) {
            return;
        }
        if (!manager.isGloballyEnabled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;
        UUID playerUUID = uuidComponent.getUuid();

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (manager.isDisabledInCreative() && player.getGameMode() == GameMode.Creative) {
            return;
        }

        if (!manager.isEnabled(playerUUID)) {
            return;
        }

        if (!manager.hasPermission(playerUUID)) {
            manager.setEnabled(playerUUID, false);
            return;
        }

        String blockId = event.getBlockType().getId();

        if (!manager.isBlockAllowed(blockId)) {
            return;
        }

        Vector3i blockPos = event.getTargetBlock();
        if (blockPos == null) return;

        String lowerBlockId = blockId.toLowerCase();
        if (lowerBlockId.contains("trunk")) {
            dropInterceptor.markVolume(blockPos, playerUUID, 6, 20);
        } else {
            dropInterceptor.markBreak(blockPos, playerUUID);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
