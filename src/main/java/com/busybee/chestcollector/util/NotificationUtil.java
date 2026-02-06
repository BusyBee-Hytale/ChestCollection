package com.busybee.chestcollector.util;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.ChatUtil;
import ai.kodari.hylib.commons.util.Notifications;
import ai.kodari.hylib.commons.util.Titles;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class NotificationUtil {

    public static void sendCollectionNotification(PlayerRef playerRef, int count, String notificationType) {
        String titleText = MessageUtil.format("notifications.items-collected.title");
        String bodyText = MessageUtil.format("notifications.items-collected.body", "%count%", String.valueOf(count));

        switch (notificationType.toUpperCase()) {
            case "NOTIFICATION" -> Notifications.player(
                playerRef,
                Message.raw(titleText).color("#22c55e"),
                Message.raw(bodyText),
                null,
                NotificationStyle.Success
            );
            case "TITLE" -> Titles.player(
                playerRef,
                ChatUtil.parse("<color:#22c55e>" + titleText),
                ChatUtil.parse("<white>" + bodyText),
                false
            );
            case "CHAT" -> Messenger.sendMessage(
                playerRef,
                "<color:#22c55e>[Collector]</color> <white>" + bodyText
            );
            case "NONE" -> {
                // Silent - no notification
            }
        }
    }
}
