package com.busybee.chestcollector.ui.pages;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.ChatUtil;
import ai.kodari.hylib.commons.util.Titles;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.busybee.chestcollector.data.CollectorData;
import com.busybee.chestcollector.ui.data.CollectorSettingsData;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CollectorSettingsPage extends InteractiveCustomUIPage<CollectorSettingsData> {

    private final UUID collectorId;
    private String filterInputValue = "";

    public CollectorSettingsPage(@Nonnull PlayerRef playerRef, UUID collectorId) {
        super(playerRef, CustomPageLifetime.CanDismiss, CollectorSettingsData.CODEC);
        this.collectorId = collectorId;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/ChestCollector/CollectorSettings.ui");
        this.setupValues(cmd);
        this.bindEvents(events);
        this.buildFilterList(cmd, events);
    }

    private void setupValues(UICommandBuilder cmd) {
        CollectorData collector = ChestCollectorPlugin.getInstance().getCollector(collectorId);
        if (collector == null) return;

        String activeText = MessageUtil.get("ui.settings.status.active");
        String disabledText = MessageUtil.get("ui.settings.status.disabled");

        cmd.set("#StatusLabel.TextSpans",
            collector.isEnabled()
                ? ChatUtil.parse("<color:#22c55e><bold>" + activeText + "</bold></color>")
                : ChatUtil.parse("<color:#ef4444><bold>" + disabledText + "</bold></color>")
        );

        cmd.set("#ItemsCollectedValue.Text", String.valueOf(collector.getItemsCollected()));

        String radiusText = MessageUtil.format("ui.settings.labels.radius", "%radius%", String.valueOf(collector.getRadius()));
        cmd.set("#RadiusLabel.Text", radiusText);

        cmd.set("#FilterInput.Value", this.filterInputValue);

        List<DropdownEntryInfo> notificationOptions = new ArrayList<>();
        notificationOptions.add(new DropdownEntryInfo(LocalizableString.fromString("Notification"), "NOTIFICATION"));
        notificationOptions.add(new DropdownEntryInfo(LocalizableString.fromString("Title"), "TITLE"));
        notificationOptions.add(new DropdownEntryInfo(LocalizableString.fromString("Chat"), "CHAT"));
        notificationOptions.add(new DropdownEntryInfo(LocalizableString.fromString("None"), "NONE"));
        cmd.set("#NotificationTypeDropdown.Entries", notificationOptions);
        cmd.set("#NotificationTypeDropdown.Value", collector.getNotificationType());
    }

    private void bindEvents(UIEventBuilder events) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Button", "Close"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleBtn",
            EventData.of("Button", "Toggle"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#RadiusSlider",
            EventData.of("Button", "RadiusChanged").append("@RadiusSlider", "#RadiusSlider.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#FilterInput",
            EventData.of("@FilterText", "#FilterInput.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddFilterBtn",
            EventData.of("Button", "AddFilter"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#NotificationTypeDropdown",
            EventData.of("@NotificationTypeDropdown", "#NotificationTypeDropdown.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#DeleteButton",
            EventData.of("Button", "Delete"),
            false
        );
    }

    private void buildFilterList(UICommandBuilder cmd, UIEventBuilder events) {
        CollectorData collector = ChestCollectorPlugin.getInstance().getCollector(collectorId);
        if (collector == null) return;

        cmd.clear("#FilterList");

        if (collector.getItemFilters().isEmpty()) {
            return;
        }

        for (int i = 0; i < collector.getItemFilters().size(); i++) {
            cmd.append("#FilterList", "Pages/ChestCollector/Components/FilterItem.ui");
            cmd.set("#FilterList[" + i + "] #FilterText.Text", collector.getItemFilters().get(i));

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FilterList[" + i + "] #RemoveBtn",
                EventData.of("Action", "RemoveFilter:" + i),
                false
            );
        }
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CollectorSettingsData data
    ) {
        super.handleDataEvent(ref, store, data);
        var player = store.getComponent(ref, Player.getComponentType());
        CollectorData collector = ChestCollectorPlugin.getInstance().getCollector(collectorId);

        if (collector == null) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.getFilterText() != null) {
            this.filterInputValue = data.getFilterText();
        }

        if (data.getNotificationTypeDropdown() != null) {
            collector.setNotificationType(data.getNotificationTypeDropdown());
            ChestCollectorPlugin.getInstance().saveCollectorAsync(collector);
            this.sendUpdate();
            return;
        }

        if (data.getRadiusSlider() != null) {
            int cappedRadius = Math.min(10, data.getRadiusSlider().intValue());
            collector.setRadius(cappedRadius);
            ChestCollectorPlugin.getInstance().saveCollectorAsync(collector);
            this.updateRadiusLabel();
            return;
        }

        if (data.getButton() != null) {
            switch (data.getButton()) {
                case "Close" -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                    return;
                }
                case "Toggle" -> {
                    boolean wasEnabled = collector.isEnabled();
                    collector.setEnabled(!wasEnabled);

                    String message = wasEnabled
                        ? MessageUtil.get("commands.collector.toggle.disabled")
                        : MessageUtil.get("commands.collector.toggle.enabled");

                    String color = wasEnabled ? "#ef4444" : "#22c55e";
                    Messenger.sendMessage(this.playerRef, "<color:" + color + ">" + message);

                    Titles.player(
                        this.playerRef,
                        ChatUtil.parse("<color:" + color + ">" + message),
                        ChatUtil.parse(wasEnabled ? "<white>Collector Disabled" : "<white>Collector Activated"),
                        true
                    );

                    ChestCollectorPlugin.getInstance().saveCollectorAsync(collector);
                    this.updateStatus();
                    return;
                }
                case "AddFilter" -> {
                    if (this.filterInputValue.isEmpty()) {
                        String message = MessageUtil.get("ui.settings.filter.empty-input");
                        Messenger.sendMessage(this.playerRef, "<color:#ef4444>" + message);
                        this.sendUpdate();
                        return;
                    }
                    collector.getItemFilters().remove("all");
                    collector.getItemFilters().add(this.filterInputValue);
                    this.filterInputValue = "";
                    ChestCollectorPlugin.getInstance().saveCollectorAsync(collector);
                    this.rebuildFilterList();
                    return;
                }
                case "Delete" -> {
                    World world = store.getExternalData().getWorld();
                    Vector3d pos = collector.getPosition();
                    int x = (int) Math.floor(pos.x);
                    int y = (int) Math.floor(pos.y);
                    int z = (int) Math.floor(pos.z);

                    BsonDocument metadata = new BsonDocument();
                    metadata.put("collector_chest", new BsonBoolean(true));
                    ItemStack collectorItem = new ItemStack("Furniture_Crude_Chest_Small", 1, metadata);

                    world.execute(() -> {
                        world.setBlock(x, y, z, "air");

                        ItemStackTransaction transaction = player.getInventory().getCombinedHotbarFirst().addItemStack(collectorItem);
                        if (transaction.getRemainder() != null && !transaction.getRemainder().isEmpty()) {
                            spawnItemAtPlayer(world, store, ref, transaction.getRemainder());
                            Messenger.sendMessage(this.playerRef, "<color:#f59e0b>Inventory full! Collector dropped at your feet.");
                        }
                    });

                    ChestCollectorPlugin.getInstance().removeCollector(collector);
                    String message = MessageUtil.get("commands.collector.deleted");
                    Messenger.sendMessage(this.playerRef, "<color:#ef4444>" + message);
                    player.getPageManager().setPage(ref, store, Page.None);
                    return;
                }
            }
        }

        if (data.getAction() != null) {
            String[] parts = data.getAction().split(":", 2);
            String action = parts[0];
            int index = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;

            if ("RemoveFilter".equals(action) && index >= 0 && index < collector.getItemFilters().size()) {
                collector.getItemFilters().remove(index);
                if (collector.getItemFilters().isEmpty()) {
                    collector.getItemFilters().add("all");
                }
                ChestCollectorPlugin.getInstance().saveCollectorAsync(collector);
                this.rebuildFilterList();
                return;
            }
        }

        this.sendUpdate();
    }

    private void spawnItemAtPlayer(World world, Store<EntityStore> store, Ref<EntityStore> playerRef, ItemStack itemStack) {
        try {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            Vector3d position = transform != null ? transform.getPosition() : new Vector3d(0, 0, 0);
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, new Vector3f(0, 0, 0)));
            holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
            holder.ensureComponent(UUIDComponent.getComponentType());
            store.addEntity(holder, AddReason.SPAWN);
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning().withCause(e).log("Error spawning item at player");
        }
    }

    private void updateStatus() {
        UICommandBuilder cmd = new UICommandBuilder();
        CollectorData collector = ChestCollectorPlugin.getInstance().getCollector(collectorId);
        if (collector == null) return;

        String activeText = MessageUtil.get("ui.settings.status.active");
        String disabledText = MessageUtil.get("ui.settings.status.disabled");

        cmd.set("#StatusLabel.TextSpans",
            collector.isEnabled()
                ? ChatUtil.parse("<color:#22c55e><bold>" + activeText + "</bold></color>")
                : ChatUtil.parse("<color:#ef4444><bold>" + disabledText + "</bold></color>")
        );

        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.buildFilterList(cmd, events);
        this.sendUpdate(cmd, events, false);
    }

    private void updateRadiusLabel() {
        UICommandBuilder cmd = new UICommandBuilder();
        CollectorData collector = ChestCollectorPlugin.getInstance().getCollector(collectorId);
        if (collector == null) return;

        String radiusText = MessageUtil.format("ui.settings.labels.radius", "%radius%", String.valueOf(collector.getRadius()));
        cmd.set("#RadiusLabel.Text", radiusText);

        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.buildFilterList(cmd, events);
        this.sendUpdate(cmd, events, false);
    }

    private void rebuildFilterList() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#FilterInput.Value", "");
        this.filterInputValue = "";

        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.buildFilterList(cmd, events);
        this.sendUpdate(cmd, events, false);
    }
}

