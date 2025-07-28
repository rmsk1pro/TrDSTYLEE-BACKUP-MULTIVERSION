package br.trcraft.backup.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MySQLJdbcBackup implements MySQLBackupMethod {

    private final JavaPlugin plugin;

    public MySQLJdbcBackup(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public File createBackup() throws Exception {
        FileConfiguration config = plugin.getConfig();

        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String user = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "");
        String database = config.getString("mysql.database", "");

        // Cria pasta do backup se não existir
        File backupFolder = new File(plugin.getConfig().getString("saveFolder", "##BACKUP"));
        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            plugin.getLogger().severe("Não foi possível criar diretório de backup MySQL");
            return null;
        }

        // Nome do arquivo
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupFolder, "mysql_jdbc_backup_" + timestamp + ".sql");

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             FileOutputStream fos = new FileOutputStream(backupFile);
             Statement stmtTables = conn.createStatement()) {

            // Pegando todas as tabelas do banco
            try (ResultSet tables = stmtTables.executeQuery("SHOW TABLES")) {

                StringBuilder dump = new StringBuilder();

                while (tables.next()) {
                    String table = tables.getString(1);

                    // Usar Statement separado para SHOW CREATE TABLE
                    try (Statement stmtCreate = conn.createStatement();
                         ResultSet createTableRs = stmtCreate.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                        if (createTableRs.next()) {
                            dump.append("DROP TABLE IF EXISTS `").append(table).append("`;\n");
                            dump.append(createTableRs.getString(2)).append(";\n\n");
                        }
                    }

                    // Usar Statement separado para SELECT *
                    try (Statement stmtData = conn.createStatement();
                         ResultSet dataRs = stmtData.executeQuery("SELECT * FROM `" + table + "`")) {

                        int columnCount = dataRs.getMetaData().getColumnCount();

                        while (dataRs.next()) {
                            StringBuilder row = new StringBuilder();
                            row.append("INSERT INTO `").append(table).append("` VALUES (");
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = dataRs.getObject(i);
                                if (value == null) {
                                    row.append("NULL");
                                } else {
                                    String valStr = value.toString().replace("'", "''");
                                    row.append("'").append(valStr).append("'");
                                }
                                if (i < columnCount) {
                                    row.append(", ");
                                }
                            }
                            row.append(");\n");
                            dump.append(row);
                        }
                    }

                    dump.append("\n");
                }

                byte[] bytes = dump.toString().getBytes("UTF-8");
                fos.write(bytes);

                plugin.getLogger().info("Backup JDBC MySQL criado: " + backupFile.getAbsolutePath());
                return backupFile;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Erro no backup JDBC MySQL: " + e.getMessage());
            throw e;
        }
    }
}
