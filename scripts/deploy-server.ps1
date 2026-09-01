$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverPlugins = Join-Path $repoRoot 'server\plugins'

$modules = @(
    'SkyKings-Core',
    'SkyKings-Combat',
    'SkyKings-Crates',
    'SkyKings-Admin'
)

Write-Host '== SkyKings Deploy ==' -ForegroundColor Cyan

$runningServer = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'spigot-1\.8\.8\.jar' }
if ($runningServer) {
    throw "SkyKings/Spigot laeuft noch. Erst in der Serverkonsole 'stop' eingeben und danach erneut deployen."
}

Set-Location $repoRoot

Write-Host '1/3 Maven Tests + Build...' -ForegroundColor Cyan
& mvn clean package
if ($LASTEXITCODE -ne 0) {
    throw "Maven Build fehlgeschlagen (ExitCode $LASTEXITCODE)."
}

if (-not (Test-Path $serverPlugins)) {
    New-Item -ItemType Directory -Path $serverPlugins -Force | Out-Null
}

Write-Host '2/3 Alte SkyKings-JARs entfernen...' -ForegroundColor Cyan
Get-ChildItem -Path $serverPlugins -Filter 'SkyKings-*.jar' -File -ErrorAction SilentlyContinue | Remove-Item -Force

Write-Host '3/3 Neue Module kopieren...' -ForegroundColor Cyan
foreach ($module in $modules) {
    $targetDir = Join-Path $repoRoot ("plugins\$module\target")
    $jar = Get-ChildItem -Path $targetDir -Filter "$module-*.jar" -File |
        Where-Object { $_.Name -notmatch '(-sources|-javadoc|original-)' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Keine gebaute JAR fuer $module in $targetDir gefunden."
    }

    Copy-Item -Path $jar.FullName -Destination (Join-Path $serverPlugins $jar.Name) -Force
    Write-Host ("  OK  {0}" -f $jar.Name) -ForegroundColor Green
}

Write-Host ''
Write-Host 'Deploy fertig. Tests, Compile und JAR-Kopie waren erfolgreich.' -ForegroundColor Green
Write-Host 'LuckPerms/Vault wurden nicht angefasst.' -ForegroundColor DarkGray
Write-Host 'Jetzt den Server normal starten und den Boot-Log bis "Done" pruefen.' -ForegroundColor Green
