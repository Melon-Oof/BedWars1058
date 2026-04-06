package com.andrei1058.bedwars.support.version.v1_21_R7.despawnable;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeamIronGolem extends DespawnableProvider<org.bukkit.entity.IronGolem> {

    @Override
    public DespawnableType getType() {
        return DespawnableType.IRON_GOLEM;
    }

    @Override
    String getDisplayName(@NotNull DespawnableAttributes attr, @NotNull ITeam team) {
        Language lang = Language.getDefaultLanguage();
        return lang.m(Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME)
                .replace("{despawn}", String.valueOf(attr.despawnSeconds()))
                .replace("{health}", StringUtils.repeat(lang.m(Messages.FORMATTING_DESPAWNABLE_UTILITY_NPC_HEALTH) + " ", 10))
                .replace("{TeamColor}", team.getColor().chat().toString());
    }

    @Override
    public org.bukkit.entity.@NotNull IronGolem spawn(@NotNull DespawnableAttributes attr,
                                                       @NotNull Location location,
                                                       @NotNull ITeam team,
                                                       VersionSupport api) {
        var bukkitEntity = (org.bukkit.entity.IronGolem) Objects.requireNonNull(location.getWorld())
                .spawnEntity(location, EntityType.IRON_GOLEM);
        applyDefaultSettings(bukkitEntity, attr, team);

        var entity = (EntityIronGolem) ((CraftEntity) bukkitEntity).getHandle();

        clearSelectors(entity);
        var goalSelector = getGoalSelector(entity);
        var targetSelector = getTargetSelector(entity);

        goalSelector.addGoal(1, new PathfinderGoalFloat(entity));
        goalSelector.addGoal(2, new PathfinderGoalMeleeAttack(entity, 1.5D, false));
        goalSelector.addGoal(3, new PathfinderGoalRandomStroll(entity, 1D));
        goalSelector.addGoal(4, new PathfinderGoalRandomLookaround(entity));
        targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(entity));
        targetSelector.addGoal(2, getTargetGoal(entity, team, api));

        return bukkitEntity;
    }
}
