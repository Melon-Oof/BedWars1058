/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles quick deposit of BedWars currencies (iron/gold/diamonds/emeralds)
 * from a player's inventory into their team chest via left-click.
 *
 * <p>Left-clicking a chest in the player's base transfers all currency stacks
 * from the player's inventory into the chest inventory. If the chest is full,
 * remaining items stay in the player's inventory and the player is notified.</p>
 */
public class QuickDepositListener implements Listener {

    private static final Set<Material> CURRENCIES = EnumSet.of(
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.EMERALD
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClickChest(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material blockType = block.getType();
        boolean isChest = blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST;
        boolean isEnderChest = blockType == Material.ENDER_CHEST;
        if (!isChest && !isEnderChest) return;

        Player player = event.getPlayer();

        if (!BedWars.config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ENABLE_QUICK_DEPOSIT)) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return;
        if (arena.isSpectator(player)) return;
        if (arena.getRespawnSessions().containsKey(player)) return;

        Inventory targetInventory;

        if (isChest) {
            ITeam playerTeam = arena.getTeam(player);
            if (playerTeam == null) return;
            int islandRadius = arena.getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS);
            if (playerTeam.getSpawn().distance(block.getLocation()) > islandRadius) return;
            if (!(block.getState() instanceof Chest)) return;
            event.setCancelled(true);
            targetInventory = ((Chest) block.getState()).getBlockInventory();
        } else {
            event.setCancelled(true);
            targetInventory = player.getEnderChest();
        }

        Map<Material, Integer> deposited = new LinkedHashMap<>();
        for (Material currency : CURRENCIES) {
            deposited.put(currency, 0);
        }

        boolean chestFull = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!CURRENCIES.contains(stack.getType())) continue;

            Material mat = stack.getType();
            ItemStack toDeposit = stack.clone();
            java.util.HashMap<Integer, ItemStack> leftover = targetInventory.addItem(toDeposit);

            if (leftover.isEmpty()) {
                player.getInventory().setItem(i, null);
                deposited.merge(mat, toDeposit.getAmount(), Integer::sum);
            } else {
                ItemStack remaining = leftover.values().iterator().next();
                int movedAmount = toDeposit.getAmount() - remaining.getAmount();
                if (movedAmount > 0) {
                    deposited.merge(mat, movedAmount, Integer::sum);
                    ItemStack newStack = stack.clone();
                    newStack.setAmount(remaining.getAmount());
                    player.getInventory().setItem(i, newStack);
                }
                chestFull = true;
                break;
            }
        }

        int totalTransferred = deposited.values().stream().mapToInt(Integer::intValue).sum();
        String destLabel = isEnderChest ? "ender chest" : "team chest";
        if (totalTransferred > 0) {
            player.updateInventory();
            if (isChest && block.getState() instanceof Chest) {
                ((Chest) block.getState()).update();
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Material, Integer> entry : deposited.entrySet()) {
                if (entry.getValue() <= 0) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getValue()).append("x ").append(entry.getKey().name().toLowerCase().replace("_", " "));
            }
            String suffix = chestFull ? " (chest full)" : "";
            player.sendMessage("\u00a7aQuick deposit \u00a77[" + destLabel + "]\u00a7r: \u00a7f" + sb + suffix);

            if (BedWars.config.getBoolean("debug")) {
                BedWars.plugin.getLogger().info("[QuickDeposit] " + player.getName() + " -> " + deposited);
            }
        } else {
            player.sendMessage("\u00a7cNo currencies to deposit into the \u00a7e" + destLabel + (chestFull ? " \u00a7c(full)" : "") + "\u00a7c.");
        }
    }
}
