# 🚀 Backup Confiável - Configuração Completa
# 📅 Versão: 2.0 | Data: 2025-11-24
# 🛡️ Sistema completo de backup para servidores Minecraft

# =============================================
# 🔧 CONFIGURAÇÕES GERAIS DO PLUGIN
# =============================================

# 📁 Pasta onde os backups serão salvos localmente
saveFolder: "##BACKUP"

# 🔢 Número máximo de backups a manter antes de apagar os mais antigos
maxBackupsBeforeErase: 10

# ⏳ Delay entre processamento de arquivos (milissegundos)
backupDelayBetweenFiles: 100

# 🐌 Desacelerar backup quando servidor estiver com lag
slowdownWhenServerLags: true

# =============================================
# ☁️ CONFIGURAÇÃO DO DROPBOX
# =============================================
# 
# 📖 TUTORIAL PARA CONFIGURAR DROPBOX:
# 
# 🔑 PASSO 1: OBTER TOKEN DE ACESSO
# 1. Acesse: https://www.dropbox.com/developers/apps
# 2. Clique em "Create app"
# 3. Configure:
#    - API: Scoped access
#    - Access: Full Dropbox
#    - Name: minecraft-backup
# 4. Em "Permissions" marque:
#    ✅ files.content.write
#    ✅ files.content.read
# 5. Em "Settings" gere o token em "OAuth 2"
# 6. Cole o token (começa com sl.) abaixo
#
# ⚠️ IMPORTANTE: Mantenha o token em segredo!
#
dropbox:
  # ⚠️ ATIVE/DESATIVE O DROPBOX
  enable: true
  
  # 🔑 TOKEN DE ACESSO (COLE AQUI O TOKEN GERADO)
  accessToken: "cole_seu_token_aqui"
  
  # 📁 PASTA NO DROPBOX (NÃO MEXER)
  remoteFolder: "/minecraft_backups"

# =============================================
# 💾 CONFIGURAÇÃO DO MYSQL (OPCIONAL)
# =============================================

mysql:
  # ⚠️ ATIVE/DESATIVE BACKUP MYSQL
  enable: false
  
  # 🔌 CONFIGURAÇÕES DE CONEXÃO
  host: "localhost"
  port: 3306
  username: "root"
  password: ""
  database: "minecraft"
  
  # 🔄 MÉTODO DE BACKUP (mysqldump ou jdbc)
  method: "mysqldump"
  
  # 🛠️ CAMINHOS DO MYSQLDUMP
  mysqldumpWindowsPath: "C:\\xampp\\mysql\\bin\\mysqldump.exe"
  mysqldumpLinuxPath: "/usr/bin/mysqldump"

# =============================================
# 🌐 CONFIGURAÇÃO DO SFTP (OPCIONAL)
# =============================================

ftp:
  # ⚠️ ATIVE/DESATIVE SFTP
  enable: false
  
  # 🔌 CONFIGURAÇÕES DO SERVIDOR SFTP
  hostname: "localhost"
  port: 22
  username: "root"
  password: ""
  
  # 📁 PASTA REMOTA NO SERVIDOR SFTP
  saveLocation: "BACKUP"

# =============================================
# ⏰ AGENDAMENTO AUTOMÁTICO
# =============================================

backupSchedule:
  # ⚠️ ATIVE/DESATIVE AGENDAMENTO AUTOMÁTICO
  enabled: true
  
  # 📅 FAZER BACKUP TODOS OS DIAS
  everyDay: true
  
  # 🌍 FUSO HORÁRIO (America/Sao_Paulo, Europe/London, etc)
  timezone: "America/Sao_Paulo"
  
  # 🕐 HORÁRIOS PARA BACKUP AUTOMÁTICO
  times:
    - "02:00"    # 2h da manhã
    - "14:00"    # 14h da tarde

# =============================================
# 📋 PASTAS ISENTAS DO BACKUP
# =============================================
# Pastas que serão ignoradas durante o backup
exemptFolders:
  - "logs"                    # Logs do servidor
  - "crash-reports"           # Relatórios de crash
  - "cache"                   # Arquivos de cache
  - "##BACKUP"                # Evita recursão
  - "plugins/EasyBackup"      # Outro plugin de backup
  - "plugins/Backup"          # Outro plugin de backup

