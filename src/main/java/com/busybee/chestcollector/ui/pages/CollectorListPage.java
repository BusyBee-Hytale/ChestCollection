package com.busybee.chestcollector.ui.pages;

import ai.kodari.hylib.commons.util.ChatUtil;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.ui.data.CollectorListData;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CollectorListPage extends InteractiveCustomUIPage<CollectorListData> {

    private final UUID playerId;

    public CollectorListPage(@Nonnull PlayerRef playerRef, UUID playerId) {
        super(playerRef, CustomPageLifetime.CanDismiss, CollectorListData.CODEC);
        this.playerId = playerId;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/ChestCollector/CollectorList.ui");

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Button", "Close"),
            false
        );

        this.buildCollectorList(cmd, events);
    }

    private void buildCollectorList(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.clear("#CollectorList");

        List<CollectorData> playerCollectors = new ArrayList<>();
        for (CollectorData collector : ChestCollectorPlugin.getInstance().getCollectors()) {
            if (collector.getOwnerId().equals(playerId)) {
                playerCollectors.add(collector);
            }
        }

        if (playerCollectors.isEmpty()) {
            cmd.set("#EmptyMessage.Visible", true);
            return;
        }

        cmd.set("#EmptyMessage.Visible", false);

        for (int i = 0; i < playerCollectors.size(); i++) {
            CollectorData collector = playerCollectors.get(i);

            cmd.append("#CollectorList", "Pages/ChestCollector/Components/CollectorListItem.ui");

            String statusText = collector.isEnabled()
                ? MessageUtil.get("ui.list.status.active")
                : MessageUtil.get("ui.list.status.disabled");
            String statusColor = collector.isEnabled() ? "#22c55e" : "#ef4444";

            cmd.set("#CollectorList[" + i + "] #StatusLabel.TextSpans",
                ChatUtil.parse("<color:" + statusColor + "><bold>" + statusText + "</bold></color>")
            );

            String location = String.format("(%d, %d, %d)",
                (int) collector.getPosition().x,
                (int) collector.getPosition().y,
                (int) collector.getPosition().z
            );
            cmd.set("#CollectorList[" + i + "] #LocationLabel.Text", location);

            cmd.set("#CollectorList[" + i + "] #ItemsCollectedLabel.Text", String.valueOf(collector.getItemsCollected()));
            cmd.set("#CollectorList[" + i + "] #RadiusLabel.Text", String.valueOf(collector.getRadius()));

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CollectorList[" + i + "] #ConfigureBtn",
                EventData.of("Action", "Configure:" + collector.getId().toString()),
                false
            );
        }
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CollectorListData data
    ) {
        super.handleDataEvent(ref, store, data);
        var player = store.getComponent(ref, Player.getComponentType());

        if (data.getButton() != null) {
            if ("Close".equals(data.getButton())) {
                player.getPageManager().setPage(ref, store, Page.None);
                return;
            }
        }

        if (data.getAction() != null) {
            String[] parts = data.getAction().split(":", 2);

            if (parts.length == 2 && "Configure".equals(parts[0])) {
                try {
                    UUID collectorId = UUID.fromString(parts[1]);
                    player.getPageManager().openCustomPage(ref, store, new CollectorSettingsPage(playerRef, collectorId));
                } catch (IllegalArgumentException e) {
                    ChestCollectorPlugin.LOGGER.atSevere().log("Invalid collector UUID: " + parts[1]);
                    this.sendUpdate();
                }
            }
        }
    }
}
