package org.enthusia.tags.daily;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class DailyMaintenancePolicy {
    private static final String RESOURCE_NAME = "daily-maintenance.yml";
    private static final String DEFAULT_TIMEZONE = "America/Indiana/Indianapolis";
    private static final Object RELOAD_LOCK = new Object();

    private static volatile JavaPlugin plugin;
    private static volatile File configFile;
    private static volatile long lastModified = Long.MIN_VALUE;
    private static volatile State state = State.empty();

    private DailyMaintenancePolicy() {
    }

    public static void initialize(JavaPlugin owningPlugin) {
        Objects.requireNonNull(owningPlugin, "owningPlugin");
        synchronized (RELOAD_LOCK) {
            plugin = owningPlugin;
            configFile = new File(owningPlugin.getDataFolder(), RESOURCE_NAME);
            ensureConfigExists(owningPlugin, configFile);
            reloadLocked();
        }
    }

    public static boolean claimsPaused() {
        State snapshot = currentState();
        ZonedDateTime now = ZonedDateTime.now(snapshot.zone());
        return snapshot.windows().stream().anyMatch(window -> window.activeAt(now));
    }

    static List<DailyMaintenanceWindow> windowsSnapshot() {
        return currentState().windows();
    }

    static boolean streakContinues(LocalDate lastClaim, LocalDate today,
                                   List<DailyMaintenanceWindow> windows) {
        if (lastClaim == null || today == null || !lastClaim.isBefore(today)) {
            return false;
        }
        LocalDate cursor = lastClaim.plusDays(1L);
        while (cursor.isBefore(today)) {
            LocalDate date = cursor;
            boolean protectedDate = windows.stream().anyMatch(window -> window.preserves(date));
            if (!protectedDate) {
                return false;
            }
            cursor = cursor.plusDays(1L);
        }
        return true;
    }

    private static State currentState() {
        File file = configFile;
        if (file != null && file.exists() && file.lastModified() != lastModified) {
            synchronized (RELOAD_LOCK) {
                if (file.exists() && file.lastModified() != lastModified) {
                    reloadLocked();
                }
            }
        }
        return state;
    }

    private static void ensureConfigExists(JavaPlugin owningPlugin, File file) {
        if (file.exists()) {
            return;
        }
        try {
            owningPlugin.saveResource(RESOURCE_NAME, false);
        } catch (IllegalArgumentException ex) {
            owningPlugin.getLogger().severe("Could not create " + RESOURCE_NAME + ": " + ex.getMessage());
        }
    }

    private static void reloadLocked() {
        JavaPlugin owner = plugin;
        File file = configFile;
        if (owner == null || file == null || !file.exists()) {
            state = State.empty();
            lastModified = file == null ? Long.MIN_VALUE : file.lastModified();
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ZoneId zone = configuredZone(configuration, owner);
        List<DailyMaintenanceWindow> windows = configuredWindows(configuration, zone, owner);
        state = new State(zone, windows);
        lastModified = file.lastModified();
        owner.getLogger().info("Loaded " + windows.size() + " daily maintenance window(s) from "
            + RESOURCE_NAME + ".");
    }

    private static ZoneId configuredZone(YamlConfiguration configuration, JavaPlugin owner) {
        String configured = configuration.getString("timezone", DEFAULT_TIMEZONE);
        if (configured == null || configured.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (DateTimeException ex) {
            owner.getLogger().warning("Invalid daily maintenance timezone " + configured
                + "; using " + DEFAULT_TIMEZONE + ".");
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private static List<DailyMaintenanceWindow> configuredWindows(YamlConfiguration configuration,
                                                                   ZoneId zone,
                                                                   JavaPlugin owner) {
        ConfigurationSection section = configuration.getConfigurationSection("windows");
        if (section == null) {
            return List.of();
        }

        List<DailyMaintenanceWindow> windows = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection window = section.getConfigurationSection(id);
            if (window == null || !window.getBoolean("enabled", true)) {
                continue;
            }
            String startValue = window.getString("start", "");
            String endValue = window.getString("end", "");
            try {
                ZonedDateTime start = LocalDateTime.parse(startValue).atZone(zone);
                ZonedDateTime end = LocalDateTime.parse(endValue).atZone(zone);
                windows.add(new DailyMaintenanceWindow(start, end,
                    window.getBoolean("preserve-streaks", true)));
            } catch (DateTimeException | IllegalArgumentException ex) {
                owner.getLogger().warning("Ignoring invalid daily maintenance window '" + id
                    + "': " + ex.getMessage());
            }
        }
        windows.sort(Comparator.comparing(DailyMaintenanceWindow::start));
        return List.copyOf(windows);
    }

    private record State(ZoneId zone, List<DailyMaintenanceWindow> windows) {
        private State {
            Objects.requireNonNull(zone, "zone");
            windows = List.copyOf(windows);
        }

        private static State empty() {
            return new State(ZoneId.of(DEFAULT_TIMEZONE), List.of());
        }
    }
}
