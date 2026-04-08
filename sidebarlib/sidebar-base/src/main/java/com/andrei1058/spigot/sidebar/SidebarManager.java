package com.andrei1058.spigot.sidebar;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class SidebarManager {


    private static SidebarManager instance;
    private SidebarProvider sidebarProvider;
    private PAPISupport papiSupport = new PAPISupport() {
        @Override
        public String replacePlaceholders(Player p, String s) {
            return s;
        }

        @Override
        public boolean hasPlaceholders(String s) {
            return false;
        }
    };

    public SidebarManager(
            SidebarProvider provider,
            PAPISupport papiSupport
    ) {
        this.sidebarProvider = provider;
        this.papiSupport = papiSupport;
    }

    public SidebarManager() throws InstantiationException {
        instance = this;

        // PAPI hook
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            papiSupport = new PAPIAdapter();
        } catch (ClassNotFoundException ignored) {
        }

        // load server version support
        // On Spigot / older Paper: org.bukkit.craftbukkit.v1_21_R7.CraftServer → parts[3] = "v1_21_R7"
        // On Paper 1.20.5+: org.bukkit.craftbukkit.CraftServer → parts[3] = "CraftServer", so fall back
        // to deriving the NMS revision from the Bukkit API version string.
        String[] parts = Bukkit.getServer().getClass().getName().split("\\.");
        String serverVersion = (parts.length > 3 && parts[3].startsWith("v"))
                ? parts[3]
                : mapBukkitVersionToNms(Bukkit.getBukkitVersion());

        String className = "com.andrei1058.spigot.sidebar." + serverVersion + ".ProviderImpl";
        try {
            Class<?> c = Class.forName(className);
            sidebarProvider = (SidebarProvider) c.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException ignored) {
            throw new InstantiationException();
        }
    }

    /**
     * Initialize sidebar manager.
     * This will detect your server version.
     */
    @SuppressWarnings("unused")
    @Nullable
    public static SidebarManager init() {

        if (null != instance) {
            return instance;
        }

        try {
            instance = new SidebarManager();
        } catch (InstantiationException e) {
            return null;
        }

        return instance;
    }

    /**
     * Create a new sidebar.
     *
     * @param title                scoreboard title.
     * @param lines                scoreboard lines.
     * @param placeholderProviders placeholders.
     * @return sb instance.
     */
    @SuppressWarnings("unused")
    public Sidebar createSidebar(
            SidebarLine title,
            @NotNull Collection<SidebarLine> lines,
            Collection<PlaceholderProvider> placeholderProviders) {
        lines.forEach(sidebarLine -> SidebarLine.markHasPlaceholders(sidebarLine, placeholderProviders));
        return sidebarProvider.createSidebar(title, lines, placeholderProviders);
    }

    /**
     * Set a user header and footer in TAB.
     *
     * @param player receiver.
     * @param header header text.
     * @param footer footer text.
     */
    @SuppressWarnings("unused")
    public void sendHeaderFooter(Player player, String header, String footer) {
        this.sidebarProvider.sendHeaderFooter(player, header, footer);
    }

    @SuppressWarnings("unused")
    public void sendHeaderFooter(Player player, TabHeaderFooter headerFooter) {
        this.sendHeaderFooter(
                player,
                buildTabContent(player, headerFooter.getHeader(), headerFooter),
                buildTabContent(player, headerFooter.getFooter(), headerFooter)
        );
    }


    @Contract(pure = true)
    private String buildTabContent(Player player, @NotNull List<SidebarLine> lines, TabHeaderFooter headerFooter) {
        String[] data = new String[lines.size()];

        for (int i = 0; i < data.length; i++) {
            SidebarLine line = lines.get(i);
            String currentLine = line.getLine();
            if (line.isInternalPlaceholders()) {
                for (PlaceholderProvider placeholderProvider : headerFooter.getPlaceholders()) {
                    currentLine = currentLine.replace(placeholderProvider.getPlaceholder(), placeholderProvider.getReplacement());
                }
            }
            if (line.isPapiPlaceholders()) {
                currentLine = ChatColor.translateAlternateColorCodes(
                        '&', SidebarManager.getInstance().getPapiSupport().replacePlaceholders(player, currentLine)
                );
            }
            data[i] = currentLine;
        }

        return StringUtils.join(data, "\n");
    }

    public PAPISupport getPapiSupport() {
        return papiSupport;
    }

    public void setPapiSupport(PAPISupport papiSupport) {
        this.papiSupport = papiSupport;
    }

    public SidebarProvider getSidebarProvider() {
        return sidebarProvider;
    }

    public void setSidebarProvider(SidebarProvider sidebarProvider) {
        this.sidebarProvider = sidebarProvider;
    }

    public static SidebarManager getInstance() {
        return instance;
    }

    public static void setInstance(SidebarManager instance) {
        SidebarManager.instance = instance;
    }

    /**
     * Maps a Bukkit version string such as {@code "1.21.11-R0.1-SNAPSHOT"} to
     * the corresponding NMS package revision string, e.g. {@code "v1_21_R7"}.
     * Used on Paper 1.20.5+ where CraftBukkit packages no longer carry a version segment.
     */
    static String mapBukkitVersionToNms(String bukkitVersion) {
        String mcVersion = bukkitVersion.split("-")[0];
        String[] mc = mcVersion.split("\\.");
        int minor = mc.length > 1 ? Integer.parseInt(mc[1]) : 0;
        int patch  = mc.length > 2 ? Integer.parseInt(mc[2]) : 0;

        if (minor == 21) {
            if (patch >= 11) return "v1_21_R7";
            if (patch >= 9)  return "v1_21_R6";
            if (patch >= 7)  return "v1_21_R5";
            if (patch >= 5)  return "v1_21_R4";
            if (patch >= 3)  return "v1_21_R3";
            if (patch >= 2)  return "v1_21_R2";
            return "v1_21_R1";
        } else if (minor == 20) {
            if (patch >= 5) return "v1_20_R4";
            if (patch >= 3) return "v1_20_R3";
            if (patch == 2) return "v1_20_R2";
            return "v1_20_R1";
        } else if (minor == 19) {
            if (patch >= 3) return "v1_19_R3";
            if (patch >= 1) return "v1_19_R2";
            return "v1_19_R1";
        } else if (minor == 18) {
            return "v1_18_R2";
        } else if (minor == 17) {
            return "v1_17_R1";
        } else if (minor == 16) {
            return "v1_16_R3";
        } else if (minor == 12) {
            return "v1_12_R1";
        } else if (minor == 8) {
            return "v1_8_R3";
        }
        return "v1_" + minor + "_R1";
    }
}
