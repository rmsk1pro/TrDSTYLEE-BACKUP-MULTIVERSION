package br.trcraft.backup.Commandos;

import br.trcraft.backup.manager.BackupManager;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandBackup implements CommandExecutor {

    private final BackupManager backupManager;
    private final JavaPlugin plugin;

    public CommandBackup(JavaPlugin plugin, BackupManager backupManager) {
        this.plugin = plugin;
        this.backupManager = backupManager;
    }

    private String convertMinecraftColorsToAnsi(String message) {
        return message
                .replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
                .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
                .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
                .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
                .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
                .replace("§f", "\u001B[97m").replace("§r", "\u001B[0m")
                .replace("§l", "").replace("§n", "").replace("§m", "").replace("§o", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("backup.staff")) {
            send(sender, "§cVocê não tem permissão para executar esse comando.");
            return true;
        }

        if (args.length == 0) {
            send(sender, "§eUse /backup create para iniciar o backup.");
            send(sender, "§eUse /backup reload para recarregar as configurações.");
            send(sender, "§eUse /backup cancel ou /backup cancelar para cancelar o backup em andamento.");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                if (plugin instanceof br.trcraft.backup.Main) {
                    ((br.trcraft.backup.Main) plugin).reloadPlugin();
                } else {
                    plugin.reloadConfig();
                    backupManager.reloadConfigs();
                }
                send(sender, "§aConfiguração recarregada com sucesso.");
                return true;

            case "create":
                if (backupManager.isBackupRunning()) {
                    send(sender, "§cJá existe um backup em andamento, aguarde ele terminar.");
                    return true;
                }

                if (sender instanceof ConsoleCommandSender) {
                    // Mensagem apenas no console, staff será notificada pelo BackupManager
                    String consoleMsg = "§c§l BACKUP §e» §aBackup iniciado pelo console. Aguarde...";
                    plugin.getLogger().info(convertMinecraftColorsToAnsi(consoleMsg));
                }

                // Jogador não recebe mensagem aqui para evitar duplicidade
                // BackupManager envia as mensagens apropriadas

                backupManager.startBackupAsync(sender);
                return true;

            case "cancel":
            case "cancelar":
                if (!backupManager.isBackupRunning()) {
                    send(sender, "§cNão há backup em andamento para cancelar.");
                } else {
                    backupManager.cancelBackup(sender);
                }
                return true;

            default:
                send(sender, "§cUso inválido. Use /backup create, /backup reload, /backup cancel ou /backup cancelar.");
                return true;
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(message);
        plugin.getLogger().info(convertMinecraftColorsToAnsi(message));
    }
}
