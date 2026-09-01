$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot 'server'

if (-not (Test-Path $serverDir)) {
    throw "Server-Ordner nicht gefunden: $serverDir"
}

$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'spigot-1\.8\.8\.jar' }
if ($runningServer) {
    throw "SkyKings/Spigot laeuft noch. Fuer ein konsistentes Backup zuerst in der Serverkonsole 'stop' eingeben."
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $serverDir ("backups\server-$stamp")
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

Write-Host '== SkyKings Server Backup ==' -ForegroundColor Cyan
Write-Host ("Ziel: {0}" -f $backupRoot) -ForegroundColor DarkGray

$worlds = @(
    'world', 'world_nether', 'world_the_end',
    'SkyPvP', 'SkyPlots', 'SkyIslands', 'SkyEvents', 'SkyCommunityEvent'
)

$pluginData = @(
    'SkyKings-Core', 'SkyKings-Combat', 'SkyKings-Crates', 'SkyKings-Admin',
    'LuckPerms', 'Vault'
)

$configFiles = @(
    'server.properties', 'bukkit.yml', 'spigot.yml', 'commands.yml',
    'permissions.yml', 'ops.json', 'whitelist.json', 'usercache.json'
)

$worldBackup = Join-Path $backupRoot 'worlds'
$pluginBackup = Join-Path $backupRoot 'plugins'
$configBackup = Join-Path $backupRoot 'config'
New-Item -ItemType Directory -Path $worldBackup -Force | Out-Null
New-Item -ItemType Directory -Path $pluginBackup -Force | Out-Null
New-Item -ItemType Directory -Path $configBackup -Force | Out-Null

foreach ($world in $worlds) {
    $source = Join-Path $serverDir $world
    if (Test-Path $source) {
        Write-Host ("[WORLD] {0}" -f $world) -ForegroundColor White
        Copy-Item -Path $source -Destination (Join-Path $worldBackup $world) -Recurse -Force
    }
}

foreach ($pluginName in $pluginData) {
    $source = Join-Path $serverDir ("plugins\$pluginName")
    if (Test-Path $source) {
        Write-Host ("[PLUGIN] {0}" -f $pluginName) -ForegroundColor White
        Copy-Item -Path $source -Destination (Join-Path $pluginBackup $pluginName) -Recurse -Force
    }
}

foreach ($fileName in $configFiles) {
    $source = Join-Path $serverDir $fileName
    if (Test-Path $source) {
        Copy-Item -Path $source -Destination (Join-Path $configBackup $fileName) -Force
    }
}

$manifest = @(
    "SkyKings backup created: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "Repository root: $repoRoot",
    "Server root: $serverDir",
    "Git HEAD: $((git -C $repoRoot rev-parse HEAD 2>$null) -join '')"
)
$manifest | Set-Content -Path (Join-Path $backupRoot 'BACKUP_INFO.txt') -Encoding UTF8

Write-Host ''
Write-Host 'Backup fertig.' -ForegroundColor Green
Write-Host 'Vor Release einmal Restore aus diesem Ordner auf einer Testkopie verifizieren.' -ForegroundColor Yellow
