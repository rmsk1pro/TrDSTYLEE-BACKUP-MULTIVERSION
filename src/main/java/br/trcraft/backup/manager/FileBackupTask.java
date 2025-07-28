package br.trcraft.backup.manager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileBackupTask extends BukkitRunnable {
    private final JavaPlugin plugin;

    public FileBackupTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getLogger().info("Iniciando backup dos arquivos...");

        File worldFolder = plugin.getServer().getWorldContainer(); // Pasta raiz do servidor

        File backupFile = new File(plugin.getDataFolder(), "backup_" + System.currentTimeMillis() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            zipFolder(worldFolder, worldFolder.getName(), zos);
            plugin.getLogger().info("Backup dos arquivos finalizado: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao fazer backup dos arquivos: " + e.getMessage());
        }
    }

    private void zipFolder(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipFolder(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
            java.nio.file.Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }
    }
}
