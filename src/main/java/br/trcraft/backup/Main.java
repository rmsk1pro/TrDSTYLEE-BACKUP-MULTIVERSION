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
        // Verifica dependências antes de inicializar
        if (!checkDependencies()) {
            getLogger().severe("❌ Dependências não atendidas. Desativando plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        printStartupMessage();

        // Verifica configuração do Dropbox ANTES da inicialização
        checkDropboxConfigPreInit();

        // Inicializa BackupManager
        try {
            this.backupManager = new BackupManager(this);
            getLogger().info("✅ BackupManager inicializado com sucesso.");
        } catch (Exception e) {
            getLogger().severe("❌ Erro ao inicializar BackupManager: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializa e inicia scheduler
        try {
            this.backupScheduler = new BackupScheduler(this, backupManager);
            backupScheduler.start();
            getLogger().info("✅ BackupScheduler inicializado com sucesso.");
        } catch (Exception e) {
            getLogger().severe("❌ Erro ao inicializar BackupScheduler: " + e.getMessage());
            e.printStackTrace();
        }

        // Registra comandos
        registerCommands();

        getLogger().info("✅ Plugin inicializado com sucesso!");
    }

    @Override
    public void onDisable() {
        printShutdownMessage();

        // Cancela backups em andamento
        if (backupManager != null && backupManager.isBackupRunning()) {
            getLogger().warning("⚠️ Backup em andamento detectado durante desativação...");
            backupManager.cancelBackup(Bukkit.getConsoleSender());
        }

        // Cancela scheduler
        if (backupScheduler != null) {
            backupScheduler.cancel();
            getLogger().info("§cBackup scheduler cancelado.");
        }

        // Limpeza de recursos
        if (backupManager != null) {
            getLogger().info("🔧 Liberando recursos do BackupManager...");
        }

        getLogger().info("✅ Plugin desativado com sucesso!");
    }

    private void registerCommands() {
        if (getCommand("backup") != null) {
            getCommand("backup").setExecutor(new CommandBackup(this, backupManager));
            getLogger().info("✅ Comando '/backup' registrado com sucesso.");
        } else {
            getLogger().severe("❌ Comando 'backup' não encontrado no plugin.yml!");
        }
    }

    /**
     * Método para recarregar a configuração e reiniciar o scheduler.
     */
    public void reloadPlugin() {
        getLogger().info("🔧 Recarregando configurações do plugin...");

        reloadConfig();                // Recarrega config.yml
        backupManager.reloadConfigs(); // Atualiza BackupManager com a nova configuração

        if (backupScheduler != null) {
            backupScheduler.cancel();  // Cancela scheduler antigo
        }

        // Cria novo scheduler e inicia
        backupScheduler = new BackupScheduler(this, backupManager);
        backupScheduler.start();

        getLogger().info("✅ Configurações recarregadas e scheduler reiniciado.");
    }

    /**
     * Verifica se todas as dependências estão presentes
     */
    private boolean checkDependencies() {
        try {
            // Verifica se classes básicas do Bukkit estão disponíveis
            Class.forName("org.bukkit.Bukkit");
            getLogger().info("✅ Bukkit API encontrada.");
        } catch (ClassNotFoundException e) {
            getLogger().severe("❌ Bukkit API não encontrada!");
            return false;
        }

        // Dependências opcionais - apenas logam avisos
        checkOptionalDependency("com.dropbox.core.DbxRequestConfig", "Dropbox SDK");
        checkOptionalDependency("com.jcraft.jsch.JSch", "JSch (SFTP)");
        checkOptionalDependency("java.sql.DriverManager", "JDBC (MySQL)");

        return true; // Plugin pode funcionar mesmo sem dependências opcionais
    }

    /**
     * Verifica dependências opcionais
     */
    private void checkOptionalDependency(String className, String dependencyName) {
        try {
            Class.forName(className);
            getLogger().info("✅ " + dependencyName + " encontrado.");
        } catch (ClassNotFoundException e) {
            getLogger().warning("⚠️ " + dependencyName + " não encontrado. Funcionalidades relacionadas não estarão disponíveis.");
        }
    }

    /**
     * Verifica configuração do Dropbox ANTES da inicialização (CORRIGIDO)
     */
    private void checkDropboxConfigPreInit() {
        if (getConfig().getBoolean("dropbox.enable", false)) {
            String accessToken = getConfig().getString("dropbox.accessToken", "");

            if (accessToken.isEmpty() || accessToken.equals("seu_access_token_aqui")) {
                getLogger().warning("⚠️ Dropbox habilitado mas access token não configurado!");
                getLogger().warning("⚠️ Configure o access token em config.yml: dropbox.accessToken");
                getLogger().warning("⚠️ Dropbox será desativado automaticamente.");

                // Desativa automaticamente se token não configurado
                getConfig().set("dropbox.enable", false);
                saveConfig();
            }

            // Verifica se SDK está disponível
            try {
                Class.forName("com.dropbox.core.DbxRequestConfig");
                getLogger().info("✅ Dropbox SDK encontrado.");
            } catch (ClassNotFoundException e) {
                getLogger().severe("❌ Dropbox habilitado mas SDK não encontrado!");
                getLogger().severe("❌ Adicione a dependência ou desative Dropbox no config.yml!");
                getLogger().severe("❌ Dropbox será desativado automaticamente.");

                // Desativa automaticamente se SDK não encontrado
                getConfig().set("dropbox.enable", false);
                saveConfig();
            }
        }
    }

    /**
     * Método antigo mantido para compatibilidade (pode ser removido)
     */
    private void checkDropboxConfig() {
        // Este método agora é redundante, mas mantido para não quebrar código existente
        checkDropboxConfigPreInit();
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
        Bukkit.getConsoleSender().sendMessage("§a           ☁️  Dropbox Integration Ready!         ");
        Bukkit.getConsoleSender().sendMessage("§a           📤  SFTP Integration Ready!           ");
        Bukkit.getConsoleSender().sendMessage("§a           💾  MySQL Backup Ready!               ");
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
        Bukkit.getConsoleSender().sendMessage("§4                                                  ");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
    }

    // Getters para acesso externo se necessário
    public BackupManager getBackupManager() {
        return backupManager;
    }

    public BackupScheduler getBackupScheduler() {
        return backupScheduler;
    }
}
