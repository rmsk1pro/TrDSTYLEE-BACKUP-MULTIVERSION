package br.trcraft.backup.manager;

import java.io.File;

public interface MySQLBackupMethod {
    /**
     * Cria o backup e retorna o arquivo gerado
     * Pode lançar exceções para serem tratadas no caller
     */
    File createBackup() throws Exception;
}
