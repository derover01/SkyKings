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

# java -version schreibt seine normale Versionsausgabe auf STDERR. Unter Windows PowerShell 5.1
# wird das mit ErrorActionPreference=Stop sonst faelschlich als NativeCommandError behandelt.
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaOutput = & java -version 2>&1
$javaExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
if ($javaExitCode -ne 0) { Fail ("java -version fehlgeschlagen (ExitCode $javaExitCode).") }
$javaVersion = ($javaOutput | Select-Object -First 1).ToString()
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
    'scripts\build-resource-pack.ps1',
    'resource-pack\pack.mcmeta',
    'resource-pack\README.md',
    'docs\RESOURCE_PACK.md',
    'docs\NEXT_PC_CHECKLIST.md',
    'docs\NEXT_RUNTIME_CHECKLIST.md',
    'docs\ROADMAP_STATUS.md'
)
foreach ($relative in $required) {
    if (-not (Test-Path (Join-Path $repoRoot $relative))) { Fail ("Fehlt: $relative") }
}
Ok 'Kritische Projektdateien inklusive Resource-Pack vorhanden'

# 5) Resource-Pack Gate
Write-Host ''
Write-Host 'Minecraft 1.8.9 Resource-Pack bauen...' -ForegroundColor Cyan
& (Join-Path $PSScriptRoot 'build-resource-pack.ps1')
if ($LASTEXITCODE -ne 0) { Fail ("Resource-Pack Build fehlgeschlagen (ExitCode $LASTEXITCODE).") }
$packZip = Join-Path (Join-Path (Join-Path $repoRoot 'build') 'resource-pack') 'SkyKings-ResourcePack-1.8.9.zip'
if (-not (Test-Path $packZip)) { Fail 'Resource-Pack ZIP fehlt nach Build.' }
Ok ("Resource-Pack Release-Artefakt: {0}" -f $packZip)

# 6) Vollstaendiger CI-aehnlicher lokaler Gate
Write-Host ''
Write-Host 'Maven Tests + Build...' -ForegroundColor Cyan
& mvn clean package
if ($LASTEXITCODE -ne 0) { Fail ("Maven Build fehlgeschlagen (ExitCode $LASTEXITCODE).") }
Ok 'Alle Maven Tests + Builds erfolgreich'

# 7) Erwartete vier Module
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
Write-Host 'Release-Artefakte:' -ForegroundColor Cyan
Write-Host '  - vier SkyKings Plugin-JARs' -ForegroundColor White
Write-Host '  - build\resource-pack\SkyKings-ResourcePack-1.8.9.zip' -ForegroundColor White
Write-Host 'Naechster Schritt:' -ForegroundColor Cyan
Write-Host '  .\scripts\deploy-server.ps1' -ForegroundColor White
Write-Host 'Danach bei frischem Plot-Test einmalig:' -ForegroundColor Cyan
Write-Host '  .\scripts\reset-skyplots.ps1' -ForegroundColor White
Write-Host 'Dann Server starten, ingame /skcheck ausfuehren und Runtime-Checklist abarbeiten.' -ForegroundColor White
