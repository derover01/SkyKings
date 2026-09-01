$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$errors = New-Object System.Collections.Generic.List[string]

function Require-Path([string]$Path) {
    if (-not (Test-Path $Path)) { $errors.Add("Fehlt: $Path") }
}

function Forbid-Path([string]$Path) {
    if (Test-Path $Path) { $errors.Add("Darf nicht mehr existieren: $Path") }
}

Write-Host 'SkyKings static release audit' -ForegroundColor Cyan

# Release modules / core documentation must always exist.
Require-Path 'plugins/SkyKings-Core/pom.xml'
Require-Path 'plugins/SkyKings-Combat/pom.xml'
Require-Path 'plugins/SkyKings-Crates/pom.xml'
Require-Path 'plugins/SkyKings-Admin/pom.xml'
Require-Path 'docs/UI_UX_DESIGN_SYSTEM.md'
Require-Path 'docs/CUSTOM_PANEL_UI_STANDARD.md'
Require-Path 'docs/NEXT_PC_CHECKLIST.md'
Require-Path 'docs/ROADMAP_STATUS.md'
Require-Path 'scripts/backup-server.ps1'
Require-Path 'scripts/restore-server-backup.ps1'
Require-Path 'scripts/reset-skyplots.ps1'

# Tournament/Juggernaut are intentionally NOT part of the final SkyKings feature set.
Forbid-Path 'plugins/SkyKings-Combat/src/main/java/net/skykings/combat/event/TournamentCommand.java'
Forbid-Path 'plugins/SkyKings-Combat/src/main/java/net/skykings/combat/event/TournamentService.java'
Forbid-Path 'plugins/SkyKings-Combat/src/main/java/net/skykings/combat/event/JuggernautCommand.java'
Forbid-Path 'plugins/SkyKings-Combat/src/main/java/net/skykings/combat/event/JuggernautService.java'

$combatPlugin = Get-Content 'plugins/SkyKings-Combat/src/main/resources/plugin.yml' -Raw
if ($combatPlugin -match '(?m)^\s{2}tournament\s*:') { $errors.Add('/tournament ist wieder in Combat plugin.yml registriert.') }
if ($combatPlugin -match '(?m)^\s{2}juggernaut\s*:') { $errors.Add('/juggernaut ist wieder in Combat plugin.yml registriert.') }

$commandsGui = Get-Content 'plugins/SkyKings-Core/src/main/java/net/skykings/core/command/CommandsGui.java' -Raw
if ($commandsGui -match '(?i)/tournament|turnier') { $errors.Add('Tournament/Turnier darf nicht mehr in der Spieler-Commands-UI auftauchen.') }
if ($commandsGui -match '(?i)/juggernaut|juggernaut') { $errors.Add('Juggernaut darf nicht mehr in der Spieler-Commands-UI auftauchen.') }

# CI must execute tests. Accidentally reintroducing skipTests makes release gates meaningless.
$workflow = Get-Content '.github/workflows/build.yml' -Raw
if ($workflow -match '-DskipTests') { $errors.Add('CI darf Maven-Tests nicht mit -DskipTests ueberspringen.') }

# Do not commit an actual Discord token assignment anywhere in tracked text files.
$trackedText = Get-ChildItem -Recurse -File -Include *.java,*.yml,*.yaml,*.md,*.ps1,*.xml |
    Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' }
foreach ($file in $trackedText) {
    $text = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($null -ne $text -and $text -match '(?im)^\s*SKYKINGS_DISCORD_BOT_TOKEN\s*=\s*[^\s$<]') {
        $errors.Add("Moegliches fest eingetragenes Discord-Token: $($file.FullName)")
    }
}

if ($errors.Count -gt 0) {
    Write-Host ''
    Write-Host 'RELEASE AUDIT FAILED' -ForegroundColor Red
    foreach ($errorMessage in $errors) { Write-Host " - $errorMessage" -ForegroundColor Red }
    exit 1
}

Write-Host 'RELEASE AUDIT OK' -ForegroundColor Green
Write-Host 'Finale Eventauswahl, Tests, Kernmodule und Release-Dateien sind statisch konsistent.'
