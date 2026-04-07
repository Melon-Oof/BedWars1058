package com.andrei1058.bedwars.support.version.v1_21_R7.despawnable;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;

public abstract class DespawnableProvider<T> {

    private static final Field GOAL_SELECTOR_FIELD;
    private static final Field TARGET_SELECTOR_FIELD;

    static {
        try {
            GOAL_SELECTOR_FIELD = EntityInsentient.class.getDeclaredField("goalSelector");
            GOAL_SELECTOR_FIELD.setAccessible(true);
            TARGET_SELECTOR_FIELD = EntityInsentient.class.getDeclaredField("targetSelector");
            TARGET_SELECTOR_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access PathfinderGoalSelector fields on EntityInsentient", e);
        }
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
        getGoalSelector(entity).removeAllGoals(g -> true);
        getTargetSelector(entity).removeAllGoals(g -> true);
    }

    protected PathfinderGoal getTargetGoal(EntityInsentient entity, ITeam team, VersionSupport api) {
        return new PathfinderGoalNearestAttackableTarget<EntityHuman>(entity, EntityHuman.class, 20, true, false,
                (EntityHuman nmsPlayer) -> !nmsPlayer.getBukkitEntity().isDead()
                        && !team.wasMember(nmsPlayer.getBukkitEntity().getUniqueId())
                        && !team.getArena().isReSpawning(nmsPlayer.getBukkitEntity().getUniqueId())
                        && !team.getArena().isSpectator(nmsPlayer.getBukkitEntity().getUniqueId()));
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
