package br.trcraft.backup.manager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class BackupScheduler extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final BackupManager backupManager;
    private final List<String> schedule;
    private final boolean scheduleEnabled;
    private final boolean everyDay;
    private final ZoneId zone;

    private final Set<ExecutionKey> executedToday = new HashSet<>();
    private LocalDate lastClearDate = null;
    private boolean firstRun = true;

    private static final DateTimeFormatter FORMAT_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter FORMAT_24H = DateTimeFormatter.ofPattern("H:mm");

    public BackupScheduler(JavaPlugin plugin, BackupManager backupManager) {
        this.plugin = plugin;
        this.backupManager = backupManager;
        this.scheduleEnabled = plugin.getConfig().getBoolean("backupSchedule.enabled", true);
        this.everyDay = plugin.getConfig().getBoolean("backupSchedule.everyDay", true);
        this.schedule = plugin.getConfig().getStringList("backupSchedule.times");

        String tz = plugin.getConfig().getString("backupSchedule.timezone", "America/Sao_Paulo");
        this.zone = ZoneId.of(tz);
    }

    @Override
    public void run() {
        if (!scheduleEnabled) return;

        if (firstRun) {
            plugin.getLogger().info("⏳ Ignorando primeira execução do scheduler para evitar backup imediato.");
            firstRun = false;
            return;
        }

        LocalDate today = LocalDate.now(zone);
        LocalTime nowTime = LocalTime.now(zone).withSecond(0).withNano(0);
        DayOfWeek todayDay = today.getDayOfWeek();

        if (lastClearDate == null || !lastClearDate.equals(today)) {
            executedToday.clear();
            lastClearDate = today;
        }

        for (String sched : schedule) {
            String schedTrimmed = sched.trim().toUpperCase(Locale.ENGLISH);

            if (matchesSchedule(schedTrimmed, today, nowTime, todayDay)) {
                ExecutionKey key = new ExecutionKey(today, schedTrimmed, nowTime);

                if (!executedToday.contains(key)) {
                    plugin.getLogger().info("⏰ 📦 Executando backup automático agendado para: " + sched);

                    String notifyMsg = "§c§l BACKUP §eBackup automático iniciado por favor, aguarde...";
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("backup.staff"))
                            .forEach(p -> p.sendMessage(notifyMsg));

                    Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.stripColor(notifyMsg));

                    backupManager.startBackupAsync(null);

                    executedToday.add(key);
                }
            }
        }
    }

    private boolean matchesSchedule(String sched, LocalDate today, LocalTime nowTime, DayOfWeek todayDay) {
        if (everyDay) {
            return timeMatches(sched, nowTime);
        } else {
            if (sched.matches("^(MON|TUE|WED|THU|FRI|SAT|SUN)(\\s+(MON|TUE|WED|THU|FRI|SAT|SUN))*\\s+.+$")) {
                String[] parts = sched.split("\\s+");
                String timePart = parts[parts.length - 1];

                boolean dayMatches = false;
                for (int i = 0; i < parts.length - 1; i++) {
                    DayOfWeek day = parseDay(parts[i]);
                    if (day != null && day == todayDay) {
                        dayMatches = true;
                        break;
                    }
                }
                if (!dayMatches) return false;

                return timeMatches(timePart, nowTime);
            } else {
                return timeMatches(sched, nowTime);
            }
        }
    }

    private DayOfWeek parseDay(String dayStr) {
        switch (dayStr) {
            case "MON": return DayOfWeek.MONDAY;
            case "TUE": return DayOfWeek.TUESDAY;
            case "WED": return DayOfWeek.WEDNESDAY;
            case "THU": return DayOfWeek.THURSDAY;
            case "FRI": return DayOfWeek.FRIDAY;
            case "SAT": return DayOfWeek.SATURDAY;
            case "SUN": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private boolean timeMatches(String scheduledTimeStr, LocalTime nowTime) {
        scheduledTimeStr = scheduledTimeStr.toUpperCase(Locale.ENGLISH).trim();

        try {
            LocalTime scheduledTime = LocalTime.parse(scheduledTimeStr, FORMAT_12H);
            return isSameMinute(scheduledTime, nowTime);
        } catch (DateTimeParseException ignored) {}

        try {
            LocalTime scheduledTime = LocalTime.parse(scheduledTimeStr, FORMAT_24H);
            return isSameMinute(scheduledTime, nowTime);
        } catch (DateTimeParseException ignored) {}

        String nowFormatted = nowTime.format(FORMAT_24H);
        return scheduledTimeStr.equalsIgnoreCase(nowFormatted);
    }

    private boolean isSameMinute(LocalTime t1, LocalTime t2) {
        return t1.getHour() == t2.getHour() && t1.getMinute() == t2.getMinute();
    }

    public void start() {
        plugin.getLogger().info("🕒 BackupScheduler iniciado no timezone: " + zone);

        long delay = 0L;
        long period = 20L * 30; // 30 segundos
        this.runTaskTimer(plugin, delay, period);
        plugin.getLogger().info("✅ Backup scheduler iniciado, checando a cada 30 segundos.");
    }

    private static class ExecutionKey {
        private final LocalDate date;
        private final String scheduleStr;
        private final LocalTime time;

        public ExecutionKey(LocalDate date, String scheduleStr, LocalTime time) {
            this.date = date;
            this.scheduleStr = scheduleStr;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExecutionKey)) return false;
            ExecutionKey that = (ExecutionKey) o;
            return date.equals(that.date) &&
                    scheduleStr.equals(that.scheduleStr) &&
                    time.equals(that.time);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, scheduleStr, time);
        }
    }
}
