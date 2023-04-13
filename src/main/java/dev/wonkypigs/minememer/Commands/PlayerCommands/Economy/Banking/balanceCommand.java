package dev.wonkypigs.minememer.Commands.PlayerCommands.Economy.Banking;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.wonkypigs.minememer.MineMemer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.wonkypigs.minememer.helpers.*;

@CommandAlias("mm|minememer")
public class balanceCommand extends BaseCommand {
    private static final MineMemer plugin = MineMemer.getInstance();

    @Subcommand("bank|balance")
    public void viewBankSelf(Player player) throws Exception {
        showPlayerBank(player, player.getUniqueId());
    }

    @Syntax("<player>")
    @CommandCompletion("*")
    @Subcommand("bank|balance")
    public void viewBankOther(Player player, OfflinePlayer target) throws Exception {
        showPlayerBank(player, target.getUniqueId());
    }

    public void showPlayerBank(Player player, UUID targetUUID) {
        Inventory inv;
        OfflinePlayer target;
        if (player.getUniqueId().equals(targetUUID)) {
            target = Bukkit.getOfflinePlayer(targetUUID);
            inv = plugin.getServer().createInventory(null, 27, plugin.lang.getString("other-bank-menu-title")
                    .replace("&", "§")
                    .replace("{name}", target.getName())
            );
        } else {
            target = player;
            inv = plugin.getServer().createInventory(null, 27, plugin.lang.getString("self-bank-menu-title")
                    .replace("&", "§")
            );
        }
        // get user's bank data
        CompletableFuture<ResultSet> future = CompletableFuture.supplyAsync(() -> {
            ResultSet results = grabBankData(targetUUID);
            return results;
        }).whenComplete((result, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            }
        });

        future.thenAccept((results) -> {
            // set local variables for the data
            int purse = 0, bankStored = 0, bankLimit = 0;
            try {
                purse = results.getInt("purse");
                bankStored = results.getInt("bankStored");
                bankLimit = results.getInt("bankLimit");
            } catch (Exception e) {
                sendErrorToPlayer(player, plugin.lang.getString("player-not-found-error"));
            }
            // menu background
            for (int i = 0; i < 27; i++) {
                ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("");
                item.setItemMeta(meta);
                inv.setItem(i, item);
            }

            // Player head item
            ItemStack skullItem = generatePlayerHead(target);
            SkullMeta skullMeta = (SkullMeta) skullItem.getItemMeta();
            skullMeta.setDisplayName(plugin.lang.getString("bank-player-skull-item-name")
                    .replace("&", "§")
                    .replace("{name}", player.getDisplayName())
            );
            ArrayList<String> lore = new ArrayList<>();
            lore.add(plugin.lang.getString("bank-player-skull-lore")
                    .replace("&", "§")
                    .replace("{amount}", String.valueOf(purse+bankStored))
                    .replace("{currency}", plugin.currencyName)
            );
            skullMeta.setLore(lore);
            skullItem.setItemMeta(skullMeta);
            inv.setItem(4, skullItem);

            // Bank item
            ItemStack bankItem = new ItemStack(Material.GOLD_BLOCK);
            ItemMeta bankMeta = bankItem.getItemMeta();
            bankMeta.setDisplayName(plugin.lang.getString("bank-bank-item-name")
                    .replace("&", "§")
                    .replace("{bankMoney}", String.valueOf(bankStored))
                    .replace("{bankLimit}", String.valueOf(bankLimit))
                    .replace("{currency}", plugin.currencyName)
            );
            bankItem.setItemMeta(bankMeta);
            inv.setItem(11, bankItem);

            // Purse item
            ItemStack balanceItem = new ItemStack(Material.PAPER);
            ItemMeta balanceMeta = balanceItem.getItemMeta();
            plugin.getLogger().info(plugin.currencyName);
            plugin.getLogger().info("" + purse);
            balanceMeta.setDisplayName((plugin.lang.getString("bank-purse-item-name"))
                    .replace("&", "§")
                    .replace("{currency}", plugin.currencyName)
                    .replace("{purseMoney}", String.valueOf(purse))
            );
            balanceItem.setItemMeta(balanceMeta);
            inv.setItem(15, balanceItem);

            // open menu
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(inv);
            });
        });
    }
}
