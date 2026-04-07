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
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.*;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class v1_21_R7 extends VersionSupport {

    private final DespawnableFactory despawnableFactory;

    /** Cached reflection field for the player network connection. */
    private static volatile Field PLAYER_CONNECTION_FIELD;
    /** Cached reflection method for sending a packet via the connection. */
    private static volatile Method SEND_PACKET_METHOD;

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
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(makeKey(key), PersistentDataType.STRING);
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
        String name = itemStack.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || itemStack.getType() == org.bukkit.Material.ELYTRA
                || itemStack.getType() == org.bukkit.Material.TURTLE_HELMET;
    }

    @Override
    public boolean isTool(org.bukkit.inventory.ItemStack itemStack) {
        String name = itemStack.getType().name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
    }

    @Override
    public boolean isSword(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack.getType().name().endsWith("_SWORD");
    }

    @Override
    public boolean isAxe(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack.getType().name().endsWith("_AXE");
    }

    @Override
    public boolean isBow(org.bukkit.inventory.ItemStack itemStack) {
        return itemStack.getType() == org.bukkit.Material.BOW;
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
        ItemMeta meta = i.getItemMeta();
        if (meta == null) throw new RuntimeException("Provided item has no metadata");
        Double val = meta.getPersistentDataContainer().get(makeKey("generic.attackDamage"), PersistentDataType.DOUBLE);
        if (val == null) throw new RuntimeException("Provided item has no damage tag");
        return val;
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
        player.hurt(player.level().damageSources().outOfWorld(), Float.MAX_VALUE);
    }

    @Override
    public void hideArmor(@NotNull Player victim, Player receiver) {
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> items = new ArrayList<>();
        net.minecraft.world.item.ItemStack empty = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AIR);
        items.add(new Pair<>(EquipmentSlot.HEAD,  empty));
        items.add(new Pair<>(EquipmentSlot.CHEST, empty));
        items.add(new Pair<>(EquipmentSlot.LEGS,  empty));
        items.add(new Pair<>(EquipmentSlot.FEET,  empty));
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(victim.getEntityId(), items);
        sendPacket(receiver, packet);
    }

    @Override
    public void showArmor(@NotNull Player victim, Player receiver) {
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> items = new ArrayList<>();
        items.add(new Pair<>(EquipmentSlot.HEAD,  CraftItemStack.asNMSCopy(victim.getInventory().getHelmet())));
        items.add(new Pair<>(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(victim.getInventory().getChestplate())));
        items.add(new Pair<>(EquipmentSlot.LEGS,  CraftItemStack.asNMSCopy(victim.getInventory().getLeggings())));
        items.add(new Pair<>(EquipmentSlot.FEET,  CraftItemStack.asNMSCopy(victim.getInventory().getBoots())));
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
            // Obtain the explosionResistance field from the block's superclass
            // (BlockBehaviour/BlockBase – name varies across NMS versions).
            Class<?> blockSuperClass = net.minecraft.world.level.block.Block.class.getSuperclass();
            Field field = null;
            // Attempt known field names in priority order:
            // "explosionResistance" = Mojang-mapped name (1.21.5+);
            // "bl", "bm", "bn" = common obfuscated names from older Spigot builds.
            for (String fieldName : new String[]{"explosionResistance", "bl", "bm", "bn"}) {
                try {
                    field = blockSuperClass.getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
            if (field == null) {
                getPlugin().getLogger().warning("registerTntWhitelist: could not find explosionResistance field; skipped.");
                return;
            }
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

            for (net.minecraft.world.level.block.Block glass : coloredGlass) {
                field.set(glass, glassBlast);
            }
        } catch (IllegalAccessException e) {
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
        ItemMeta meta = i.getItemMeta();
        if (meta == null) return i;
        meta.getPersistentDataContainer().set(makeKey(VersionSupport.PLUGIN_TAG_GENERIC_KEY), PersistentDataType.STRING, data);
        i.setItemMeta(meta);
        return i;
    }

    @Override
    public org.bukkit.inventory.ItemStack setTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        meta.getPersistentDataContainer().set(makeKey(key), PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public boolean isCustomBedWarsItem(org.bukkit.inventory.ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(makeKey(VersionSupport.PLUGIN_TAG_GENERIC_KEY));
    }

    @Override
    public String getCustomData(org.bukkit.inventory.ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(makeKey(VersionSupport.PLUGIN_TAG_GENERIC_KEY), PersistentDataType.STRING);
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
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return "null";
        String val = meta.getPersistentDataContainer().get(makeKey(VersionSupport.PLUGIN_TAG_TIER_KEY), PersistentDataType.STRING);
        return val != null ? val : "null";
    }

    @Override
    public org.bukkit.inventory.ItemStack setShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack,
                                                                    String identifier) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        meta.getPersistentDataContainer().set(makeKey(VersionSupport.PLUGIN_TAG_TIER_KEY), PersistentDataType.STRING, identifier);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public org.bukkit.inventory.ItemStack getPlayerHead(Player player, org.bukkit.inventory.ItemStack copyTagFrom) {
        org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(materialPlayerHead());

        if (copyTagFrom != null) {
            ItemMeta fromMeta = copyTagFrom.getItemMeta();
            ItemMeta headMeta = head.getItemMeta();
            if (fromMeta != null && headMeta != null) {
                // Copy all known BedWars PDC string keys from source item to head
                var fromPDC = fromMeta.getPersistentDataContainer();
                var headPDC = headMeta.getPersistentDataContainer();
                for (NamespacedKey key : fromPDC.getKeys()) {
                    String strVal = fromPDC.get(key, PersistentDataType.STRING);
                    if (strVal != null) {
                        headPDC.set(key, PersistentDataType.STRING, strVal);
                        continue;
                    }
                    Double dblVal = fromPDC.get(key, PersistentDataType.DOUBLE);
                    if (dblVal != null) headPDC.set(key, PersistentDataType.DOUBLE, dblVal);
                }
                head.setItemMeta(headMeta);
            }
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
        Packet<?> show = createSpawnEntityPacket(entityPlayer);
        PacketPlayOutEntityVelocity playerVelocity = new PacketPlayOutEntityVelocity(entityPlayer);
        PacketPlayOutEntityHeadRotation head =
                new PacketPlayOutEntityHeadRotation(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()));

        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = getPlayerEquipment(entityPlayer);

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
                    Packet<?> show2 = createSpawnEntityPacket(boundTo);
                    PacketPlayOutEntityVelocity vel2 = new PacketPlayOutEntityVelocity(boundTo);
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
        // Bukkit API: the first world is the main level
        List<org.bukkit.World> worlds = getPlugin().getServer().getWorlds();
        return worlds.isEmpty() ? "world" : worlds.get(0).getName();
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
        var fb = ((CraftFireball) fireball).getHandle();
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
        // Pack RGB into a single 24-bit int: red in bits 16-23, green in bits 8-15, blue in bits 0-7.
        int packedRgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        ParticleParamRedstone particleOptions = new ParticleParamRedstone(packedRgb, 1.0f);
        // PacketPlayOutWorldParticles: T, override, longCoords, x, y, z, offsetX, offsetY, offsetZ, speed, count
        PacketPlayOutWorldParticles particlePacket = new PacketPlayOutWorldParticles(
                particleOptions, true, false,
                player.getLocation().getX(),
                player.getLocation().getY() + 2.6,
                player.getLocation().getZ(),
                0.0f, 0.0f, 0.0f, 0.0f, 1
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

    /**
     * Creates a {@link NamespacedKey} for PersistentDataContainer storage.
     * <p>
     * Keys are normalised to lowercase (NamespacedKey requirement) with any characters
     * outside {@code [a-z0-9_\-./ ]} replaced by {@code '_'} so that arbitrary plugin
     * tag names (e.g. "generic.attackDamage") remain valid NamespacedKey values.
     */
    private NamespacedKey makeKey(String key) {
        String safeKey = key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-./]", "_");
        return new NamespacedKey(getPlugin(), safeKey);
    }

    public EntityPlayer getPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(@NotNull Player player) {
        return getPlayerEquipment(getPlayer(player));
    }

    public List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(
            @NotNull EntityPlayer entityPlayer) {
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EquipmentSlot.MAIN_HAND, entityPlayer.getItemBySlot(EquipmentSlot.MAIN_HAND)));
        list.add(new Pair<>(EquipmentSlot.OFF_HAND,  entityPlayer.getItemBySlot(EquipmentSlot.OFF_HAND)));
        list.add(new Pair<>(EquipmentSlot.HEAD,     entityPlayer.getItemBySlot(EquipmentSlot.HEAD)));
        list.add(new Pair<>(EquipmentSlot.CHEST,    entityPlayer.getItemBySlot(EquipmentSlot.CHEST)));
        list.add(new Pair<>(EquipmentSlot.LEGS,     entityPlayer.getItemBySlot(EquipmentSlot.LEGS)));
        list.add(new Pair<>(EquipmentSlot.FEET,     entityPlayer.getItemBySlot(EquipmentSlot.FEET)));
        return list;
    }

    /** Creates an add-entity packet for any entity (including players). */
    private static Packet<?> createSpawnEntityPacket(@NotNull net.minecraft.world.entity.Entity entity) {
        return new PacketPlayOutSpawnEntity(entity);
    }

    private void sendPacket(Player player, Packet<?> packet) {
        try {
            Object handle = ((CraftPlayer) player).getHandle();
            ensureConnectionReflection(handle);
            if (PLAYER_CONNECTION_FIELD != null && SEND_PACKET_METHOD != null) {
                Object conn = PLAYER_CONNECTION_FIELD.get(handle);
                SEND_PACKET_METHOD.invoke(conn, packet);
            }
        } catch (Exception e) {
            getPlugin().getLogger().log(Level.WARNING, "Failed to send packet to " + player.getName(), e);
        }
    }

    private void sendPackets(Player player, Packet<?> @NotNull ... packets) {
        try {
            Object handle = ((CraftPlayer) player).getHandle();
            ensureConnectionReflection(handle);
            if (PLAYER_CONNECTION_FIELD != null && SEND_PACKET_METHOD != null) {
                Object conn = PLAYER_CONNECTION_FIELD.get(handle);
                for (Packet<?> p : packets) {
                    SEND_PACKET_METHOD.invoke(conn, p);
                }
            }
        } catch (Exception e) {
            getPlugin().getLogger().log(Level.WARNING, "Failed to send packets to " + player.getName(), e);
        }
    }

    /**
     * Initialises the cached reflection handles for the player network connection
     * and its packet-send method.  Called once; subsequent calls are no-ops.
     */
    private static synchronized void ensureConnectionReflection(Object playerHandle) throws Exception {
        if (PLAYER_CONNECTION_FIELD != null) return;

        // Walk the class hierarchy to find the field that holds the network connection.
        // Under Mojang mapping it is named "connection"; obfuscated builds use shorter names.
        Class<?> cls = playerHandle.getClass();
        outer:
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String typeName = f.getType().getSimpleName();
                // Matches PlayerConnection, ServerGamePacketListenerImpl, NetworkHandler, etc.
                if (typeName.contains("Connection") || typeName.contains("PacketListener")
                        || typeName.contains("NetworkHandler")) {
                    f.setAccessible(true);
                    Object conn = f.get(playerHandle);
                    if (conn == null) continue;
                    // Find the send/dispatch method on that connection object.
                    for (Method m : conn.getClass().getMethods()) {
                        if (m.getParameterCount() != 1) continue;
                        Class<?> paramType = m.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(Packet.class) || Packet.class.isAssignableFrom(paramType)) {
                            PLAYER_CONNECTION_FIELD = f;
                            SEND_PACKET_METHOD = m;
                            break outer;
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }
    }
}
