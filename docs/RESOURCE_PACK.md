# SkyKings Resource Pack — 1.8.9

Stand: 2026-09-03

Der Pack ist ein fester Pre-Launch-Baustein, aber keine Gameplay-Abhaengigkeit. Der technische Delivery-/Build-Layer und das erste echte Core-Iconset sind implementiert. Der Build erzeugt aus einer kompakten binären RGBA-Quelle reproduzierbar 19 Legacy-1.8-Itemtexturen plus `pack.png`.

## Status

- Foundation: IMPLEMENTIERT
- Build-ZIP: IMPLEMENTIERT
- CI-Artifact: IMPLEMENTIERT
- Server-Delivery + `/pack`: IMPLEMENTIERT
- Join-Auslieferung + Resend-Cooldown: IMPLEMENTIERT
- `/skcheck` Pack-Diagnose: IMPLEMENTIERT
- Core-Iconset (19 UI-Icons + Logo): IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- GUI-Decorator fuer zentrale SkyKings-Menues: IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- PlayerShop-Villager-Coin-Token auf Coin-Icon: IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- weitere Rank-/Rarity-/Effect-Art: OPTIONALER POLISH
- Client-Runtime-Test: OFFEN
- stabile Produktions-HTTPS-Auslieferung: OFFEN

## 1.8.9-Regeln

- `pack_format: 1`
- kein modernes CustomModelData
- keine modernen JSON-Font-/Glyph-Provider
- Icon-Texturen nur auf bewusst reservierten Vanilla-Materialien
- Schwerter, Bow, Rod, Armor, Ender Pearls, Golden Apples und wichtige PvP-Bloecke werden nicht global ueberschrieben
- jedes Icon besitzt weiterhin einen eindeutigen Itemnamen/Lore-Fallback
- der Pack bleibt optional; keine Business-Logik entscheidet anhand einer Textur

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

Die grafische Quelle liegt zentral in `resource-pack-source/skykings-ui-atlas.rgba.gz`. Sie enthaelt gzip-komprimierte rohe RGBA-Pixel. `scripts/tools/ResourcePackAtlasBuilder.java` rekonstruiert daraus das 160×128-Atlasbild und schreibt die finalen PNGs mit Java 8 selbst. Das verhindert Abhaengigkeiten von Designer-PNG-Decodern und macht denselben Build auf Windows und GitHub Actions reproduzierbar.

`ResourcePackIcon` ist die autoritative Java-Zuordnung der 19 reservierten Materialien. Ein Unit-Test erzwingt exakt 19 eindeutige Slots und blockiert geschuetzte PvP-Kernitems. `ResourcePackGuiDecorator` ersetzt nur dekorative GUI-Materialien und behaelt Namen/Lore. Reward-, PvP- und Kaufitems bleiben unveraendert.

Der virtuelle Coin-Input im echten PlayerShop-Villager-Handel verwendet ebenfalls `GOLD_NUGGET`/Coin-Textur. `NETHER_STAR` bleibt damit eindeutig der SkyKings-Stern.

## Runtime-Gate

Vor groesserem Launch:

- [ ] Pack-ZIP laedt auf frischem Minecraft-1.8.9-Client
- [ ] `pack.png` wird korrekt angezeigt
- [ ] Home / Back / Next korrekt
- [ ] Locked / Ready / Completed korrekt
- [ ] Premium korrekt
- [ ] Coins und SkyKings Stern eindeutig verschieden
- [ ] Battle Pass / Quests / Kits korrekt
- [ ] Crates / Jackpot / Shop / Trade korrekt
- [ ] Clan / Duel / Event korrekt
- [ ] keine Missing-Texture-Flaechen
- [ ] GUI Scale Small/Normal getestet
- [ ] Battle Pass, Quests, Kits, Crates, Commands Hub, Jackpot, Trade und PlayerShop mit Pack lesbar
- [ ] dieselben Systeme ohne Pack voll bedienbar
- [ ] PlayerShop-Villager zeigt im Input das Coin-Icon, niemals den Stern
- [ ] Merchant-Coin-Token kann weiterhin nicht entnommen/verschoben werden
- [ ] normales PvP-Pack bleibt fuer Waffen/Ruestung/Bow/Rod/Pearls/Gapples nutzbar
- [ ] finale ZIP unter stabiler HTTPS-URL erreichbar
- [ ] `resource-pack.enabled: true` + URL setzen
- [ ] `/skcheck` zeigt Pack `[OK]`
- [ ] `/pack` manuell testen
- [ ] Join-Auslieferung nach Relog/Restart testen
- [ ] Ablehnen/deaktivierter Pack: Server bleibt komplett spielbar

## Build

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```

Der Build validiert `pack.mcmeta`, erzeugt alle 19 Pflichttexturen + `pack.png` und bricht ab, sobald eines der erwarteten ZIP-Assets fehlt oder leer ist.
