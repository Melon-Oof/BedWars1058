package com.andrei1058.bedwars.support.version.v1_21_R7.despawnable;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
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

import java.util.Objects;

public abstract class DespawnableProvider<T> {

    abstract DespawnableType getType();

    abstract String getDisplayName(DespawnableAttributes attr, ITeam team);

    abstract T spawn(@NotNull DespawnableAttributes attr, @NotNull Location location, @NotNull ITeam team,
                     VersionSupport api);

    protected boolean notSameTeam(@NotNull Entity entity, ITeam team, @NotNull VersionSupport api) {
        var despawnable = api.getDespawnablesList().getOrDefault(entity.getBukkitEntity().getUniqueId(), null);
        return null == despawnable || despawnable.getTeam() != team;
    }

    protected PathfinderGoalSelector getTargetSelector(@NotNull EntityCreature entity) {
        return entity.targetSelector;
    }

    protected PathfinderGoalSelector getGoalSelector(@NotNull EntityCreature entity) {
        return entity.goalSelector;
    }

    protected void clearSelectors(@NotNull EntityCreature entity) {
        entity.goalSelector.removeAllGoals(g -> true);
        entity.targetSelector.removeAllGoals(g -> true);
    }

    protected PathfinderGoal getTargetGoal(EntityInsentient entity, ITeam team, VersionSupport api) {
        return new PathfinderGoalNearestAttackableTarget<>(entity, EntityHuman.class, 20, true, false,
                (EntityLiving livingEntity) -> {
                    if (livingEntity instanceof EntityHuman nmsPlayer) {
                        return !nmsPlayer.getBukkitEntity().isDead()
                                && !team.wasMember(nmsPlayer.getBukkitEntity().getUniqueId())
                                && !team.getArena().isReSpawning(nmsPlayer.getBukkitEntity().getUniqueId())
                                && !team.getArena().isSpectator(nmsPlayer.getBukkitEntity().getUniqueId());
                    }
                    return notSameTeam(livingEntity, team, api);
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
