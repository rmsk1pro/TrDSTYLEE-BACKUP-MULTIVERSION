package br.trcraft.backup.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MySQLDumpBackup implements MySQLBackupMethod {

    private final JavaPlugin plugin;

    public MySQLDumpBackup(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public File createBackup() throws IOException, InterruptedException {
        FileConfiguration config = plugin.getConfig();

        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String user = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "");
        String database = config.getString("mysql.database", "");

        String mysqldumpPath = plugin.getServer().getWorldContainer().getAbsolutePath().startsWith("C:") ?
                config.getString("mysql.mysqldumpWindowsPath", "C:\\xampp\\mysql\\bin\\mysqldump.exe") :
                config.getString("mysql.mysqldumpLinuxPath", "/usr/bin/mysqldump");

        // Cria pasta do backup se não existir
        File backupFolder = new File(config.getString("saveFolder", "##BACKUP"));
        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            plugin.getLogger().severe("Não foi possível criar diretório de backup MySQL");
            return null;
        }

        // Nome do arquivo
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupFolder, "mysql_backup_" + timestamp + ".sql");

        // Monta comando mysqldump como lista de argumentos (evita problemas de parsing de aspas)
        List<String> command = new ArrayList<>();
        command.add(mysqldumpPath);
        command.add("-h" + host);
        command.add("-P" + port);
        command.add("-u" + user);
        command.add("-p" + password);
        command.add(database);
        command.add("-r");
        command.add(backupFile.getAbsolutePath());

        plugin.getLogger().info("Executando mysqldump para backup MySQL...");

        // Executa comando com ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int result = process.waitFor();

        if (result == 0 && backupFile.exists()) {
            plugin.getLogger().info("Backup MySQL criado com sucesso em: " + backupFile.getAbsolutePath());
            return backupFile;
        } else {
            plugin.getLogger().severe("Erro ao executar mysqldump, código de saída: " + result);
            return null;
        }
    }
}
