package com.andrei1058.bedwars.support.version.v1_21_R7.despawnable;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.VersionSupport;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeamSilverfish extends DespawnableProvider<org.bukkit.entity.Silverfish> {

    @Override
    public DespawnableType getType() {
        return DespawnableType.SILVERFISH;
    }

    @Override
    String getDisplayName(@NotNull DespawnableAttributes attr, @NotNull ITeam team) {
        Language lang = Language.getDefaultLanguage();
        return lang.m(Messages.SHOP_UTILITY_NPC_SILVERFISH_NAME)
                .replace("{despawn}", String.valueOf(attr.despawnSeconds()))
                .replace("{health}", StringUtils.repeat(lang.m(Messages.FORMATTING_DESPAWNABLE_UTILITY_NPC_HEALTH) + " ", 10))
                .replace("{TeamColor}", team.getColor().chat().toString());
    }

    @Override
    public org.bukkit.entity.@NotNull Silverfish spawn(@NotNull DespawnableAttributes attr,
                                                        @NotNull Location location,
                                                        @NotNull ITeam team,
                                                        VersionSupport api) {
        var bukkitEntity = (org.bukkit.entity.Silverfish) Objects.requireNonNull(location.getWorld())
                .spawnEntity(location, EntityType.SILVERFISH);
        applyDefaultSettings(bukkitEntity, attr, team);

        var entity = (Silverfish) ((CraftEntity) bukkitEntity).getHandle();
        clearSelectors(entity);

        var goalSelector = getGoalSelector(entity);
        var targetSelector = getTargetSelector(entity);
        goalSelector.addGoal(1, new FloatGoal(entity));
        goalSelector.addGoal(2, new MeleeAttackGoal(entity, 1.9D, false));
        goalSelector.addGoal(3, new RandomStrollGoal(entity, 2D));
        goalSelector.addGoal(4, new RandomLookAroundGoal(entity));
        targetSelector.addGoal(1, new HurtByTargetGoal(entity));
        targetSelector.addGoal(2, getTargetGoal(entity, team, api));

        return bukkitEntity;
    }
}
