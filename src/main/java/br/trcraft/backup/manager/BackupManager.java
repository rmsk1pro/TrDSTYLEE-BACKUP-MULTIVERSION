package br.trcraft.backup.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class BackupManager {
    private final JavaPlugin plugin;
    private final Backup fileBackup;
    private MySQLBackupMethod mySQLBackupMethod;
    private SFTPManager sftp;

    private volatile boolean backupRunning = false;
    private volatile boolean cancelRequested = false;

    public BackupManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fileBackup = new Backup(plugin);
        initMySQLBackupMethod();
        setupSFTP();
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        setupSFTP();
        initMySQLBackupMethod();
        plugin.getLogger().info("Configurações recarregadas com sucesso.");
    }

    public synchronized boolean isBackupRunning() {
        return backupRunning;
    }

    private synchronized void setBackupRunning(boolean running) {
        this.backupRunning = running;
    }

    public synchronized void cancelBackup(CommandSender sender) {
        if (!backupRunning) {
            sendMessage(sender, "§cNão há backup em andamento para cancelar.");
        } else {
            cancelRequested = true;
            sendMessage(sender, "§eSolicitação de cancelamento do backup recebida. Abortando...");
            plugin.getLogger().info("Solicitação de cancelamento de backup recebida.");
        }
    }

    public void startBackupAsync() {
        startBackupAsync(null);
    }

    public void startBackupAsync(CommandSender sender) {
        boolean fromConsole = sender == null || !(sender instanceof Player);

        synchronized (this) {
            if (backupRunning) {
                sendMessage(sender, "§cJá existe um backup em andamento, aguarde ele terminar.");
                return;
            }
            setBackupRunning(true);
            cancelRequested = false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                notifyStart(sender, fromConsole);

                if (checkCancel(sender, "Backup cancelado antes de iniciar.")) return;

                plugin.getLogger().info("📁 Iniciando backup de arquivos...");
                File backupFile = fileBackup.createBackup();

                if (checkCancel(sender, "Backup cancelado após criação do backup de arquivos.")) return;

                if (backupFile != null && backupFile.exists()) {
                    plugin.getLogger().info("✅ Backup de arquivos criado em: " + backupFile.getAbsolutePath());
                    uploadFileIfConfigured(backupFile, sender, "arquivos");
                } else {
                    plugin.getLogger().warning("❌ Falha ao criar backup de arquivos.");
                    sendMessage(sender, "§cFalha ao criar backup de arquivos.");
                }

                if (checkCancel(sender, "Backup cancelado antes do backup MySQL.")) return;

                if (mySQLBackupMethod != null) {
                    plugin.getLogger().info("💾 Iniciando backup MySQL...");
                    File mysqlBackupFile = null;

                    try {
                        mysqlBackupFile = mySQLBackupMethod.createBackup();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Erro ao criar backup MySQL: " + e.getMessage());
                        e.printStackTrace();
                        sendMessage(sender, "§cErro ao criar backup MySQL: " + e.getMessage());
                    }

                    if (checkCancel(sender, "Backup cancelado após iniciar backup MySQL.")) return;

                    if (mysqlBackupFile != null && mysqlBackupFile.exists()) {
                        plugin.getLogger().info("✅ Backup MySQL criado em: " + mysqlBackupFile.getAbsolutePath());
                        uploadFileIfConfigured(mysqlBackupFile, sender, "MySQL");
                    } else {
                        plugin.getLogger().warning("❌ Falha ao criar backup MySQL.");
                        sendMessage(sender, "§cFalha ao criar backup MySQL.");
                    }
                } else {
                    plugin.getLogger().info("ℹ️ Backup MySQL está desativado nas configurações.");
                }

                if (checkCancel(sender, "Backup cancelado no final do processo.")) return;

                notifyFinish(sender, fromConsole);

            } finally {
                setBackupRunning(false);
                cancelRequested = false;
            }
        });
    }

    private void notifyStart(CommandSender sender, boolean fromConsole) {
        if (fromConsole) {
            plugin.getLogger().info("§c§l BACKUP §e» §aBACKUP INICIANDO...");
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("backup.staff"))
                            .forEach(p -> p.sendMessage("§c§l BACKUP §e» §aBackup iniciado pelo console. Aguarde...")));
        } else {
            sendMessage(sender, "§c§l BACKUP §e» §aIniciando backup, por favor aguarde...");
            plugin.getLogger().info("§c§l BACKUP §e» §aBackup iniciado por jogador.");
        }
    }

    private void notifyFinish(CommandSender sender, boolean fromConsole) {
        if (fromConsole) {
            plugin.getLogger().info("§c§l BACKUP §e» §aBackup finalizado.");
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("backup.staff"))
                            .forEach(p -> p.sendMessage("§c§l BACKUP §e» §aBackup finalizado.")));
        } else {
            sendMessage(sender, "§aBackup finalizado com sucesso.");
            plugin.getLogger().info("§c§l BACKUP §e» §aBackup finalizado.");
        }
    }

    private boolean checkCancel(CommandSender sender, String reason) {
        if (cancelRequested) {
            plugin.getLogger().info(reason);
            sendMessage(sender, "§c" + reason);
            return true;
        }
        return false;
    }

    private void uploadFileIfConfigured(File file, CommandSender sender, String label) {
        if (sftp != null) {
            try {
                sftp.uploadFile(file, sender); // Síncrono: aguarda upload terminar
                plugin.getLogger().info("📤 Backup " + label + " enviado via SFTP.");
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao enviar backup " + label + " via SFTP: " + e.getMessage());
                e.printStackTrace();
                sendMessage(sender, "§cErro ao enviar backup " + label + " via SFTP: " + e.getMessage());
            }
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
        }
    }

    private void initMySQLBackupMethod() {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("mysql.enable", false)) {
            plugin.getLogger().info("Backup MySQL desabilitado.");
            mySQLBackupMethod = null;
            return;
        }

        String method = config.getString("mysql.method", "mysqldump").toLowerCase();
        switch (method) {
            case "jdbc":
                plugin.getLogger().info("Backup MySQL via JDBC habilitado.");
                mySQLBackupMethod = new MySQLJdbcBackup(plugin);
                break;
            case "mysqldump":
            default:
                plugin.getLogger().info("Backup MySQL via mysqldump habilitado.");
                mySQLBackupMethod = new MySQLDumpBackup(plugin);
                break;
        }
    }

    private void setupSFTP() {
        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean("ftp.enable", false)) {
            String hostname = config.getString("ftp.hostname", "localhost");
            int port = config.getInt("ftp.port", 22);
            String username = config.getString("ftp.username", "root");
            String password = config.getString("ftp.password", "");
            String saveFolder = config.getString("ftp.saveLocation", "BACKUP");
            sftp = new SFTPManager(hostname, port, username, password, saveFolder, plugin);
            plugin.getLogger().info("SFTP ativado e configurado.");
        } else {
            sftp = null;
            plugin.getLogger().info("SFTP desativado nas configurações.");
        }
    }

    public Backup getFileBackup() {
        return fileBackup;
    }

    public MySQLBackupMethod getMySQLBackupMethod() {
        return mySQLBackupMethod;
    }

    public SFTPManager getSftp() {
        return sftp;
    }
}
