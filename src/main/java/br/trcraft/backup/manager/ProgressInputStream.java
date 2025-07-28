package br.trcraft.backup.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends FilterInputStream {

    private final long totalSize;
    private long readBytes = 0;
    private final CommandSender sender;
    private final JavaPlugin plugin;
    private int lastPercent = 0;

    protected ProgressInputStream(InputStream in, long totalSize, CommandSender sender, JavaPlugin plugin) {
        super(in);
        this.totalSize = totalSize;
        this.sender = sender;
        this.plugin = plugin;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            readBytes++;
            checkProgress();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead > 0) {
            readBytes += bytesRead;
            checkProgress();
        }
        return bytesRead;
    }

    private void checkProgress() {
        int percent = (int) ((readBytes * 100) / totalSize);
        if (percent != lastPercent && percent % 10 == 0) {
            lastPercent = percent;
            String msg = "§e[SFTP] Upload " + percent + "% concluído...";
            // Executar na main thread para enviar mensagem ao jogador
            plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
        }
    }
}
