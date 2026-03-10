package com.busybee.chestcollector.systems;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.Notifications;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.ui.pages.CollectorSettingsPage;
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
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PlaceBlockHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public PlaceBlockHandler() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;
        UUID playerUUID = uuidComponent.getUuid();

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null) return;

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null) return;

        if (!itemInHand.getItemId().equals("Furniture_Crude_Chest_Small")) return;

        BsonDocument metadata = itemInHand.getMetadata();
        if (metadata == null || !metadata.containsKey("collector_chest")) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && !player.hasPermission("chestcollector.place")) {
            Messenger.sendMessage(playerRef, "<color:#ef4444>" + MessageUtil.get("commands.collector.no-permission"));
            event.setCancelled(true);
            return;
        }

        YamlConfig config = ChestCollectorPlugin.getInstance().getConfig();
        int maxCollectors = config.getInt("collector.max-per-player", 5);

        long currentCount = ChestCollectorPlugin.getInstance().getCollectorCountForPlayer(playerUUID);
        if (currentCount >= maxCollectors) {
            String message = MessageUtil.format("commands.collector.max-limit", "%limit%", String.valueOf(maxCollectors));
            Messenger.sendMessage(playerRef, "<color:#ef4444>" + message);
            event.setCancelled(true);
            return;
        }

        Vector3i blockPos = event.getTargetBlock();
        Vector3d position = new Vector3d(blockPos.x + 0.5, blockPos.y, blockPos.z + 0.5);

        String worldId = store.getExternalData().getWorld().getName();
        CollectorData collector = new CollectorData(playerUUID, position, worldId);

        int defaultRadius = config.getInt("collector.default-radius", 10);
        collector.setRadius(defaultRadius);

        String defaultNotificationType = config.getString("collector.notification-type", "NOTIFICATION");
        collector.setNotificationType(defaultNotificationType);

        ChestCollectorPlugin.getInstance().addCollector(collector);

        String notifTitle = MessageUtil.get("notifications.collector-placed.title");
        String notifBody = MessageUtil.get("notifications.collector-placed.body");

        Notifications.player(
            playerRef,
            Message.raw(notifTitle).color("#22c55e"),
            Message.raw(notifBody),
            null,
            NotificationStyle.Success
        );

        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new CollectorSettingsPage(playerRef, collector.getId()));
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
