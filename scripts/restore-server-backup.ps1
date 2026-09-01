param(
    [Parameter(Mandatory=$true)]
    [string]$BackupPath,

    [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot 'server'

if (-not $ConfirmRestore) {
    throw "Restore ist gesperrt. Nur bewusst mit -ConfirmRestore ausfuehren."
}
if (-not (Test-Path $serverDir)) {
    throw "Server-Ordner nicht gefunden: $serverDir"
}

$resolvedBackup = (Resolve-Path $BackupPath -ErrorAction Stop).Path
$manifest = Join-Path $resolvedBackup 'BACKUP_INFO.txt'
if (-not (Test-Path $manifest)) {
    throw "Kein gueltiges SkyKings-Backup: BACKUP_INFO.txt fehlt in $resolvedBackup"
}

$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'spigot-1\.8\.8\.jar' }
if ($runningServer) {
    throw "SkyKings/Spigot laeuft noch. Erst 'stop' in der Serverkonsole eingeben."
}

Write-Host '== SkyKings Server Restore ==' -ForegroundColor Yellow
Write-Host ("Quelle: {0}" -f $resolvedBackup) -ForegroundColor DarkGray
Write-Host 'Vor dem Restore wird der aktuelle Serverzustand automatisch gesichert.' -ForegroundColor Yellow

# PowerShell-Scripts signalisieren Fehler ueber Exceptions/terminating errors, nicht verlaesslich ueber LASTEXITCODE.
& (Join-Path $PSScriptRoot 'backup-server.ps1')
Write-Host '[OK] Safety-Backup erstellt.' -ForegroundColor Green

$worldsDir = Join-Path $resolvedBackup 'worlds'
$pluginsDir = Join-Path $resolvedBackup 'plugins'
$configDir = Join-Path $resolvedBackup 'config'

$worldNames = @(
    'world', 'world_nether', 'world_the_end',
    'SkyPvP', 'SkyPlots', 'SkyIslands', 'SkyEvents', 'SkyCommunityEvent'
)
$pluginNames = @(
    'SkyKings-Core', 'SkyKings-Combat', 'SkyKings-Crates', 'SkyKings-Admin',
    'LuckPerms', 'Vault'
)

foreach ($world in $worldNames) {
    $source = Join-Path $worldsDir $world
    if (-not (Test-Path $source)) { continue }
    $target = Join-Path $serverDir $world
    if (Test-Path $target) { Remove-Item -Path $target -Recurse -Force }
    Copy-Item -Path $source -Destination $target -Recurse -Force
    Write-Host ("[RESTORE WORLD] {0}" -f $world) -ForegroundColor Green
}

foreach ($pluginName in $pluginNames) {
    $source = Join-Path $pluginsDir $pluginName
    if (-not (Test-Path $source)) { continue }
    $target = Join-Path $serverDir ("plugins\$pluginName")
    if (Test-Path $target) { Remove-Item -Path $target -Recurse -Force }
    Copy-Item -Path $source -Destination $target -Recurse -Force
    Write-Host ("[RESTORE PLUGIN] {0}" -f $pluginName) -ForegroundColor Green
}

if (Test-Path $configDir) {
    Get-ChildItem -Path $configDir -File | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $serverDir $_.Name) -Force
        Write-Host ("[RESTORE CONFIG] {0}" -f $_.Name) -ForegroundColor Green
    }
}

Write-Host ''
Write-Host 'Restore fertig.' -ForegroundColor Green
Write-Host 'Jetzt Server starten, Boot-Log pruefen und ingame /skcheck ausfuehren.' -ForegroundColor Cyan
Write-Host 'Danach Coins, Clans, Plots, Battle Pass, Quests und Eventdaten stichprobenartig vergleichen.' -ForegroundColor Cyan
