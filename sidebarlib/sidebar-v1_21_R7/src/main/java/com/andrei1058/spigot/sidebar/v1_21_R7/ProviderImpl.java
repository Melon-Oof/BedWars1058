package com.andrei1058.spigot.sidebar.v1_21_R7;

import com.andrei1058.spigot.sidebar.*;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

@SuppressWarnings("unused")
public class ProviderImpl extends SidebarProvider {
    private static SidebarProvider instance;

    private static volatile IScoreboardCriteria dummyCriteria;
    private static volatile IScoreboardCriteria healthCriteria;

    /**
     * Finds IScoreboardCriteria instances via reflection since the obfuscated field
     * names (b/f) changed to Codec types in MC 1.21.11.
     */
    private static synchronized void ensureCriteria() {
        if (dummyCriteria != null && healthCriteria != null) return;
        IScoreboardCriteria first = null, last = null;
        for (Field field : IScoreboardCriteria.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!IScoreboardCriteria.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                IScoreboardCriteria crit = (IScoreboardCriteria) field.get(null);
                if (crit == null) continue;
                if (first == null) first = crit;
                last = crit;
                // Health criterion is read-only; detect via any no-arg boolean method returning true
                if (isReadOnlyCriteria(crit)) {
                    healthCriteria = crit;
                } else if (dummyCriteria == null) {
                    dummyCriteria = crit;
                }
            } catch (Exception ignored) {
            }
        }
        // Fallback: use first=dummy, last=health if detection failed
        if (dummyCriteria == null) dummyCriteria = first;
        if (healthCriteria == null) healthCriteria = last != null ? last : first;
    }

    private static boolean isReadOnlyCriteria(IScoreboardCriteria crit) {
        for (java.lang.reflect.Method m : crit.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != boolean.class) continue;
            try {
                Boolean result = (Boolean) m.invoke(crit);
                if (Boolean.TRUE.equals(result)) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public Sidebar createSidebar(SidebarLine title, Collection<SidebarLine> lines, Collection<PlaceholderProvider> placeholderProviders) {
        return new SidebarImpl(title, lines, placeholderProviders);
    }

    @Override
    public SidebarObjective createObjective(@NotNull WrappedSidebar sidebar, String name, boolean health, SidebarLine title, int type) {
        ensureCriteria();
        IScoreboardCriteria criteria = health ? healthCriteria : dummyCriteria;
        return ((SidebarImpl) sidebar).createObjective(name, criteria, title, type);
    }

    @Override
    public ScoreLine createScoreLine(WrappedSidebar sidebar, SidebarLine line, int score, String color) {
        return ((SidebarImpl)sidebar).createScore(line, score, color);
    }

    @Override
    public void sendScore(@NotNull WrappedSidebar sidebar, String playerName, int score) {
        if (sidebar.getHealthObjective() == null) return;
        PacketPlayOutScoreboardScore packetPlayOutScoreboardScore = new PacketPlayOutScoreboardScore(
                playerName,
                sidebar.getHealthObjective().getName(),
                score,
                Optional.empty(),
                Optional.empty()
        );
        for (Player player : sidebar.getReceivers()) {
            PacketSender.send(player, packetPlayOutScoreboardScore);
        }
    }

    @Override
    public VersionedTabGroup createPlayerTab(WrappedSidebar sidebar, String identifier, SidebarLine prefix, SidebarLine suffix, PlayerTab.PushingRule pushingRule, PlayerTab.NameTagVisibility nameTagVisibility, @Nullable Collection<PlaceholderProvider> placeholders) {
        return new PlayerListImpl(sidebar, identifier, prefix, suffix, pushingRule, nameTagVisibility, placeholders);
    }

    @Override
    public void sendHeaderFooter(Player player, String header, String footer) {
        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(IChatBaseComponent.b(header), IChatBaseComponent.b(footer));
        PacketSender.send(player, packet);
    }

    public static SidebarProvider getInstance() {
        return null == instance ? instance = new ProviderImpl() : instance;
    }
}
