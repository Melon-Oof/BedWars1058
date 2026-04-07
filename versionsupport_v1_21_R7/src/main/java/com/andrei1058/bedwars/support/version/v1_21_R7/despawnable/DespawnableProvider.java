package com.andrei1058.bedwars.support.version.v1_21_R7.despawnable;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class DespawnableProvider<T> {

    private static final Field GOAL_SELECTOR_FIELD;
    private static final Field TARGET_SELECTOR_FIELD;

    static {
        // Locate the goalSelector and targetSelector fields by type rather than
        // by name so that obfuscated Spigot builds (where the fields are named
        // with single letters) are handled transparently.
        List<Field> selectorFields = new ArrayList<>();
        for (Field f : EntityInsentient.class.getDeclaredFields()) {
            if (PathfinderGoalSelector.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                selectorFields.add(f);
            }
        }
        if (selectorFields.size() < 2) {
            throw new RuntimeException(
                    "Failed to find goal/target selector fields on EntityInsentient (found "
                            + selectorFields.size() + ")");
        }
        GOAL_SELECTOR_FIELD   = selectorFields.get(0);
        TARGET_SELECTOR_FIELD = selectorFields.get(1);
    }

    abstract DespawnableType getType();

    abstract String getDisplayName(DespawnableAttributes attr, ITeam team);

    abstract T spawn(@NotNull DespawnableAttributes attr, @NotNull Location location, @NotNull ITeam team,
                     VersionSupport api);

    protected boolean notSameTeam(@NotNull Entity entity, ITeam team, @NotNull VersionSupport api) {
        var despawnable = api.getDespawnablesList().getOrDefault(entity.getBukkitEntity().getUniqueId(), null);
        return null == despawnable || despawnable.getTeam() != team;
    }

    protected PathfinderGoalSelector getTargetSelector(@NotNull EntityInsentient entity) {
        try {
            return (PathfinderGoalSelector) TARGET_SELECTOR_FIELD.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get targetSelector", e);
        }
    }

    protected PathfinderGoalSelector getGoalSelector(@NotNull EntityInsentient entity) {
        try {
            return (PathfinderGoalSelector) GOAL_SELECTOR_FIELD.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get goalSelector", e);
        }
    }

    protected void clearSelectors(@NotNull EntityInsentient entity) {
        clearGoalSelector(getGoalSelector(entity));
        clearGoalSelector(getTargetSelector(entity));
    }

    private void clearGoalSelector(PathfinderGoalSelector selector) {
        // Try the Mojang-mapped method first; if absent (obfuscated build) fall
        // back to clearing every Set field found on the selector.
        try {
            selector.getClass()
                    .getMethod("removeAllGoals", java.util.function.Predicate.class)
                    .invoke(selector, (java.util.function.Predicate<Object>) o -> true);
            return;
        } catch (Exception ignored) {}
        for (Field f : selector.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object val = f.get(selector);
                if (val instanceof Set) {
                    ((Set<?>) val).clear();
                }
            } catch (Exception ignored) {}
        }
    }

    protected PathfinderGoal getTargetGoal(EntityInsentient entity, ITeam team, VersionSupport api) {
        return new PathfinderGoalNearestAttackableTarget<>(entity, EntityLiving.class, 20, true, false,
                entityLiving -> {
                    if (entityLiving instanceof EntityHuman) {
                        return !((EntityHuman) entityLiving).getBukkitEntity().isDead()
                                && !team.wasMember(((EntityHuman) entityLiving).getBukkitEntity().getUniqueId())
                                && !team.getArena().isReSpawning(((EntityHuman) entityLiving).getBukkitEntity().getUniqueId())
                                && !team.getArena().isSpectator(((EntityHuman) entityLiving).getBukkitEntity().getUniqueId());
                    }
                    return notSameTeam(entityLiving, team, api);
                });
    }

    protected void applyDefaultSettings(org.bukkit.entity.@NotNull LivingEntity bukkitEntity,
                                        DespawnableAttributes attr, ITeam team) {
        bukkitEntity.setRemoveWhenFarAway(false);
        bukkitEntity.setPersistent(true);
        bukkitEntity.setCustomNameVisible(true);
        bukkitEntity.setCustomName(getDisplayName(attr, team));

        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(attr.health());
        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(attr.speed());
        Objects.requireNonNull(bukkitEntity.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(attr.damage());
    }
}
