package com.busybee.autopickup.listeners;

import com.busybee.autopickup.manager.AutoPickupManager;
import com.busybee.autopickup.system.DropInterceptorSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.UUID;

public class MobDropListener extends DeathSystems.OnDeathSystem {

    private final AutoPickupManager manager;
    private final DropInterceptorSystem dropInterceptor;

    public MobDropListener(DropInterceptorSystem dropInterceptor) {
        this.manager = AutoPickupManager.getInstance();
        this.dropInterceptor = dropInterceptor;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!manager.isGloballyEnabled()) {
            return;
        }

        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) return;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        Ref<EntityStore> killerRef = entitySource.getRef();

        Player player = store.getComponent(killerRef, Player.getComponentType());
        if (player == null) return;

        if (manager.isDisabledInCreative() && player.getGameMode() == GameMode.Creative) {
            return;
        }

        UUIDComponent killerUuidComponent = store.getComponent(killerRef, UUIDComponent.getComponentType());
        if (killerUuidComponent == null) return;
        UUID playerUUID = killerUuidComponent.getUuid();

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) return;

        if (!manager.isEnabled(playerUUID)) {
            return;
        }

        if (!manager.hasPermission(playerUUID)) {
            manager.setEnabled(playerUUID, false);
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d position = transform.getPosition();
        Vector3i blockPos = new Vector3i(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );

        dropInterceptor.markVolume(blockPos, playerUUID, 2, 2);
    }
}
