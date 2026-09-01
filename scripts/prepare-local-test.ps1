param(
    [switch]$ResetSkyPlots
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Run-Step([string]$Name, [scriptblock]$Action) {
    Write-Host ''
    Write-Host "== $Name ==" -ForegroundColor Cyan
    & $Action
    if (-not $?) { throw "$Name ist fehlgeschlagen." }
}

Write-Host 'SkyKings Local Test Preparation' -ForegroundColor Yellow
Write-Host 'Server muss vorher mit "stop" vollstaendig beendet sein.' -ForegroundColor Yellow

Run-Step 'Release Preflight' { & "$PSScriptRoot\release-preflight.ps1" }
Run-Step 'Retired SkyEvents Cleanup' { & "$PSScriptRoot\remove-legacy-skyevents.ps1" }
Run-Step 'Test, Build & Deploy' { & "$PSScriptRoot\deploy-server.ps1" }

if ($ResetSkyPlots) {
    Write-Host ''
    Write-Host 'SkyPlots Reset wurde EXPLIZIT angefordert.' -ForegroundColor Yellow
    Run-Step 'Backup + SkyPlots Reset' { & "$PSScriptRoot\reset-skyplots.ps1" }
} else {
    Write-Host ''
    Write-Host 'SkyPlots bleibt unveraendert.' -ForegroundColor DarkGray
    Write-Host 'Fuer einen bewussten frischen Raster-Test: .\scripts\prepare-local-test.ps1 -ResetSkyPlots' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host 'VORBEREITUNG FERTIG' -ForegroundColor Green
Write-Host 'Server starten:' -ForegroundColor White
Write-Host '  cd .\server' -ForegroundColor Gray
Write-Host '  java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui' -ForegroundColor Gray
Write-Host ''
Write-Host 'Nach "Done" ingame zuerst: /skcheck und /skymap list' -ForegroundColor White
