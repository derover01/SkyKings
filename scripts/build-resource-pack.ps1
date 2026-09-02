$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$packRoot = Join-Path $repoRoot 'resource-pack'
$packSourceRoot = Join-Path $repoRoot 'resource-pack-source'
$atlas = Join-Path $packSourceRoot 'skykings-ui-atlas.png'
$atlasTool = Join-Path (Join-Path $PSScriptRoot 'tools') 'ResourcePackAtlasBuilder.java'
$buildRoot = Join-Path (Join-Path $repoRoot 'build') 'resource-pack'
$stageRoot = Join-Path $buildRoot 'stage'
$toolBuild = Join-Path $buildRoot 'atlas-tool'
$outputZip = Join-Path $buildRoot 'SkyKings-ResourcePack-1.8.9.zip'

function Fail($text) { throw ("[FEHLER] {0}" -f $text) }
function Ok($text) { Write-Host ("[OK] {0}" -f $text) -ForegroundColor Green }

if (-not (Test-Path $packRoot)) { Fail 'resource-pack Verzeichnis fehlt.' }
$mcmeta = Join-Path $packRoot 'pack.mcmeta'
if (-not (Test-Path $mcmeta)) { Fail 'resource-pack/pack.mcmeta fehlt.' }
if (-not (Test-Path $atlas)) { Fail 'resource-pack-source/skykings-ui-atlas.png fehlt.' }
if (-not (Test-Path $atlasTool)) { Fail 'scripts/tools/ResourcePackAtlasBuilder.java fehlt.' }

try {
    $meta = Get-Content $mcmeta -Raw | ConvertFrom-Json
} catch {
    Fail ("pack.mcmeta ist kein gueltiges JSON: {0}" -f $_.Exception.Message)
}

if ($null -eq $meta.pack) { Fail 'pack.mcmeta enthaelt keinen pack-Block.' }
if ([int]$meta.pack.pack_format -ne 1) { Fail ("Minecraft 1.8.9 erwartet pack_format 1, gefunden: {0}" -f $meta.pack.pack_format) }
Ok 'pack.mcmeta ist fuer Minecraft 1.8.9 gueltig'

if (Test-Path $stageRoot) { Remove-Item $stageRoot -Recurse -Force }
if (Test-Path $toolBuild) { Remove-Item $toolBuild -Recurse -Force }
New-Item -ItemType Directory -Path $stageRoot -Force | Out-Null
New-Item -ItemType Directory -Path $toolBuild -Force | Out-Null
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null

Copy-Item $mcmeta (Join-Path $stageRoot 'pack.mcmeta') -Force

$assets = Join-Path $packRoot 'assets'
if (Test-Path $assets) { Copy-Item $assets (Join-Path $stageRoot 'assets') -Recurse -Force }

# The real SkyKings icon textures are generated from one committed atlas. This keeps the
# art source centralized and makes Windows + Linux CI produce the same 1.8.x filenames.
& javac -encoding UTF-8 -d $toolBuild $atlasTool
if ($LASTEXITCODE -ne 0) { Fail 'ResourcePackAtlasBuilder konnte nicht kompiliert werden.' }
& java -cp $toolBuild ResourcePackAtlasBuilder $atlas $stageRoot
if ($LASTEXITCODE -ne 0) { Fail 'SkyKings UI-Atlas konnte nicht in Pack-Texturen aufgeteilt werden.' }
Ok 'SkyKings UI-Atlas in echte 1.8.x Item-Texturen umgewandelt'

# Entwicklungsdateien duerfen niemals im ausgelieferten Pack landen.
Get-ChildItem $stageRoot -Recurse -Force -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -in @('.gitkeep', 'README.md', 'Thumbs.db', '.DS_Store') } |
    Remove-Item -Force

if (Test-Path $outputZip) { Remove-Item $outputZip -Force }
Compress-Archive -Path (Join-Path $stageRoot '*') -DestinationPath $outputZip -CompressionLevel Optimal

if (-not (Test-Path $outputZip)) { Fail 'Resource-Pack ZIP wurde nicht erzeugt.' }

# ZIP-Root und Pflichtassets verifizieren. pack.mcmeta/pack.png muessen direkt im Root liegen.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($outputZip)
try {
    $required = @(
        'pack.mcmeta',
        'pack.png',
        'assets/minecraft/textures/items/minecart_normal.png',
        'assets/minecraft/textures/items/barrier.png',
        'assets/minecraft/textures/items/gold_nugget.png',
        'assets/minecraft/textures/items/nether_star.png',
        'assets/minecraft/textures/items/map_empty.png',
        'assets/minecraft/textures/items/minecart_command_block.png',
        'assets/minecraft/textures/items/name_tag.png',
        'assets/minecraft/textures/items/magma_cream.png'
    )
    foreach ($entryName in $required) {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $entryName } | Select-Object -First 1
        if ($null -eq $entry) { Fail ("Pflichtasset fehlt im ZIP: {0}" -f $entryName) }
    }
} finally {
    $zip.Dispose()
}

Remove-Item $stageRoot -Recurse -Force
Remove-Item $toolBuild -Recurse -Force
Ok ("Resource-Pack gebaut: {0}" -f $outputZip)
