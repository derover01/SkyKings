$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host '== SkyKings Release Preflight ==' -ForegroundColor Cyan

function Ok($text) { Write-Host ("[OK] {0}" -f $text) -ForegroundColor Green }
function Warn($text) { Write-Host ("[WARN] {0}" -f $text) -ForegroundColor Yellow }
function Fail($text) { throw ("[FEHLER] {0}" -f $text) }

# 1) Umgebung
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail 'git wurde nicht gefunden.' }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Fail 'mvn wurde nicht gefunden. Maven-PATH fuer diese PowerShell setzen.' }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Fail 'java wurde nicht gefunden.' }
Ok 'git, mvn und java verfuegbar'

$javaVersion = (& java -version 2>&1 | Select-Object -First 1) -join ''
if ($javaVersion -notmatch '1\.8\.') { Fail ("Java 8 erwartet, gefunden: $javaVersion") }
Ok ("Java 8: {0}" -f $javaVersion)

$mvnVersion = (& mvn -version 2>&1 | Select-Object -First 1) -join ''
Ok $mvnVersion

# 2) Server darf nicht laufen
$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'spigot-1\.8\.8\.jar' }
if ($runningServer) { Fail "Spigot laeuft noch. Erst 'stop' in der Serverkonsole." }
Ok 'Spigot ist gestoppt'

# 3) Repository-Zustand
$branch = (& git rev-parse --abbrev-ref HEAD).Trim()
if ($branch -ne 'main') { Warn ("Aktueller Branch ist '$branch', erwartet wird main.") } else { Ok 'Git Branch main' }

$status = & git status --porcelain
if ($status) {
    Warn 'Lokale uncommittete Aenderungen vorhanden. Vor Deploy bewusst pruefen.'
    $status | ForEach-Object { Write-Host ("      {0}" -f $_) -ForegroundColor DarkGray }
} else {
    Ok 'Working Tree sauber'
}

# 4) Kritische Dateien
$required = @(
    'pom.xml',
    'server\spigot-1.8.8.jar',
    'scripts\deploy-server.ps1',
    'scripts\backup-server.ps1',
    'scripts\reset-skyplots.ps1',
    'docs\NEXT_PC_CHECKLIST.md',
    'docs\ROADMAP_STATUS.md'
)
foreach ($relative in $required) {
    if (-not (Test-Path (Join-Path $repoRoot $relative))) { Fail ("Fehlt: $relative") }
}
Ok 'Kritische Projektdateien vorhanden'

# 5) Vollstaendiger CI-aehnlicher lokaler Gate
Write-Host ''
Write-Host 'Maven Tests + Build...' -ForegroundColor Cyan
& mvn clean package
if ($LASTEXITCODE -ne 0) { Fail ("Maven Build fehlgeschlagen (ExitCode $LASTEXITCODE).") }
Ok 'Alle Maven Tests + Builds erfolgreich'

# 6) Erwartete vier Module
$modules = @('SkyKings-Core','SkyKings-Combat','SkyKings-Crates','SkyKings-Admin')
foreach ($module in $modules) {
    $target = Join-Path $repoRoot ("plugins\$module\target")
    $jar = Get-ChildItem -Path $target -Filter "$module-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '(-sources|-javadoc|original-)' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $jar) { Fail ("Keine Release-JAR fuer $module gefunden.") }
    Ok ("{0}: {1}" -f $module, $jar.Name)
}

Write-Host ''
Write-Host 'PRE-FLIGHT GRUEN' -ForegroundColor Green
Write-Host 'Naechster Schritt:' -ForegroundColor Cyan
Write-Host '  .\scripts\deploy-server.ps1' -ForegroundColor White
Write-Host 'Danach bei frischem Plot-Test einmalig:' -ForegroundColor Cyan
Write-Host '  .\scripts\reset-skyplots.ps1' -ForegroundColor White
Write-Host 'Dann Server starten und ingame /skcheck ausfuehren.' -ForegroundColor White
