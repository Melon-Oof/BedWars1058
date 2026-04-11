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

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerInvisibilityPotionEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.sidebar.SidebarService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * This is used to hide and show player name tag above head when he drinks an invisibility
 * potion or when the potion is gone. It is required because it is related to scoreboards.
 */
public class InvisibilityPotionListener implements Listener {

    @EventHandler
    public void onPotion(@NotNull PlayerInvisibilityPotionEvent e) {
        if (e.getTeam() == null) return;
        syncVisibility(e.getArena(), e.getPlayer(), e.getType() == PlayerInvisibilityPotionEvent.Type.ADDED);
        SidebarService.getInstance().handleInvisibility(
                e.getTeam(), e.getPlayer(), e.getType() == PlayerInvisibilityPotionEvent.Type.ADDED
        );
    }

    @EventHandler
    public void onDrink(PlayerItemConsumeEvent e) {
        IArena a = Arena.getArenaByPlayer(e.getPlayer());
        if (a == null) return;
        if (e.getItem().getType() != Material.POTION) return;
        // remove potion bottle
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                        nms.minusAmount(e.getPlayer(), new ItemStack(Material.GLASS_BOTTLE), 1),
                5L);

        if (nms.isInvisibilityPotion(e.getItem())) {
            // Poll for the invisibility effect up to ~2 seconds to handle server lag
            applyInvisibilityWhenPresent(a, e.getPlayer(), 0);

            // Re-hide armor at tick+20: vanilla sends ENTITY_EQUIPMENT after the bottle
            // removal, which can restore visible armor. Overwrite it 1 second later.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!e.getPlayer().isOnline()) return;
                if (!e.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) return;
                syncVisibility(a, e.getPlayer(), true);
            }, 20L);
        }
    }

    private void applyInvisibilityWhenPresent(IArena arena, Player player, int attempt) {
        if (arena == null || player == null || !player.isOnline()) return;

        PotionEffect invis = null;
        for (PotionEffect pe : player.getActivePotionEffects()) {
            if (pe.getType() == PotionEffectType.INVISIBILITY) {
                invis = pe;
                break;
            }
        }

        if (invis == null) {
            if (attempt >= 20) return;
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyInvisibilityWhenPresent(arena, player, attempt + 1), 2L);
            return;
        }

        ITeam t = arena.getTeam(player);
        int durationSeconds = Math.max(1, invis.getDuration() / 20);
        if (arena.getShowTime().containsKey(player)) {
            arena.getShowTime().replace(player, durationSeconds);
        } else {
            arena.getShowTime().put(player, durationSeconds);
        }

        syncVisibility(arena, player, true);
        if (t != null) {
            Bukkit.getPluginManager().callEvent(new PlayerInvisibilityPotionEvent(PlayerInvisibilityPotionEvent.Type.ADDED, t, player, t.getArena()));
        }
    }

    /**
     * Show or hide a player (and their armor) to all nearby players based on invisibility rules.
     */
    public static void syncVisibility(IArena arena, Player victim, boolean hidden) {
        if (arena == null || victim == null) return;
        for (Player viewer : victim.getWorld().getPlayers()) {
            if (viewer.equals(victim)) continue;
            if (hidden) {
                if (shouldHidePlayerForViewer(arena, victim, viewer)) {
                    nms.spigotHidePlayer(victim, viewer);
                    nms.hideArmor(victim, viewer);
                } else {
                    nms.spigotShowPlayer(victim, viewer);
                    nms.showArmor(victim, viewer);
                }
            } else {
                nms.spigotShowPlayer(victim, viewer);
                nms.showArmor(victim, viewer);
            }
        }
    }

    /**
     * Returns true if the given viewer should not see the invisible victim.
     * Teammates can see through if {@code invisibility.team-can-see-armor} is enabled.
     */
    public static boolean shouldHidePlayerForViewer(IArena arena, Player victim, Player viewer) {
        if (arena == null || victim == null || viewer == null) return false;
        if (viewer.equals(victim)) return false;
        if (plugin.getConfig().getBoolean(ConfigPath.INVISIBILITY_TEAM_CAN_SEE_ARMOR, false)) {
            ITeam victimTeam = arena.getTeam(victim);
            if (victimTeam == null) return true;
            if (arena.isSpectator(viewer)) return true;
            ITeam viewerTeam = arena.getTeam(viewer);
            return viewerTeam == null || !victimTeam.equals(viewerTeam);
        }
        return true;
    }

    /**
     * Convenience alias used when deciding whether to send a hide-armor packet.
     */
    public static boolean shouldHideArmorForViewer(IArena arena, Player victim, Player viewer) {
        return shouldHidePlayerForViewer(arena, victim, viewer);
    }
}
