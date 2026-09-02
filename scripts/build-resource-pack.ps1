$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$packRoot = Join-Path $repoRoot 'resource-pack'
$buildRoot = Join-Path $repoRoot 'build\resource-pack'
$stageRoot = Join-Path $buildRoot 'stage'
$outputZip = Join-Path $buildRoot 'SkyKings-ResourcePack-1.8.9.zip'

function Fail($text) { throw ("[FEHLER] {0}" -f $text) }
function Ok($text) { Write-Host ("[OK] {0}" -f $text) -ForegroundColor Green }

if (-not (Test-Path $packRoot)) { Fail 'resource-pack Verzeichnis fehlt.' }
$mcmeta = Join-Path $packRoot 'pack.mcmeta'
if (-not (Test-Path $mcmeta)) { Fail 'resource-pack\pack.mcmeta fehlt.' }

try {
    $meta = Get-Content $mcmeta -Raw | ConvertFrom-Json
} catch {
    Fail ("pack.mcmeta ist kein gueltiges JSON: {0}" -f $_.Exception.Message)
}

if ($null -eq $meta.pack) { Fail 'pack.mcmeta enthaelt keinen pack-Block.' }
if ([int]$meta.pack.pack_format -ne 1) { Fail ("Minecraft 1.8.9 erwartet pack_format 1, gefunden: {0}" -f $meta.pack.pack_format) }
Ok 'pack.mcmeta ist fuer Minecraft 1.8.9 gueltig'

if (Test-Path $stageRoot) { Remove-Item $stageRoot -Recurse -Force }
New-Item -ItemType Directory -Path $stageRoot -Force | Out-Null
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null

Copy-Item $mcmeta (Join-Path $stageRoot 'pack.mcmeta') -Force

$packIcon = Join-Path $packRoot 'pack.png'
if (Test-Path $packIcon) { Copy-Item $packIcon (Join-Path $stageRoot 'pack.png') -Force }

$assets = Join-Path $packRoot 'assets'
if (Test-Path $assets) { Copy-Item $assets (Join-Path $stageRoot 'assets') -Recurse -Force }

# Entwicklungsdateien duerfen niemals im ausgelieferten Pack landen.
Get-ChildItem $stageRoot -Recurse -Force -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -in @('.gitkeep', 'README.md', 'Thumbs.db', '.DS_Store') } |
    Remove-Item -Force

if (Test-Path $outputZip) { Remove-Item $outputZip -Force }
Compress-Archive -Path (Join-Path $stageRoot '*') -DestinationPath $outputZip -CompressionLevel Optimal

if (-not (Test-Path $outputZip)) { Fail 'Resource-Pack ZIP wurde nicht erzeugt.' }

# ZIP Root grob verifizieren. pack.mcmeta muss direkt im Root liegen.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($outputZip)
try {
    $rootMeta = $zip.Entries | Where-Object { $_.FullName -eq 'pack.mcmeta' } | Select-Object -First 1
    if ($null -eq $rootMeta) { Fail 'pack.mcmeta liegt nicht direkt im ZIP-Root.' }
} finally {
    $zip.Dispose()
}

Remove-Item $stageRoot -Recurse -Force
Ok ("Resource-Pack gebaut: {0}" -f $outputZip)
