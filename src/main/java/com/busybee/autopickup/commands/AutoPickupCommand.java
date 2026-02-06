package com.busybee.autopickup.commands;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.Notifications;
import com.busybee.autopickup.manager.AutoPickupManager;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

public class AutoPickupCommand extends AbstractPlayerCommand {

    private final AutoPickupManager manager;

    public AutoPickupCommand() {
        super("autopickup", "Toggle automatic item pickup");
        this.manager = AutoPickupManager.getInstance();
    }

    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;
        UUID playerUUID = uuidComponent.getUuid();

        if (!manager.isGloballyEnabled()) {
            String message = MessageUtil.get("autopickup.disabled-globally");
            Messenger.sendMessage(playerRef, "<color:#ef4444>" + message);
            return;
        }

        if (!manager.hasPermission(playerUUID)) {
            String message = MessageUtil.get("autopickup.no-permission");
            Messenger.sendMessage(playerRef, "<color:#ef4444>" + message);
            return;
        }

        manager.toggleAutopickup(playerUUID);
        boolean isEnabled = manager.isEnabled(playerUUID);

        // Messages and colors based on current state after toggle
        String messageKey = isEnabled ? "autopickup.enabled" : "autopickup.disabled";
        String message = MessageUtil.get(messageKey);
        String color = isEnabled ? "#22c55e" : "#ef4444";
        String statusTitle = isEnabled ? "Auto Pickup ENABLED" : "Auto Pickup DISABLED";
        String statusSubtitle = isEnabled ? "Items will be auto-collected" : "Items will drop normally";

        String notificationType = manager.getToggleNotificationType();
        switch (notificationType.toUpperCase()) {
            case "TITLE":
                EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0.0f);
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        Message.raw(statusTitle).color(color),
                        Message.raw(statusSubtitle).color("#ffffff"),
                        true,
                        null,
                        3.0f,
                        2.0f,
                        1.0f
                );
                break;

            case "NOTIFICATION":
                world.execute(() -> {
                    try {
                        Notifications.player(
                                playerRef,
                                Message.raw("Auto Pickup").color(color),
                                Message.raw(statusSubtitle).color("#ffffff"),
                                null,
                                isEnabled ? NotificationStyle.Success : NotificationStyle.Warning
                        );
                    } catch (Exception e) {
                        Messenger.sendMessage(playerRef, "<color:" + color + ">" + message);
                    }
                });
                break;

            case "CHAT":
                Messenger.sendMessage(playerRef, "<color:" + color + ">" + message);
                break;

            case "NONE":
                break;

            default:
                EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0.0f);
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        Message.raw(statusTitle).color(color),
                        Message.raw(statusSubtitle).color("#ffffff"),
                        true,
                        null,
                        3.0f,
                        2.0f,
                        1.0f
                );
                break;
        }
    }
}
