package com.busybee.chestcollector.commands;

import ai.kodari.hylib.commons.message.Messenger;
import com.busybee.chestcollector.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

public class CollectorCommand extends AbstractCommandCollection {

    public CollectorCommand() {
        super("collector", "Manage collector chests");
        addSubCommand((AbstractCommand) new GetSubCommand());
        addSubCommand((AbstractCommand) new SettingsSubCommand());
        addSubCommand((AbstractCommand) new ListSubCommand());
        addSubCommand((AbstractCommand) new HelpSubCommand());
    }

    static class GetSubCommand extends AbstractPlayerCommand {

        GetSubCommand() {
            super("get", "Get a collector chest");
        }

        @Override
        public String getPermission() {
            return "chestcollector.command.get";
        }

        @Override
        protected void execute(
                @Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            if (!player.hasPermission("chestcollector.command.get")) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + MessageUtil.get("commands.collector.no-permission"));
                return;
            }

            Item chestItem = Item.getAssetMap().getAsset("Furniture_Crude_Chest_Small");
            if (chestItem == null) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>Chest item not found!");
                return;
            }

            BsonDocument metadata = new BsonDocument();
            metadata.put("collector_chest", new BsonBoolean(true));

            ItemStack collectorChest = new ItemStack("Furniture_Crude_Chest_Small", 1, metadata);

            world.execute(() -> {
                ItemStackTransaction transaction = player.getInventory()
                    .getCombinedHotbarFirst()
                    .addItemStack(collectorChest);

                ItemStack remainder = transaction.getRemainder();
                if (remainder != null && !remainder.isEmpty()) {
                    Messenger.sendMessage(playerRef, "<color:#ef4444>Inventory full!");
                } else {
                    String message = MessageUtil.get("commands.collector.chest-given");
                    Messenger.sendMessage(playerRef, "<color:#22c55e>" + message);
                }
            });
        }
    }

    static class SettingsSubCommand extends AbstractPlayerCommand {

        SettingsSubCommand() {
            super("settings", "Open collector settings");
        }

        @Override
        public String getPermission() {
            return "chestcollector.command.settings";
        }

        @Override
        protected void execute(
                @Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            if (!player.hasPermission("chestcollector.command.settings")) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + MessageUtil.get("commands.collector.no-permission"));
                return;
            }

            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) return;

            player.getPageManager().openCustomPage(ref, store, new com.busybee.chestcollector.ui.pages.CollectorListPage(playerRef, uuidComponent.getUuid()));
        }
    }

    static class HelpSubCommand extends AbstractPlayerCommand {

        HelpSubCommand() {
            super("help", "Show command help");
        }

        @Override
        public String getPermission() {
            return "chestcollector.command.help";
        }

        @Override
        protected void execute(
                @Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            if (!player.hasPermission("chestcollector.command.help")) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + MessageUtil.get("commands.collector.no-permission"));
                return;
            }

            Messenger.sendMessage(playerRef, "<color:#93844c><bold>Collector Commands:");
            Messenger.sendMessage(playerRef, "<color:#ffffff>/collector get <color:#64748b>- Get a collector chest");
            Messenger.sendMessage(playerRef, "<color:#ffffff>/collector settings <color:#64748b>- Open collector management UI");
            Messenger.sendMessage(playerRef, "<color:#ffffff>/collector list <color:#64748b>- List all your collectors");
        }
    }

    static class ListSubCommand extends AbstractPlayerCommand {

        ListSubCommand() {
            super("list", "List all your collectors");
        }

        @Override
        public String getPermission() {
            return "chestcollector.command.settings";
        }

        @Override
        protected void execute(
                @Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            if (!player.hasPermission("chestcollector.command.settings")) {
                Messenger.sendMessage(playerRef, "<color:#ef4444>" + MessageUtil.get("commands.collector.no-permission"));
                return;
            }

            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) return;

            player.getPageManager().openCustomPage(ref, store, new com.busybee.chestcollector.ui.pages.CollectorListPage(playerRef, uuidComponent.getUuid()));
        }
    }
}
