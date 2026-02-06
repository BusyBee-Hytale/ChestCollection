package com.busybee.autopickup.utils;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.Notifications;
import com.busybee.autopickup.manager.AutoPickupManager;
import com.busybee.chestcollector.ChestCollectorPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class AutoPickupUtil {

    public static Player getPlayer(PlayerRef playerRef) {
        if (playerRef == null) return null;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return null;

        Store<EntityStore> store = ref.getStore();
        return store.getComponent(ref, Player.getComponentType());
    }

    public static ItemStack giveItem(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }

        try {
            ItemStackTransaction transaction = player.getInventory()
                    .getCombinedHotbarFirst()
                    .addItemStack(itemStack);

            if (transaction.succeeded()) {
                ItemStack remainder = transaction.getRemainder();

                int originalQuantity = itemStack.getQuantity();
                int pickedUpQuantity = originalQuantity;

                if (remainder != null && !remainder.isEmpty()) {
                    pickedUpQuantity = originalQuantity - remainder.getQuantity();
                }

                if (pickedUpQuantity > 0) {
                    ItemStack pickedUpStack = itemStack.withQuantity(pickedUpQuantity);
                    sendItemPickupNotification(player, pickedUpStack);
                }

                return remainder;
            }

            return itemStack;
        } catch (Exception e) {
            ChestCollectorPlugin.LOGGER.atWarning()
                    .withCause(e)
                    .log("Failed to give item to player");
            return itemStack;
        }
    }

    public static void sendItemPickupNotification(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getQuantity() <= 0) {
            return;
        }

        try {
            World world = player.getWorld();
            if (world == null) return;

            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            String notificationType = AutoPickupManager.getInstance().getNotificationType();
            if ("NONE".equalsIgnoreCase(notificationType)) {
                return; // No notification
            }

            world.execute(() -> {
                try {
                    String itemName = itemStack.getItemId();
                    if (itemName.contains("_")) {
                        String[] parts = itemName.split("_");
                        if (parts.length > 0) {
                            itemName = parts[parts.length - 1];
                        }
                    }

                    int quantity = itemStack.getQuantity();
                    String message = "+" + quantity + " " + itemName;

                    switch (notificationType.toUpperCase()) {
                        case "NOTIFICATION":
                            Notifications.player(
                                    playerRef,
                                    Message.raw("Auto Pickup").color("#22c55e"),
                                    Message.raw(message).color("#ffffff"),
                                    null,
                                    NotificationStyle.Success
                            );
                            break;

                        case "TITLE":
                            Notifications.player(
                                    playerRef,
                                    Message.raw("Auto Pickup").color("#22c55e"),
                                    Message.raw(message).color("#ffffff"),
                                    null,
                                    NotificationStyle.Success
                            );
                            break;

                        case "CHAT":
                            Messenger.sendMessage(playerRef, "<color:#22c55e>[Auto Pickup] <color:#ffffff>" + message);
                            break;

                        default:
                            Notifications.player(
                                    playerRef,
                                    Message.raw("Auto Pickup").color("#22c55e"),
                                    Message.raw(message).color("#ffffff"),
                                    null,
                                    NotificationStyle.Success
                            );
                            break;
                    }
                } catch (Exception e) {
                }
            });
        } catch (Exception e) {
        }
    }
}
