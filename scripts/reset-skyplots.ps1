$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot "server"
$worldDir = Join-Path $serverDir "SkyPlots"
$coreDataDir = Join-Path $serverDir "plugins\SkyKings-Core"
$plotsFile = Join-Path $coreDataDir "plots.yml"
$bordersFile = Join-Path $coreDataDir "plot-borders.yml"

if (!(Test-Path $serverDir)) {
    throw "Server-Ordner nicht gefunden: $serverDir"
}

# Ein World-/Claim-Reset waehrend Spigot laeuft kann Chunks oder YAML-Daten direkt wieder schreiben.
$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match "spigot-1\.8\.8\.jar" }
if ($runningServer) {
    throw "SkyKings/Spigot laeuft noch. Erst in der Serverkonsole 'stop' eingeben und danach dieses Script erneut starten."
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $serverDir "backups\SkyPlots-reset-$stamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

Write-Host "SkyPlots Reset" -ForegroundColor Cyan
Write-Host "Backup: $backupRoot" -ForegroundColor DarkGray

if (Test-Path $worldDir) {
    Move-Item -Path $worldDir -Destination (Join-Path $backupRoot "SkyPlots")
    Write-Host "[BACKUP] Alte SkyPlots-Welt gesichert." -ForegroundColor Green
} else {
    Write-Host "[INFO] Keine bestehende SkyPlots-Welt gefunden." -ForegroundColor Yellow
}

if (Test-Path $plotsFile) {
    Move-Item -Path $plotsFile -Destination (Join-Path $backupRoot "plots.yml")
    Write-Host "[BACKUP] plots.yml gesichert; Claims starten sauber neu." -ForegroundColor Green
}

if (Test-Path $bordersFile) {
    Move-Item -Path $bordersFile -Destination (Join-Path $backupRoot "plot-borders.yml")
    Write-Host "[BACKUP] plot-borders.yml gesichert; Rand-Test startet sauber neu." -ForegroundColor Green
}

Write-Host "" 
Write-Host "Reset fertig." -ForegroundColor Green
Write-Host "Beim naechsten Serverstart erzeugt SkyKings die Plotwelt neu:" -ForegroundColor White
Write-Host "- 65x65 Grasflaeche = eine Plotzelle" -ForegroundColor DarkGray
Write-Host "- 7 Block Stone-Brick-Strasse = neutral/geschuetzt" -ForegroundColor DarkGray
Write-Host "- freie Plot-Raender = Holzstufe" -ForegroundColor DarkGray
Write-Host "- geclaimter Plot-Rand = Steinstufe" -ForegroundColor DarkGray
Write-Host "" 
Write-Host "Falls etwas unerwartet ist, liegen Welt + Plotdaten unveraendert im Backup-Ordner." -ForegroundColor Yellow