# =============================================
# 🔔 CONFIGURAÇÕES DE NOTIFICAÇÃO (FUTURO)
# =============================================

notifications:
  # 💬 NOTIFICAÇÕES NO CHAT
  chat:
    enabled: true
    successMessage: "§a✅ Backup concluído com sucesso!"
    startMessage: "§e⏳ Iniciando backup, por favor aguarde..."
    errorMessage: "§c❌ Erro durante o backup!"
  
  # 📧 NOTIFICAÇÕES POR WEBHOOK (FUTURO)
  webhook:
    enabled: false
    discordWebhook: ""
    successMessage: "Backup concluído com sucesso!"
    errorMessage: "Erro durante o backup!"

# =============================================
# ⚡ CONFIGURAÇÕES DE PERFORMANCE
# =============================================

performance:
  # 🧠 LIMITE DE MEMÓRIA PARA UPLOAD (MB)
  maxMemoryUsage: 512
  
  # 🔄 TENTATIVAS DE UPLOAD EM CASO DE FALHA
  maxRetryAttempts: 3
  
  # ⏱️ TIMEOUT PARA UPLOAD (SEGUNDOS)
  uploadTimeout: 300

# =============================================
# 🎨 CONFIGURAÇÕES DE MENSAGENS
# =============================================

messages:
  # 🎯 PREFIXO DAS MENSAGENS
  prefix: "§c§lBACKUP §e»"
  
  # 🌈 CORES DAS MENSAGENS
  colors:
    success: "§a"
    error: "§c"
    warning: "§e"
    info: "§f"
    progress: "§b"
  
  # 📊 CONFIGURAÇÕES DA BARRA DE PROGRESSO
  progressBar:
    enabled: true
    length: 10
    filledChar: "█"
    emptyChar: "░"
    filledColor: "§a"
    emptyColor: "§7"

# =============================================
# 🔒 CONFIGURAÇÕES DE SEGURANÇA
# =============================================

security:
  # 🔑 CRIPTOGRAFIA DE ARQUIVOS (FUTURO)
  encryption:
    enabled: false
    algorithm: "AES"
    key: ""
  
  # 📧 NOTIFICAÇÕES DE SEGURANÇA
  alerts:
    failedBackup: true
    invalidToken: true
    diskSpaceLow: true

# =============================================
# 📊 CONFIGURAÇÕES DE LOG
# =============================================

logging:
  # 📝 NÍVEL DE LOG (INFO, WARNING, ERROR, DEBUG)
  level: "INFO"
  
  # 💾 SALVAR LOGS EM ARQUIVO
  saveToFile: true
  
  # 📁 PASTA DE LOGS
  logFolder: "logs/backup"
  
  # 🔍 LOGS DETALHADOS
  verbose: false

# =============================================
# 🎯 CONFIGURAÇÕES AVANÇADAS
# =============================================

advanced:
  # 🔄 MODO DE COMPATIBILIDADE
  compatibilityMode: false
  
  # 🧪 MODO DE DESENVOLVIMENTO
  developmentMode: false
  
  # 📦 COMPRESSÃO MÁXIMA
  maximumCompression: true
  
  # 🔍 VERIFICAÇÃO DE INTEGRIDADE
  integrityCheck: true

# =============================================
# 💡 DICAS E INFORMAÇÕES
# =============================================

# 🎊 PARABÉNS! Sua configuração está completa!
#
# 📋 PRÓXIMOS PASSOS:
# 1. 🔑 Configure o token do Dropbox (seção acima)
# 2. 💾 Configure MySQL se necessário
# 3. 🌐 Configure SFTP se necessário  
# 4. ⏰ Ajuste os horários de agendamento
# 5. 🚀 Execute /backup reload
# 6. ✅ Teste com /backup create
#
# 🆘 PRECISA DE AJUDA?
# Consulte a documentação completa em:
# https://github.com/seunome/backup-confiável/wiki
#
# 🐛 ENCONTROU UM BUG?
# Reporte em: https://github.com/seunome/backup-confiável/issues
#
# 🎯 CONFIGURAÇÃO OTIMIZADA PARA:
# - Servidores com 2GB+ de RAM
# - Backups de até 10GB
# - Uploads simultâneos para múltiplos destinos
# - Zero impacto no desempenho do servidor
