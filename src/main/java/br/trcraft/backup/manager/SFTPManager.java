package br.trcraft.backup.manager;

import com.jcraft.jsch.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Properties;

public class SFTPManager {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String remoteDirectory;
    private final JavaPlugin plugin;

    public SFTPManager(String host, int port, String username, String password, String remoteDirectory, JavaPlugin plugin) {
        this.host = host;
        this.port = (port > 0) ? port : 22;
        this.username = username;
        this.password = password;
        this.remoteDirectory = remoteDirectory;
        this.plugin = plugin;
    }

    /**
     * Upload síncrono do arquivo via SFTP, com relatório de progresso.
     */
    public void uploadFile(File file, CommandSender sender) throws Exception {
        Session session = null;
        ChannelSftp sftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(10000);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10000);

            try {
                sftp.cd(remoteDirectory);
            } catch (SftpException e) {
                sftp.mkdir(remoteDirectory);
                sftp.cd(remoteDirectory);
            }

            try (FileInputStream fis = new FileInputStream(file);
                 InputStream progressStream = new ProgressInputStream(fis, file.length(), sender, plugin)) {
                sftp.put(progressStream, file.getName());
            }

            sendMessage(sender, "§c§lBACKUP §e» §aUpload concluído! §2✅");
        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
        } else if (sender != null) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
        plugin.getLogger().info(convertColorsToAnsi(message));
    }

    private String convertColorsToAnsi(String message) {
        return message
                .replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
                .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
                .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
                .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
                .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
                .replace("§f", "\u001B[97m").replace("§l", "\u001B[1m").replace("§o", "\u001B[3m")
                .replace("§n", "\u001B[4m").replace("§r", "\u001B[0m") + "\u001B[0m";
    }

    private class ProgressInputStream extends InputStream {
        private final InputStream input;
        private final long totalSize;
        private final CommandSender sender;
        private final JavaPlugin plugin;

        private long bytesRead = 0;
        private int lastPercent = -10;

        public ProgressInputStream(InputStream input, long totalSize, CommandSender sender, JavaPlugin plugin) {
            this.input = input;
            this.totalSize = totalSize;
            this.sender = sender;
            this.plugin = plugin;
        }

        @Override
        public int read() throws IOException {
            int value = input.read();
            if (value != -1) {
                bytesRead++;
                checkProgress();
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = input.read(b, off, len);
            if (read > 0) {
                bytesRead += read;
                checkProgress();
            }
            return read;
        }

        private void checkProgress() {
            int percent = (int) ((bytesRead * 100) / totalSize);
            if (percent >= lastPercent + 10 || percent == 100) {
                lastPercent = (percent / 10) * 10;
                sendProgress(percent);
            }
        }

        private void sendProgress(int percent) {
            String bar = buildProgressBar(percent);
            String message = "§c§l BACKUP §e» §aUpload: " + bar + " §f" + percent + "%";

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (sender instanceof Player) {
                    sender.sendMessage(message);
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("backup.staff")) {
                            p.sendMessage(message);
                        }
                    }
                }
            });

            plugin.getLogger().info(convertColorsToAnsi(message));
        }

        private String buildProgressBar(int percent) {
            int filled = percent / 10;
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < filled; i++) bar.append("■");
            bar.append("§7");
            for (int i = filled; i < 10; i++) bar.append("■");
            return bar.toString();
        }

        @Override
        public void close() throws IOException {
            input.close();
            if (lastPercent < 100) {
                sendProgress(100);
            }
        }
    }
}
