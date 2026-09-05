# SkyKings Resource Pack — 1.8.9

Stand: 2026-09-05

Der Pack ist ein fester Pre-Launch-Baustein, aber keine Gameplay-Abhaengigkeit. Delivery, reproduzierbarer Build, Core-Iconset, zentrale GUI-Verdrahtung und ein fester HTTPS-Distributionspfad sind implementiert. Der Build erzeugt aus einer kompakten binaeren RGBA-Quelle reproduzierbar 19 Legacy-1.8-Itemtexturen plus `pack.png`.

## Status

- Foundation: IMPLEMENTIERT
- Build-ZIP: IMPLEMENTIERT
- CI-Artifact: IMPLEMENTIERT
- Server-Delivery + `/pack`: IMPLEMENTIERT
- Join-Auslieferung + Resend-Cooldown: IMPLEMENTIERT
- `/skcheck` Pack-Diagnose: IMPLEMENTIERT
- Core-Iconset (19 UI-Icons + Logo): IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- zentraler GUI-Decorator ueber `GuiManager.open()`: IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- Material-Kollisionsaudit ueber aktuelle Core/Combat/Crates/Admin-JARs: IMPLEMENTIERT
- Java↔Atlas Build-Sync-Gate: IMPLEMENTIERT
- PlayerShop-Villager-Coin-Token auf Coin-Icon: IMPLEMENTIERT / CLIENT-RUNTIME-TEST
- feste Distribution `dist/SkyKings-ResourcePack-1.8.9.zip`: IMPLEMENTIERT
- direkte HTTPS-URL in Core-Vorlage vorkonfiguriert: IMPLEMENTIERT, `enabled: false` bis Clienttest
- weitere Rank-/Rarity-/Effect-Art: OPTIONALER POLISH
- Client-Runtime-Test: OFFEN

## Produktionspfad

Die aktuell freigegebene ZIP liegt versioniert im oeffentlichen Repository unter:

```text
dist/SkyKings-ResourcePack-1.8.9.zip
```

Die Core-Vorlage verwendet dafuer bereits den direkten HTTPS-Pfad:

```text
https://raw.githubusercontent.com/derover01/SkyKings/main/dist/SkyKings-ResourcePack-1.8.9.zip
```

Die URL ist ASCII-only, HTTPS, ohne Fragment und mit 94 Zeichen deutlich unter dem Legacy-Limit von 255 Zeichen. `resource-pack.enabled` bleibt trotzdem bewusst `false`, bis ein frischer echter Minecraft-1.8.9-Client den Download, das Laden und alle Kern-GUIs erfolgreich getestet hat.

## 1.8.9-Regeln

- `pack_format: 1`
- kein modernes CustomModelData
- keine modernen JSON-Font-/Glyph-Provider
- Icon-Texturen nur auf bewusst reservierten Vanilla-Materialien
- Schwerter, Bow, Rod, Armor, Ender Pearls, Golden Apples und wichtige PvP-Bloecke werden nicht global ueberschrieben
- jedes Icon besitzt weiterhin einen eindeutigen Itemnamen/Lore-Fallback
- der Pack bleibt optional; keine Business-Logik entscheidet anhand einer Textur
- `GOLD_NUGGET` und `NETHER_STAR` sind bewusst semantische globale Server-Items fuer Coins bzw. SkyKings-Sterne
- die uebrigen Pack-Slots duerfen im aktuellen Servercode keine zweite Gameplay-Bedeutung besitzen

## Verbindliches Asset-Manifest

