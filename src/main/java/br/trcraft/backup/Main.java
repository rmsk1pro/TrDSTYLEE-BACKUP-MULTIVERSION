package br.trcraft.backup;

import br.trcraft.backup.Commandos.CommandBackup;
import br.trcraft.backup.manager.BackupManager;
import br.trcraft.backup.manager.BackupScheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private BackupManager backupManager;
    private BackupScheduler backupScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        printStartupMessage();

        // Inicializa BackupManager
        this.backupManager = new BackupManager(this);

        // Inicializa e inicia scheduler
        this.backupScheduler = new BackupScheduler(this, backupManager);
        backupScheduler.start();

        registerCommands();
    }

    @Override
    public void onDisable() {
        printShutdownMessage();
        if (backupScheduler != null) {
            backupScheduler.cancel();
            getLogger().info("§cBackup scheduler cancelado.");
        }
    }

    private void registerCommands() {
        if (getCommand("backup") != null) {
            getCommand("backup").setExecutor(new CommandBackup(this, backupManager));
        } else {
            getLogger().severe("Comando 'backup' não encontrado no plugin.yml!");
        }
    }

    /**
     * Método para recarregar a configuração e reiniciar o scheduler.
     */
    public void reloadPlugin() {
        reloadConfig();                // Recarrega config.yml
        backupManager.reloadConfigs(); // Atualiza BackupManager com a nova configuração

        if (backupScheduler != null) {
            backupScheduler.cancel();  // Cancela scheduler antigo
        }

        // Cria novo scheduler e inicia
        backupScheduler = new BackupScheduler(this, backupManager);
        backupScheduler.start();

        getLogger().info("Configurações recarregadas e scheduler reiniciado.");
    }

    private void printStartupMessage() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§a ██████╗  █████╗  ██████╗██╗  ██╗██╗   ██╗██████╗ ");
        Bukkit.getConsoleSender().sendMessage("§a ██╔══██╗██╔══██╗██╔════╝██║ ██╔╝██║   ██║██╔══██╗");
        Bukkit.getConsoleSender().sendMessage("§a ██████╔╝███████║██║     █████╔╝ ██║   ██║██████╔╝");
        Bukkit.getConsoleSender().sendMessage("§a ██╔══██╗██╔══██║██║     ██╔═██╗ ██║   ██║██╔═══╝ ");
        Bukkit.getConsoleSender().sendMessage("§a ██████╔╝██║  ██║╚██████╗██║  ██╗╚██████╔╝██║     ");
        Bukkit.getConsoleSender().sendMessage("§a ╚═════╝ ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝ ╚═════╝ ╚═╝     ");
        Bukkit.getConsoleSender().sendMessage("§a                                                  ");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void printShutdownMessage() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§4 ██████╗  █████╗  ██████╗██╗  ██╗██╗   ██╗██████╗ ");
        Bukkit.getConsoleSender().sendMessage("§4 ██╔══██╗██╔══██╗██╔════╝██║ ██╔╝██║   ██║██╔══██╗");
        Bukkit.getConsoleSender().sendMessage("§4 ██████╔╝███████║██║     █████╔╝ ██║   ██║██████╔╝");
        Bukkit.getConsoleSender().sendMessage("§4 ██╔══██╗██╔══██║██║     ██╔═██╗ ██║   ██║██╔═══╝ ");
        Bukkit.getConsoleSender().sendMessage("§4 ██████╔╝██║  ██║╚██████╗██║  ██╗╚██████╔╝██║     ");
        Bukkit.getConsoleSender().sendMessage("§4 ╚═════╝ ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝ ╚═════╝ ╚═╝     ");
        Bukkit.getConsoleSender().sendMessage("§a                                                  ");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
    }
}
