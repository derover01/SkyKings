# SkyKings Resource Pack — 1.8.9

Stand: 2026-09-03

Der Pack ist ein fester Pre-Launch-Baustein, aber keine Gameplay-Abhaengigkeit. Der technische Delivery-/Build-Layer ist implementiert; der erste echte Core-Icon-Satz liegt jetzt als zentraler Atlas vor und wird beim Build in Legacy-1.8-Itemtexturen zerlegt.

## Status

- Foundation: IMPLEMENTIERT
- Build-ZIP: IMPLEMENTIERT
- CI-Artifact: IMPLEMENTIERT
- Server-Delivery + `/pack`: IMPLEMENTIERT
- `/skcheck` Pack-Diagnose: IMPLEMENTIERT
- Core-Iconset (19 UI-Icons + Logo): IMPLEMENTIERT / RUNTIME-TEST
- weitere Rank-/Rarity-/Effect-Art: POLISH / OFFEN
- Client-Runtime-Test: OFFEN
- stabile HTTPS-Auslieferung: OFFEN

## 1.8.9-Regeln

- `pack_format: 1`
- kein modernes CustomModelData
- keine modernen JSON-Font-/Glyph-Provider
- Icon-Texturen nur auf bewusst reservierten Vanilla-Materialien
- Schwerter, Bow, Rod, Armor, Ender Pearls, Golden Apples und wichtige PvP-Bloecke nicht global ueberschreiben
- jedes Icon braucht weiterhin einen eindeutigen Itemnamen/Lore-Fallback

## Verbindliches Asset-Manifest

| Bereich | Icon | Material | Legacy PNG | Runtime-Status |
|---|---|---|---|---|
| Navigation | Home | `MINECART` | `minecart_normal.png` | testen |
| Navigation | Back | `POWERED_MINECART` | `minecart_furnace.png` | testen |
| Navigation | Next | `HOPPER_MINECART` | `minecart_hopper.png` | testen |
| State | Locked | `BARRIER` | `barrier.png` | testen |
| State | Ready | `SLIME_BALL` | `slimeball.png` | testen |
| State | Completed | `FIREWORK` | `fireworks.png` | testen |
| Premium | Premium | `EYE_OF_ENDER` | `ender_eye.png` | testen |
| Economy | Coins | `GOLD_NUGGET` | `gold_nugget.png` | testen |
| Economy | SkyKings Star | `NETHER_STAR` | `nether_star.png` | testen |
| Progression | Battle Pass | `EMPTY_MAP` | `map_empty.png` | testen |
| Progression | Quest | `BOOK_AND_QUILL` | `book_writable.png` | testen |
| Progression | Kit | `STORAGE_MINECART` | `minecart_chest.png` | testen |
| Economy | Crates | `COMMAND_MINECART` | `minecart_command_block.png` | testen |
| Economy | Jackpot | `DIODE` | `repeater.png` | testen |
| Economy | Shop | `HOPPER` | `hopper.png` | testen |
| Economy | Trade | `NAME_TAG` | `name_tag.png` | testen |
| Social | Clan | `WRITTEN_BOOK` | `book_written.png` | testen |
| Event | Duel | `SHEARS` | `shears.png` | testen |
| Event | Event | `MAGMA_CREAM` | `magma_cream.png` | testen |
| Branding | SkyKings Logo | Atlas tile 20 | `pack.png` | testen |

Die grafische Quelle liegt zentral in `resource-pack-source/skykings-ui-atlas.rgba.gz.b64`. Sie enthaelt gzip-komprimierte rohe RGBA-Pixel als Base64-Text. `scripts/tools/ResourcePackAtlasBuilder.java` rekonstruiert daraus das 160x128-Atlasbild und schreibt die finalen PNGs selbst. So ist der Build unabhaengig von PNG-Decoder-Unterschieden zwischen Java-Versionen.

## Runtime-Gate

Vor groesserem Launch:

- [ ] Pack-ZIP laedt auf frischem 1.8.9-Client
- [ ] alle 19 reservierten Icons zeigen die erwartete SkyKings-Grafik
- [ ] keine Missing-Texture-Flaechen
- [ ] GUI Scale Small/Normal getestet
- [ ] Battle Pass, Quests, Kits, Crates und Commands Hub mit Pack lesbar
- [ ] dieselben Systeme ohne Pack voll bedienbar
- [ ] PlayerShop/Trade funktionieren mit und ohne Pack
- [ ] normales PvP-Pack bleibt fuer Waffen/Ruestung nutzbar
- [ ] finale ZIP unter stabiler HTTPS-URL erreichbar
- [ ] `resource-pack.enabled: true` + URL setzen
- [ ] `/skcheck` zeigt Pack `[OK]`
- [ ] `/pack` manuell testen
- [ ] Join-Auslieferung nach Relog/Restart testen

## Build

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```