| Bereich | Icon | Material | Legacy PNG | Runtime-Status |
|---|---|---|---|---|
| Navigation | Home | `MINECART` | `minecart_normal.png` | testen |
| Navigation | Back | `POWERED_MINECART` | `minecart_furnace.png` | testen |
| Navigation | Next | `HOPPER_MINECART` | `minecart_hopper.png` | testen |
| State | Locked | `BARRIER` | `barrier.png` | testen |
| State | Ready | `SLIME_BALL` | `slimeball.png` | testen |
| State | Completed | `GHAST_TEAR` | `ghast_tear.png` | testen |
| Premium | Premium | `PRISMARINE_CRYSTALS` | `prismarine_crystals.png` | testen |
| Economy | Coins | `GOLD_NUGGET` | `gold_nugget.png` | testen |
| Economy | SkyKings Star | `NETHER_STAR` | `nether_star.png` | testen |
| Progression | Battle Pass | `EMPTY_MAP` | `map_empty.png` | testen |
| Progression | Quest | `PRISMARINE_SHARD` | `prismarine_shard.png` | testen |
| Progression | Kit | `STORAGE_MINECART` | `minecart_chest.png` | testen |
| Economy | Crates | `COMMAND_MINECART` | `minecart_command_block.png` | testen |
| Economy | Jackpot | `DIODE` | `repeater.png` | testen |
| Economy | Shop | `CARROT_STICK` | `carrot_on_a_stick.png` | testen |
| Economy | Trade | `FIREWORK_CHARGE` | `firework_charge.png` | testen |
| Social | Clan | `WRITTEN_BOOK` | `book_written.png` | testen |
| Event | Duel | `SHEARS` | `shears.png` | testen |
| Event | Event | `MAGMA_CREAM` | `magma_cream.png` | testen |
| Branding | SkyKings Logo | Atlas tile 20 | `pack.png` | testen |

Die grafische Quelle liegt zentral in `resource-pack-source/skykings-ui-atlas.rgba.gz`. Sie enthaelt gzip-komprimierte rohe RGBA-Pixel. `scripts/tools/ResourcePackAtlasBuilder.java` rekonstruiert daraus das 160x128-Atlasbild und schreibt die finalen PNGs mit Java 8 selbst. Das verhindert Abhaengigkeiten von Designer-PNG-Decodern und macht denselben Build auf Windows und GitHub Actions reproduzierbar.

`ResourcePackIcon` ist die autoritative Java-Zuordnung der 19 reservierten Materialien. Der Unit-Test erzwingt exakt 19 eindeutige Slots, blockiert geschuetzte PvP-Kernitems und bekannte Shared-Material-Kollisionen. `build-resource-pack.ps1` prueft dieselben 19 Bindings gegen den Build, bevor das ZIP erzeugt wird.

`ResourcePackGuiDecorator` ersetzt nur dekorative GUI-Materialien und behaelt Namen/Lore. Der zentrale `GuiManager.open()` ruft den Decorator unmittelbar vor dem Oeffnen des Inventars auf; die Klicklogik bleibt slot-basiert. Reward-, PvP- und Kaufitems werden dadurch nicht in ihrer Business-Logik veraendert.

Der Materialaudit des CI-Builds vom 05.09.2026 zeigte Kollisionen der frueheren Slots `FIREWORK`, `EYE_OF_ENDER`, `BOOK_AND_QUILL`, `HOPPER` und `NAME_TAG` mit anderen SkyKings-Funktionen. Completed, Premium, Quest, Shop und Trade wurden deshalb auf aktuell unbenutzte 1.8-Slots verschoben.

Der virtuelle Coin-Input im echten PlayerShop-Villager-Handel verwendet weiterhin `GOLD_NUGGET`/Coin-Textur. `NETHER_STAR` bleibt damit eindeutig der SkyKings-Stern.

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
- [ ] GUI Scale Small/Normal/Large/Auto getestet
- [ ] Battle Pass, Quests, Kits, Crates, Commands Hub, Jackpot, Trade und PlayerShop mit Pack lesbar
- [ ] dieselben Systeme ohne Pack voll bedienbar
- [ ] PlayerShop-Villager zeigt im Input das Coin-Icon, niemals den Stern
- [ ] Merchant-Coin-Token kann weiterhin nicht entnommen/verschoben werden
- [ ] normales PvP-Pack bleibt fuer Waffen/Ruestung/Bow/Rod/Pearls/Gapples nutzbar
- [x] finale ZIP unter festem direkten HTTPS-Pfad vorbereitet
- [ ] `resource-pack.enabled: true` nach bestandenem Clienttest setzen
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

Der Build validiert `pack.mcmeta`, validiert das Java-Materialmapping, erzeugt alle 19 Pflichttexturen + `pack.png` und bricht ab, sobald eines der erwarteten ZIP-Assets fehlt oder leer ist.
