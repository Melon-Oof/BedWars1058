package com.andrei1058.bedwars.support.version.v1_21_R7;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.entity.Despawnable;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.support.version.common.VersionCommon;
import com.andrei1058.bedwars.support.version.v1_21_R7.despawnable.DespawnableAttributes;
import com.andrei1058.bedwars.support.version.v1_21_R7.despawnable.DespawnableFactory;
import com.andrei1058.bedwars.support.version.v1_21_R7.despawnable.DespawnableType;
import com.mojang.datafixers.util.Pair;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBase;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftFireball;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class v1_21_R7 extends VersionSupport {

    private final DespawnableFactory despawnableFactory;

    public v1_21_R7(Plugin plugin, String name) {
        super(plugin, name);
        loadDefaultEffects();
        this.despawnableFactory = new DespawnableFactory(this);
    }

    @Override
    public void registerVersionListeners() {
        new VersionCommon(this);
    }

    @Override
    public void registerCommand(String name, Command cmd) {
        ((CraftServer) getPlugin().getServer()).getCommandMap().register(name, cmd);
    }

    @Override
    public String getTag(org.bukkit.inventory.ItemStack itemStack, String key) {
        var tag = getTag(itemStack);
        return tag == null ? null : tag.contains(key) ? tag.getString(key) : null;
    }

    @Override
    public void sendTitle(@NotNull Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(title == null ? " " : title, subtitle == null ? " " : subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void spawnSilverfish(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn,
                                double damage) {
        var attr = new DespawnableAttributes(DespawnableType.SILVERFISH, speed, health, damage, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam);

        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_SILVERFISH_NAME,
                PlayerKillEvent.PlayerKillCause.SILVERFISH_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.SILVERFISH
        );
    }

    @Override
    public void spawnIronGolem(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn) {
        var attr = new DespawnableAttributes(DespawnableType.IRON_GOLEM, speed, health, 4, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam);
        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM
        );
    }

    @Override
    public void playAction(@NotNull Player p, String text) {
        p.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.translateAlternateColorCodes('&', text))
        );
    }

    @Override
    public boolean isBukkitCommandRegistered(String name) {
        return ((CraftServer) getPlugin().getServer()).getCommandMap().getCommand(name) != null;
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(@NotNull Player p) {
        return p.getInventory().getItemInMainHand();
    }

    @Override
    public void hideEntity(@NotNull Entity e, Player p) {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(e.getEntityId());
        this.sendPacket(p, packet);
    }

    @Override
    public void minusAmount(Player p, org.bukkit.inventory.@NotNull ItemStack i, int amount) {
        if (i.getAmount() - amount <= 0) {
            if (p.getInventory().getItemInOffHand().equals(i)) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(i);
            }
            return;
        }
        i.setAmount(i.getAmount() - amount);
        p.updateInventory();
    }

    @Override
    public void setSource(TNTPrimed tnt, Player owner) {
        EntityLiving nmsEntityLiving = ((CraftLivingEntity) owner).getHandle();
        EntityTNTPrimed nmsTNT = ((CraftTNTPrimed) tnt).getHandle();
        try {
            Field sourceField = EntityTNTPrimed.class.getDeclaredField("owner");
            sourceField.setAccessible(true);
            sourceField.set(nmsTNT, nmsEntityLiving);
        } catch (NoSuchFieldException e) {
            // Fallback for alternative field name
            try {
                Field sourceField = EntityTNTPrimed.class.getDeclaredField("source");
                sourceField.setAccessible(true);
                sourceField.set(nmsTNT, nmsEntityLiving);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean isArmor(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        // Elytra is not a subclass of ItemArmor, handle separately
        return i instanceof ItemArmor || itemStack.getType() == org.bukkit.Material.ELYTRA;
    }

    @Override
    public boolean isTool(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemTool;
    }

    @Override
    public boolean isSword(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemSword;
    }

    @Override
    public boolean isAxe(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemAxe;
    }

    @Override
    public boolean isBow(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemBow;
    }

    @Override
    public boolean isProjectile(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null) return false;
        org.bukkit.Material type = itemStack.getType();
        return type == org.bukkit.Material.SNOWBALL
                || type == org.bukkit.Material.EGG
                || type == org.bukkit.Material.ENDER_PEARL
                || type == org.bukkit.Material.FIREWORK_ROCKET
                || type == org.bukkit.Material.SPLASH_POTION
                || type == org.bukkit.Material.LINGERING_POTION
                || type == org.bukkit.Material.FIRE_CHARGE
                || type == org.bukkit.Material.TRIDENT
                || type == org.bukkit.Material.BOW
                || type == org.bukkit.Material.CROSSBOW;
    }

    @Override
    public boolean isInvisibilityPotion(org.bukkit.inventory.@NotNull ItemStack itemStack) {
        if (!itemStack.getType().equals(org.bukkit.Material.POTION)) return false;
        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) itemStack.getItemMeta();
        return pm != null && pm.hasCustomEffects() && pm.hasCustomEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void registerEntities() {
    }

    @Override
    public void spawnShop(@NotNull Location loc, String name1, List<Player> players, IArena arena) {
        Location l = loc.clone();
        if (l.getWorld() == null) return;

        Villager vlg = (Villager) l.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        vlg.setAI(false);
        vlg.setRemoveWhenFarAway(false);
        vlg.setCollidable(false);
        vlg.setInvulnerable(true);
        vlg.setSilent(true);

        for (Player p : players) {
            String[] name = Language.getMsg(p, name1).split(",");
            if (name.length == 1) {
                ArmorStand a = createArmorStand(name[0], l.clone().add(0, 1.85, 0));
                new ShopHolo(Language.getPlayerLanguage(p).getIso(), a, null, l, arena);
            } else {
                ArmorStand a = createArmorStand(name[0], l.clone().add(0, 2.1, 0));
                ArmorStand b = createArmorStand(name[1], l.clone().add(0, 1.85, 0));
                new ShopHolo(Language.getPlayerLanguage(p).getIso(), a, b, l, arena);
            }
        }
        for (ShopHolo sh : ShopHolo.getShopHolo()) {
            if (sh.getA() == arena) {
                sh.update();
            }
        }
    }

    @Override
    public double getDamage(org.bukkit.inventory.ItemStack i) {
        var tag = getTag(i);
        if (null == tag) {
            throw new RuntimeException("Provided item has no Tag");
        }
        return tag.getDouble("generic.attackDamage");
    }

    private static ArmorStand createArmorStand(String name, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        ArmorStand a = loc.getWorld().spawn(loc, ArmorStand.class);
        a.setGravity(false);
        a.setVisible(false);
        a.setCustomNameVisible(true);
        a.setCustomName(name);
        return a;
    }

    @Override
    public void voidKill(Player p) {
        EntityPlayer player = getPlayer(p);
        player.hurt(player.damageSources().outOfWorld(), Float.MAX_VALUE);
    }

    @Override
    public void hideArmor(@NotNull Player victim, Player receiver) {
        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> items = new ArrayList<>();
        items.add(new Pair<>(EnumItemSlot.HEAD,  net.minecraft.world.item.ItemStack.EMPTY));
        items.add(new Pair<>(EnumItemSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY));
        items.add(new Pair<>(EnumItemSlot.LEGS,  net.minecraft.world.item.ItemStack.EMPTY));
        items.add(new Pair<>(EnumItemSlot.FEET,  net.minecraft.world.item.ItemStack.EMPTY));
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(victim.getEntityId(), items);
        sendPacket(receiver, packet);
    }

    @Override
    public void showArmor(@NotNull Player victim, Player receiver) {
        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> items = new ArrayList<>();
        items.add(new Pair<>(EnumItemSlot.HEAD,  CraftItemStack.asNMSCopy(victim.getInventory().getHelmet())));
        items.add(new Pair<>(EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(victim.getInventory().getChestplate())));
        items.add(new Pair<>(EnumItemSlot.LEGS,  CraftItemStack.asNMSCopy(victim.getInventory().getLeggings())));
        items.add(new Pair<>(EnumItemSlot.FEET,  CraftItemStack.asNMSCopy(victim.getInventory().getBoots())));
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(victim.getEntityId(), items);
        sendPacket(receiver, packet);
    }

    @Override
    public void spawnDragon(Location l, ITeam bwt) {
        if (l == null || l.getWorld() == null) {
            getPlugin().getLogger().log(Level.WARNING, "Could not spawn Dragon. Location is null");
            return;
        }
        EnderDragon ed = (EnderDragon) l.getWorld().spawnEntity(l, EntityType.ENDER_DRAGON);
        ed.setPhase(EnderDragon.Phase.CIRCLING);
    }

    @Override
    public void colorBed(ITeam bwt) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState bed = bwt.getBed().clone().add(x, 0, z).getBlock().getState();
                if (bed instanceof Bed) {
                    bed.setType(bwt.getColor().bedMaterial());
                    bed.update();
                }
            }
        }
    }

    @Override
    public void registerTntWhitelist(float endStoneBlast, float glassBlast) {
        try {
            Field field = BlockBase.class.getDeclaredField("explosionResistance");
            field.setAccessible(true);

            // End stone
            field.set(Blocks.END_STONE, endStoneBlast);

            // Plain glass
            field.set(Blocks.GLASS, glassBlast);

            var coloredGlass = new net.minecraft.world.level.block.Block[]{
                    Blocks.WHITE_STAINED_GLASS,
                    Blocks.ORANGE_STAINED_GLASS,
                    Blocks.MAGENTA_STAINED_GLASS,
                    Blocks.LIGHT_BLUE_STAINED_GLASS,
                    Blocks.YELLOW_STAINED_GLASS,
                    Blocks.LIME_STAINED_GLASS,
                    Blocks.PINK_STAINED_GLASS,
                    Blocks.GRAY_STAINED_GLASS,
                    Blocks.LIGHT_GRAY_STAINED_GLASS,
                    Blocks.CYAN_STAINED_GLASS,
                    Blocks.PURPLE_STAINED_GLASS,
                    Blocks.BLUE_STAINED_GLASS,
                    Blocks.BROWN_STAINED_GLASS,
                    Blocks.GREEN_STAINED_GLASS,
                    Blocks.RED_STAINED_GLASS,
                    Blocks.BLACK_STAINED_GLASS,
                    Blocks.TINTED_GLASS,
            };

            Arrays.stream(coloredGlass).forEach(glass -> {
                try {
                    field.set(glass, glassBlast);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setBlockTeamColor(@NotNull Block block, TeamColor teamColor) {
        if (block.getType().toString().contains("STAINED_GLASS") || block.getType().toString().equals("GLASS")) {
            block.setType(teamColor.glassMaterial());
        } else if (block.getType().toString().contains("_TERRACOTTA")) {
            block.setType(teamColor.glazedTerracottaMaterial());
        } else if (block.getType().toString().contains("_WOOL")) {
            block.setType(teamColor.woolMaterial());
        }
    }

    @Override
    public void setCollide(@NotNull Player p, IArena a, boolean value) {
        p.setCollidable(value);
        if (a == null) return;
        a.updateSpectatorCollideRule(p, value);
    }

    @Override
    public org.bukkit.inventory.ItemStack addCustomData(org.bukkit.inventory.ItemStack i, String data) {
        var tag = getCreateTag(i);
        tag.putString(VersionSupport.PLUGIN_TAG_GENERIC_KEY, data);
        return applyTag(i, tag);
    }

    @Override
    public org.bukkit.inventory.ItemStack setTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        var tag = getCreateTag(itemStack);
        tag.putString(key, value);
        return applyTag(itemStack, tag);
    }

    @Override
    public boolean isCustomBedWarsItem(org.bukkit.inventory.ItemStack i) {
        return getCreateTag(i).contains(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
    }

    @Override
    public String getCustomData(org.bukkit.inventory.ItemStack i) {
        return getCreateTag(i).getString(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
    }

    @Override
    public org.bukkit.inventory.ItemStack colourItem(org.bukkit.inventory.ItemStack itemStack, ITeam bedWarsTeam) {
        if (itemStack == null) return null;
        String type = itemStack.getType().toString();
        if (isBed(itemStack.getType())) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().bedMaterial(), itemStack.getAmount());
        } else if (type.contains("_STAINED_GLASS_PANE")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassPaneMaterial(), itemStack.getAmount());
        } else if (type.contains("STAINED_GLASS") || type.equals("GLASS")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassMaterial(), itemStack.getAmount());
        } else if (type.contains("_TERRACOTTA")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glazedTerracottaMaterial(), itemStack.getAmount());
        } else if (type.contains("_WOOL")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().woolMaterial(), itemStack.getAmount());
        }
        return itemStack;
    }

    @Override
    public org.bukkit.inventory.ItemStack createItemStack(String material, int amount, short data) {
        try {
            return new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(material), amount);
        } catch (Exception ex) {
            getPlugin().getLogger().log(Level.WARNING, material + " is not a valid " + getName() + " material!");
            return new org.bukkit.inventory.ItemStack(org.bukkit.Material.BEDROCK);
        }
    }

    @Override
    public org.bukkit.Material materialFireball() {
        return org.bukkit.Material.FIRE_CHARGE;
    }

    @Override
    public org.bukkit.Material materialPlayerHead() {
        return org.bukkit.Material.PLAYER_HEAD;
    }

    @Override
    public org.bukkit.Material materialSnowball() {
        return org.bukkit.Material.SNOWBALL;
    }

    @Override
    public org.bukkit.Material materialGoldenHelmet() {
        return org.bukkit.Material.GOLDEN_HELMET;
    }

    @Override
    public org.bukkit.Material materialGoldenChestPlate() {
        return org.bukkit.Material.GOLDEN_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialGoldenLeggings() {
        return org.bukkit.Material.GOLDEN_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialNetheriteHelmet() {
        return org.bukkit.Material.NETHERITE_HELMET;
    }

    @Override
    public org.bukkit.Material materialNetheriteChestPlate() {
        return org.bukkit.Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialNetheriteLeggings() {
        return org.bukkit.Material.NETHERITE_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialElytra() {
        return org.bukkit.Material.ELYTRA;
    }

    @Override
    public org.bukkit.Material materialCake() {
        return org.bukkit.Material.CAKE;
    }

    @Override
    public org.bukkit.Material materialCraftingTable() {
        return org.bukkit.Material.CRAFTING_TABLE;
    }

    @Override
    public org.bukkit.Material materialEnchantingTable() {
        return org.bukkit.Material.ENCHANTING_TABLE;
    }

    @Override
    public org.bukkit.Material woolMaterial() {
        return org.bukkit.Material.WHITE_WOOL;
    }

    @Override
    public String getShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack) {
        var tag = getCreateTag(itemStack);
        return tag.contains(VersionSupport.PLUGIN_TAG_TIER_KEY) ? tag.getString(VersionSupport.PLUGIN_TAG_TIER_KEY) : "null";
    }

    @Override
    public org.bukkit.inventory.ItemStack setShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack,
                                                                    String identifier) {
        var tag = getCreateTag(itemStack);
        tag.putString(VersionSupport.PLUGIN_TAG_TIER_KEY, identifier);
        return applyTag(itemStack, tag);
    }

    @Override
    public org.bukkit.inventory.ItemStack getPlayerHead(Player player, org.bukkit.inventory.ItemStack copyTagFrom) {
        org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(materialPlayerHead());

        if (copyTagFrom != null) {
            var tag = getTag(copyTagFrom);
            head = applyTag(head, tag);
        }

        var meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwnerProfile(player.getPlayerProfile());
        }
        head.setItemMeta(meta);
        return head;
    }

    @Override
    public void sendPlayerSpawnPackets(Player respawned, IArena arena) {
        if (respawned == null || arena == null || !arena.isPlayer(respawned)) return;
        if (arena.getRespawnSessions().containsKey(respawned)) return;

        EntityPlayer entityPlayer = getPlayer(respawned);
        PacketPlayOutSpawnEntity show = createSpawnEntityPacket(entityPlayer);
        PacketPlayOutEntityVelocity playerVelocity =
                new PacketPlayOutEntityVelocity(entityPlayer.getId(), entityPlayer.getDeltaMovement());
        PacketPlayOutEntityHeadRotation head =
                new PacketPlayOutEntityHeadRotation(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()));

        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list = getPlayerEquipment(entityPlayer);

        for (Player p : arena.getPlayers()) {
            if (p == null || p.equals(respawned)) continue;
            if (arena.getRespawnSessions().containsKey(p)) continue;

            EntityPlayer boundTo = getPlayer(p);
            if (p.getWorld().equals(respawned.getWorld())
                    && respawned.getLocation().distance(p.getLocation()) <= arena.getRenderDistance()) {

                // Send respawned player to regular players
                this.sendPackets(p, show, head, playerVelocity,
                        new PacketPlayOutEntityEquipment(respawned.getEntityId(), list));

                // Send nearby players to respawned player
                if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    hideArmor(p, respawned);
                } else {
                    PacketPlayOutSpawnEntity show2 = createSpawnEntityPacket(boundTo);
                    PacketPlayOutEntityVelocity vel2 =
                            new PacketPlayOutEntityVelocity(boundTo.getId(), boundTo.getDeltaMovement());
                    PacketPlayOutEntityHeadRotation head2 =
                            new PacketPlayOutEntityHeadRotation(boundTo, getCompressedAngle(boundTo.getBukkitYaw()));
                    this.sendPackets(respawned, show2, vel2, head2);
                    showArmor(p, respawned);
                }
            }
        }

        for (Player spectator : arena.getSpectators()) {
            if (spectator == null || spectator.equals(respawned)) continue;
            respawned.hidePlayer(getPlugin(), spectator);
            if (spectator.getWorld().equals(respawned.getWorld())
                    && respawned.getLocation().distance(spectator.getLocation()) <= arena.getRenderDistance()) {
                this.sendPackets(spectator, show, playerVelocity,
                        new PacketPlayOutEntityEquipment(respawned.getEntityId(), list),
                        new PacketPlayOutEntityHeadRotation(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw())));
            }
        }
    }

    @Override
    public String getInventoryName(@NotNull InventoryEvent e) {
        return e.getView().getTitle();
    }

    @Override
    public void setUnbreakable(@NotNull ItemMeta itemMeta) {
        itemMeta.setUnbreakable(true);
    }

    @Override
    public String getMainLevel() {
        return ((DedicatedServer) MinecraftServer.getServer()).getProperties().levelName;
    }

    @Override
    public int getVersion() {
        return 11;
    }

    @Override
    public void setJoinSignBackground(@NotNull BlockState b, org.bukkit.Material material) {
        if (b.getBlockData() instanceof WallSign) {
            b.getBlock().getRelative(((WallSign) b.getBlockData()).getFacing().getOppositeFace()).setType(material);
        }
    }

    @Override
    public void spigotShowPlayer(Player victim, @NotNull Player receiver) {
        receiver.showPlayer(getPlugin(), victim);
    }

    @Override
    public void spigotHidePlayer(Player victim, @NotNull Player receiver) {
        receiver.hidePlayer(getPlugin(), victim);
    }

    @Override
    public org.bukkit.entity.Fireball setFireballDirection(org.bukkit.entity.Fireball fireball, @NotNull Vector vector) {
        Fireball fb = ((CraftFireball) fireball).getHandle();
        try {
            Field xField = fb.getClass().getSuperclass().getDeclaredField("xPower");
            Field yField = fb.getClass().getSuperclass().getDeclaredField("yPower");
            Field zField = fb.getClass().getSuperclass().getDeclaredField("zPower");
            xField.setAccessible(true);
            yField.setAccessible(true);
            zField.setAccessible(true);
            xField.set(fb, vector.getX() * 0.1D);
            yField.set(fb, vector.getY() * 0.1D);
            zField.set(fb, vector.getZ() * 0.1D);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (org.bukkit.entity.Fireball) fb.getBukkitEntity();
    }

    @Override
    public void playRedStoneDot(@NotNull Player player) {
        Color color = Color.RED;
        ParticleParamRedstone particleOptions = new ParticleParamRedstone(
                new Vector3f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f),
                1.0f
        );
        PacketPlayOutWorldParticles particlePacket = new PacketPlayOutWorldParticles(
                particleOptions, true,
                player.getLocation().getX(),
                player.getLocation().getY() + 2.6,
                player.getLocation().getZ(),
                0, 0, 0, 0, 0
        );
        for (Player inWorld : player.getWorld().getPlayers()) {
            if (inWorld.equals(player)) continue;
            this.sendPacket(inWorld, particlePacket);
        }
    }

    @Override
    public void clearArrowsFromPlayerBody(Player player) {
        // Minecraft clears arrows on death in newer versions
    }

    @Override
    public void placeTowerBlocks(@NotNull Block b, @NotNull IArena a, @NotNull TeamColor color, int x, int y, int z) {
        b.getRelative(x, y, z).setType(color.woolMaterial());
        a.addPlacedBlock(b.getRelative(x, y, z));
    }

    @Override
    public void placeLadder(@NotNull Block b, int x, int y, int z, @NotNull IArena a, int ladderData) {
        Block block = b.getRelative(x, y, z);
        block.setType(Material.LADDER);
        Ladder ladder = (Ladder) block.getBlockData();
        a.addPlacedBlock(block);
        switch (ladderData) {
            case 2 -> { ladder.setFacing(BlockFace.NORTH); block.setBlockData(ladder); }
            case 3 -> { ladder.setFacing(BlockFace.SOUTH); block.setBlockData(ladder); }
            case 4 -> { ladder.setFacing(BlockFace.WEST);  block.setBlockData(ladder); }
            case 5 -> { ladder.setFacing(BlockFace.EAST);  block.setBlockData(ladder); }
        }
    }

    @Override
    public void playVillagerEffect(@NotNull Player player, Location location) {
        // Renamed from VILLAGER_HAPPY to HAPPY_VILLAGER in Paper 1.20.5+
        player.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Gets the NMS {@link net.minecraft.world.item.Item} from a Bukkit {@link org.bukkit.inventory.ItemStack}. */
    private @Nullable net.minecraft.world.item.Item getItem(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        return i == null ? null : i.getItem();
    }

    /**
     * Reads the custom-data {@link NBTTagCompound} for a Bukkit {@link org.bukkit.inventory.ItemStack}.
     * <p>
     * Since 1.20.5+ the monolithic item NBT tag was replaced with a DataComponent system.
     * Plugin-specific data lives in {@link DataComponents#CUSTOM_DATA}.
     */
    private @Nullable NBTTagCompound getTag(@NotNull org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (i == null) return null;
        return getTag(i);
    }

    private @Nullable NBTTagCompound getTag(@NotNull net.minecraft.world.item.ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    private @NotNull NBTTagCompound initializeTag(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (i == null) throw new RuntimeException("Cannot convert given item to a NMS item");
        return initializeTag(i);
    }

    private @NotNull NBTTagCompound initializeTag(net.minecraft.world.item.ItemStack itemStack) {
        var tag = getTag(itemStack);
        if (tag != null) throw new RuntimeException("Provided item already has a Tag");
        tag = new NBTTagCompound();
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return tag;
    }

    public @NotNull NBTTagCompound getCreateTag(net.minecraft.world.item.ItemStack itemStack) {
        var tag = getTag(itemStack);
        return tag == null ? initializeTag(itemStack) : tag;
    }

    public @NotNull NBTTagCompound getCreateTag(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (i == null) throw new RuntimeException("Cannot convert given item to a NMS item");
        return getCreateTag(i);
    }

    public org.bukkit.inventory.ItemStack applyTag(org.bukkit.inventory.ItemStack itemStack,
                                                    @Nullable NBTTagCompound tag) {
        return CraftItemStack.asBukkitCopy(applyTag(getNmsItemCopy(itemStack), tag));
    }

    public net.minecraft.world.item.ItemStack applyTag(@NotNull net.minecraft.world.item.ItemStack itemStack,
                                                       @Nullable NBTTagCompound tag) {
        if (tag == null || tag.isEmpty()) {
            itemStack.remove(DataComponents.CUSTOM_DATA);
        } else {
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return itemStack;
    }

    public net.minecraft.world.item.ItemStack getNmsItemCopy(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (i == null) throw new RuntimeException("Cannot convert given item to a NMS item");
        return i;
    }

    public EntityPlayer getPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(@NotNull Player player) {
        return getPlayerEquipment(getPlayer(player));
    }

    public List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(
            @NotNull EntityPlayer entityPlayer) {
        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EnumItemSlot.MAINHAND, entityPlayer.getItemBySlot(EnumItemSlot.MAINHAND)));
        list.add(new Pair<>(EnumItemSlot.OFFHAND,  entityPlayer.getItemBySlot(EnumItemSlot.OFFHAND)));
        list.add(new Pair<>(EnumItemSlot.HEAD,     entityPlayer.getItemBySlot(EnumItemSlot.HEAD)));
        list.add(new Pair<>(EnumItemSlot.CHEST,    entityPlayer.getItemBySlot(EnumItemSlot.CHEST)));
        list.add(new Pair<>(EnumItemSlot.LEGS,     entityPlayer.getItemBySlot(EnumItemSlot.LEGS)));
        list.add(new Pair<>(EnumItemSlot.FEET,     entityPlayer.getItemBySlot(EnumItemSlot.FEET)));
        return list;
    }

    /** Creates a {@link PacketPlayOutSpawnEntity} for any entity (including players). */
    private static PacketPlayOutSpawnEntity createSpawnEntityPacket(
            @NotNull net.minecraft.world.entity.Entity entity) {
        return new PacketPlayOutSpawnEntity(
                entity.getId(),
                entity.getUUID(),
                entity.getX(), entity.getY(), entity.getZ(),
                entity.getXRot(), entity.getYRot(),
                entity.getType(),
                0,
                entity.getDeltaMovement(),
                entity.getYHeadRot()
        );
    }

    private void sendPacket(Player player, Packet<?> packet) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(packet);
    }

    private void sendPackets(Player player, Packet<?> @NotNull ... packets) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().connection;
        for (Packet<?> p : packets) {
            connection.send(p);
        }
    }
}
