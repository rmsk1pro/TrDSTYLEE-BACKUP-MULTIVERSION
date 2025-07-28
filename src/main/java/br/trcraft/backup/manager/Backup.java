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

    public File createBackup() {
        FileConfiguration config = plugin.getConfig();

        String backupFolderName = config.getString("saveFolder", "##BACKUP");
        File backupDir = backupFolderName.startsWith(File.separator)
                ? new File(backupFolderName)
                : new File(plugin.getServer().getWorldContainer(), backupFolderName);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            plugin.getLogger().severe("[EasyBackupSFTP] Não foi possível criar a pasta de backup: " + backupDir.getAbsolutePath());
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
                    .filter(path -> !path.startsWith(backupDir.toPath())) // Ignora backups antigos
                    .filter(path -> !isInExemptFolders(path, serverRoot, exemptFolders))
                    .forEach(path -> {
                        Path relativePath = serverRoot.relativize(path);
                        String zipEntryName = relativePath.toString().replace("\\", "/");
                        try {
                            // Verifica se o arquivo pode ser aberto e lido
                            try (FileInputStream testStream = new FileInputStream(path.toFile())) {
                                testStream.read(); // lê 1 byte para validar
                            } catch (IOException e) {
                                plugin.getLogger().info("[EasyBackupSFTP] Ignorando arquivo bloqueado ou em uso: " + path.toString());
                                return; // ignora este arquivo
                            }

                            zos.putNextEntry(new ZipEntry(zipEntryName));
                            Files.copy(path, zos);
                            zos.closeEntry();

                            if (config.getBoolean("slowdownWhenServerLags", true)) {
                                try {
                                    Thread.sleep(config.getInt("backupDelayBetweenFiles", 100));
                                } catch (InterruptedException ignored) {}
                            }
                        } catch (IOException e) {
                            plugin.getLogger().severe("[EasyBackupSFTP] Erro ao adicionar arquivo ao backup: " + path);
                            e.printStackTrace();
                        }
                    });

            plugin.getLogger().info("[EasyBackupSFTP] Backup criado com sucesso: " + backupFile.getAbsolutePath());
            return backupFile;
        } catch (IOException e) {
            plugin.getLogger().severe("[EasyBackupSFTP] Erro ao criar backup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isInExemptFolders(Path path, Path basePath, List<String> exemptFolders) {
        if (exemptFolders == null) return false;
        for (String exempt : exemptFolders) {
            Path exemptPath = basePath.resolve(exempt).normalize();
            if (path.startsWith(exemptPath)) {
                return true;
            }
        }
        return false;
    }
}
