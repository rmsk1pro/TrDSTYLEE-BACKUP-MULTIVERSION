package br.trcraft.backup.manager;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.UUID;

public class DropboxManager {

    private final JavaPlugin plugin;
    private DbxClientV2 client;
    private final boolean enabled;
    private final String accessToken;
    private final String remoteFolder;
    private static final long CHUNK_SIZE = 100 * 1024 * 1024; // 100MB por parte

    public DropboxManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("dropbox.enable", false);
        this.accessToken = plugin.getConfig().getString("dropbox.accessToken", "");
        this.remoteFolder = plugin.getConfig().getString("dropbox.remoteFolder", "/minecraft_backups");

        if (enabled && !accessToken.isEmpty()) {
            initializeClient();
        }
    }

    private void initializeClient() {
        try {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("minecraft-backup-plugin").build();
            this.client = new DbxClientV2(config, accessToken);

            // Testa a conexão
            client.users().getCurrentAccount();
            plugin.getLogger().info("✅ Dropbox manager inicializado com sucesso.");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Erro ao inicializar Dropbox: " + e.getMessage());
            this.client = null;
        }
    }

    /**
     * Faz upload de um arquivo para o Dropbox em PARTES (para arquivos grandes)
     */
    public void uploadFile(File file, CommandSender sender) throws Exception {
        if (!enabled || client == null) {
            throw new Exception("Dropbox não está configurado ou habilitado.");
        }

        if (!file.exists()) {
            throw new Exception("Arquivo não existe: " + file.getAbsolutePath());
        }

        long fileSize = file.length();
        String remotePath = remoteFolder + "/" + file.getName();

        plugin.getLogger().info("☁️ Iniciando upload para Dropbox: " + file.getName() + " (" + formatFileSize(fileSize) + ")");
        sendMessage(sender, "§c§lBACKUP §e» §aIniciando upload para Dropbox...");

        // Decide se faz upload simples ou em partes
        if (fileSize <= CHUNK_SIZE) {
            uploadSimple(file, remotePath, sender);
        } else {
            uploadChunked(file, remotePath, sender, fileSize);
        }
    }

    /**
     * Upload SIMPLES para arquivos pequenos (< 100MB)
     */
    private void uploadSimple(File file, String remotePath, CommandSender sender) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            ProgressInputStream progressStream = new ProgressInputStream(in, file.length(), sender, plugin, file.getName());

            FileMetadata metadata = client.files().uploadBuilder(remotePath)
                    .withAutorename(true)
                    .uploadAndFinish(progressStream);

            sendMessage(sender, "§c§lBACKUP §e» §aUpload para Dropbox concluído! §2✅");
            plugin.getLogger().info("📤 Arquivo enviado para Dropbox: " + metadata.getName() + " (" + formatFileSize(metadata.getSize()) + ")");
        }
    }

    /**
     * Upload em PARTES para arquivos grandes (> 100MB)
     */
    private void uploadChunked(File file, String remotePath, CommandSender sender, long fileSize) throws Exception {
        String sessionId = null;
        long uploaded = 0;

        try (FileInputStream fileStream = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(CHUNK_SIZE, 8 * 1024 * 1024)]; // 8MB buffer

            // Envia partes
            while (uploaded < fileSize) {
                long remaining = fileSize - uploaded;
                long chunkSize = Math.min(CHUNK_SIZE, remaining);

                ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
                int bytesRead;
                long chunkUploaded = 0;

                // Lê o chunk para memória
                while (chunkUploaded < chunkSize &&
                        (bytesRead = fileStream.read(buffer, 0, (int) Math.min(buffer.length, chunkSize - chunkUploaded))) != -1) {
                    chunkStream.write(buffer, 0, bytesRead);
                    chunkUploaded += bytesRead;
                }

                byte[] chunkData = chunkStream.toByteArray();

                if (sessionId == null) {
                    // Primeira parte - inicia sessão
                    sessionId = client.files().uploadSessionStart()
                            .uploadAndFinish(new ByteArrayInputStream(chunkData))
                            .getSessionId();
                    plugin.getLogger().info("📦 Iniciando upload em partes...");
                } else {
                    // Partes subsequentes
                    UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
                    client.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(new ByteArrayInputStream(chunkData));
                }

                uploaded += chunkUploaded;

                // Progresso
                int percent = (int) ((uploaded * 100) / fileSize);
                sendProgress(sender, file.getName(), percent, uploaded, fileSize, true);

                plugin.getLogger().info("📦 Parte enviada: " + formatFileSize(uploaded) + " / " + formatFileSize(fileSize));
            }

            // Finaliza upload
            UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
            CommitInfo commitInfo = CommitInfo.newBuilder(remotePath)
                    .withAutorename(true)
                    .build();

            FileMetadata metadata = client.files().uploadSessionFinish(cursor, commitInfo)
                    .uploadAndFinish(new ByteArrayInputStream(new byte[0]));

            sendMessage(sender, "§c§lBACKUP §e» §aUpload COMPLETO para Dropbox! §2✅");
            plugin.getLogger().info("📤 Arquivo grande enviado para Dropbox: " + metadata.getName() + " (" + formatFileSize(metadata.getSize()) + ")");

        } catch (Exception e) {
            throw new Exception("Erro no upload em partes: " + e.getMessage(), e);
        }
    }

    /**
     * Formata tamanho de arquivo para leitura humana
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Envia progresso do upload
     */
    private void sendProgress(CommandSender sender, String fileName, int percent, long uploaded, long total, boolean chunked) {
        String bar = buildProgressBar(percent);
        String chunkInfo = chunked ? " [PARTES]" : "";
        String sizeInfo = " (" + formatFileSize(uploaded) + " / " + formatFileSize(total) + ")";

        String message = "§c§l BACKUP §e» §aDropbox (" + fileName + ")" + chunkInfo + ": " + bar + " §f" + percent + "%" + sizeInfo;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (sender instanceof Player) {
                sender.sendMessage(message);
            } else {
                // Envia para todos os staffs online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("backup.staff")) {
                        p.sendMessage(message);
                    }
                }
                // Também envia para console
                Bukkit.getConsoleSender().sendMessage(message);
            }
        });

        // Log para arquivo
        String cleanMessage = "[BackupSFTP] BACKUP » Dropbox (" + fileName + ")" + chunkInfo + ": " +
                getProgressBarText(percent) + " " + percent + "%" + sizeInfo;
        plugin.getLogger().info(cleanMessage);
    }

    /**
     * Constrói barra de progresso com cores
     */
    private String buildProgressBar(int percent) {
        int filled = Math.min(10, percent / 10);
        StringBuilder bar = new StringBuilder();

        // Parte preenchida (verde)
        if (filled > 0) {
            bar.append("§a");
            for (int i = 0; i < filled; i++) {
                bar.append("█");
            }
        }

        // Parte não preenchida (cinza)
        if (filled < 10) {
            bar.append("§7");
            for (int i = filled; i < 10; i++) {
                bar.append("█");
            }
        }

        return bar.toString();
    }

    /**
     * Barra de progresso para logs (sem cores)
     */
    private String getProgressBarText(int percent) {
        int filled = Math.min(10, percent / 10);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < filled; i++) bar.append("█");
        for (int i = filled; i < 10; i++) bar.append("░");

        return bar.toString();
    }

    /**
     * Tratamento de erros simplificado
     */
    private String handleUploadError(UploadErrorException e) {
        String errorMessage = e.getMessage();

        if (errorMessage.contains("insufficient_space")) {
            return "Espaço insuficiente no Dropbox";
        } else if (errorMessage.contains("payload_too_large")) {
            return "Arquivo muito grande - usando upload em partes";
        } else if (errorMessage.contains("conflict")) {
            return "Arquivo já existe (conflito)";
        } else if (errorMessage.contains("no_write_permission")) {
            return "Sem permissão de escrita";
        } else {
            return "Erro de upload: " + errorMessage;
        }
    }

    public boolean isEnabled() {
        return enabled && client != null;
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
        return message.replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
                .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
                .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
                .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
                .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
                .replace("§f", "\u001B[97m").replace("§l", "\u001B[1m").replace("§o", "\u001B[3m")
                .replace("§n", "\u001B[4m").replace("§r", "\u001B[0m") + "\u001B[0m";
    }

    // ProgressInputStream permanece igual...
    private static class ProgressInputStream extends InputStream {
        private final InputStream input;
        private final long totalSize;
        private final CommandSender sender;
        private final JavaPlugin plugin;
        private final String fileName;

        private long bytesRead = 0;
        private int lastPercent = -1;

        public ProgressInputStream(InputStream input, long totalSize, CommandSender sender, JavaPlugin plugin, String fileName) {
            this.input = input;
            this.totalSize = totalSize;
            this.sender = sender;
            this.plugin = plugin;
            this.fileName = fileName;
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
            if (totalSize <= 0) return;

            int percent = (int) ((bytesRead * 100) / totalSize);
            if (percent != lastPercent && (percent % 10 == 0 || percent == 100)) {
                lastPercent = percent;
                sendProgress(percent);
            }
        }

        private void sendProgress(int percent) {
            String bar = buildProgressBar(percent);
            String message = "§c§l BACKUP §e» §aDropbox (" + fileName + "): " + bar + " §f" + percent + "%";

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (sender instanceof Player) {
                    sender.sendMessage(message);
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("backup.staff")) {
                            p.sendMessage(message);
                        }
                    }
                    Bukkit.getConsoleSender().sendMessage(message);
                }
            });

            String cleanMessage = "[BackupSFTP] BACKUP » Dropbox (" + fileName + "): " +
                    getProgressBarText(percent) + " " + percent + "%";
            plugin.getLogger().info(cleanMessage);
        }

        private String buildProgressBar(int percent) {
            int filled = Math.min(10, percent / 10);
            StringBuilder bar = new StringBuilder();

            if (filled > 0) {
                bar.append("§a");
                for (int i = 0; i < filled; i++) bar.append("█");
            }

            if (filled < 10) {
                bar.append("§7");
                for (int i = filled; i < 10; i++) bar.append("█");
            }

            return bar.toString();
        }

        private String getProgressBarText(int percent) {
            int filled = Math.min(10, percent / 10);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < filled; i++) bar.append("█");
            for (int i = filled; i < 10; i++) bar.append("░");
            return bar.toString();
        }

        @Override
        public void close() throws IOException {
            input.close();
            if (lastPercent < 100) sendProgress(100);
        }
    }
}