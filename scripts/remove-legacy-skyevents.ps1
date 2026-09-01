$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$server = Join-Path $root 'server'
$world = Join-Path $server 'SkyEvents'
$backups = Join-Path $server 'backups'

$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'spigot-1\.8\.8\.jar' }
if ($runningServer) {
    throw "SkyEvents kann nicht entfernt werden, solange Spigot laeuft. Erst 'stop' ausfuehren."
}

if (-not (Test-Path $world)) {
    Write-Host '[OK] Kein aktiver SkyEvents-Weltordner vorhanden.' -ForegroundColor Green
    exit 0
}

if (-not (Test-Path $backups)) { New-Item -ItemType Directory -Path $backups | Out-Null }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$target = Join-Path $backups ("SkyEvents-retired-$stamp")

Write-Host 'SkyEvents wird aus dem aktiven Serverbestand entfernt.' -ForegroundColor Yellow
Write-Host ("Backup: {0}" -f $target) -ForegroundColor Gray
Move-Item -Path $world -Destination $target
Write-Host '[OK] SkyEvents wurde ins Backup verschoben und ist nicht mehr aktiv.' -ForegroundColor Green
Write-Host 'Duel/LMS/Clan-War-Arenapunkte, die noch auf SkyEvents zeigen, bitte ingame neu setzen.' -ForegroundColor Yellow
