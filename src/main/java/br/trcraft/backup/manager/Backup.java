package br.trcraft.backup.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Backup {

    private final JavaPlugin plugin;

    public Backup(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cria backup completo do servidor, exceto pastas isentas
     * @return Arquivo de backup criado ou null se falhar
     */
    public File createBackup() {
        FileConfiguration config = plugin.getConfig();
        String backupFolderName = config.getString("saveFolder", "BACKUP");
        File backupDir = backupFolderName.startsWith(File.separator)
                ? new File(backupFolderName)
                : new File(plugin.getServer().getWorldContainer(), backupFolderName);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            plugin.getLogger().severe("[Backup] Não foi possível criar a pasta de backup: " + backupDir.getAbsolutePath());
            return null;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = config.getString("name", "backup_%time%").replace("%time%", date);
        String extension = config.getString("extension", "zip");
        File backupFile = new File(backupDir, backupName + "." + extension);

        List<String> exemptFolders = config.getStringList("exemptFolders");
        Path serverRoot = plugin.getServer().getWorldContainer().toPath();

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(serverRoot)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> !path.startsWith(backupDir.toPath()))
                    .filter(path -> !isInExemptFolders(path, serverRoot, exemptFolders))
                    .forEach(path -> {
                        Path relativePath = serverRoot.relativize(path);
                        String zipEntryName = relativePath.toString().replace("\\", "/");
                        try {
                            try (FileInputStream testStream = new FileInputStream(path.toFile())) {
                                testStream.read(); // verifica se arquivo pode ser lido
                            } catch (IOException e) {
                                plugin.getLogger().info("[Backup] Ignorando arquivo bloqueado ou em uso: " + path);
                                return;
                            }

                            zos.putNextEntry(new ZipEntry(zipEntryName));
                            Files.copy(path, zos);
                            zos.closeEntry();

                            if (config.getBoolean("slowdownWhenServerLags", true)) {
                                try { Thread.sleep(config.getInt("backupDelayBetweenFiles", 100)); }
                                catch (InterruptedException ignored) {}
                            }
                        } catch (IOException e) {
                            plugin.getLogger().severe("[Backup] Erro ao adicionar arquivo ao backup: " + path);
                            e.printStackTrace();
                        }
                    });

            plugin.getLogger().info("[Backup] Backup criado com sucesso: " + backupFile.getAbsolutePath());
            return backupFile;

        } catch (IOException e) {
            plugin.getLogger().severe("[Backup] Erro ao criar backup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isInExemptFolders(Path path, Path basePath, List<String> exemptFolders) {
        if (exemptFolders == null) return false;
        for (String exempt : exemptFolders) {
            Path exemptPath = basePath.resolve(exempt).normalize();
            if (path.startsWith(exemptPath)) return true;
        }
        return false;
    }

    /**
     * Retorna a pasta onde os backups são salvos
     */
    public File getBackupFolder() {
        String backupFolderName = plugin.getConfig().getString("saveFolder", "BACKUP");
        return backupFolderName.startsWith(File.separator)
                ? new File(backupFolderName)
                : new File(plugin.getServer().getWorldContainer(), backupFolderName);
    }
}
