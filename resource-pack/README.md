# SkyKings 2.0 Resource Pack

Der SkyKings-Resource-Pack ist ein **UI-/Branding-Layer fuer Minecraft 1.8.9**. Er soll den Server moderner und eigenstaendiger wirken lassen, ohne die normalen PvP-, Block- und Item-Texturen des Spielers unnoetig zu ersetzen.

## Technische Basis

- Zielversion: Minecraft Java 1.8.9
- `pack_format`: `1`
- kein modernes `CustomModelData`
- keine modernen JSON-Font-/Glyph-Provider
- keine Abhaengigkeit der Serverlogik vom Pack
- alle Commands, GUIs und Statusinformationen bleiben ohne Pack voll bedienbar
- die echten UI-Texturen werden beim Build aus `resource-pack-source/skykings-ui-atlas.rgba.gz` erzeugt
- der zentrale `GuiManager` dekoriert passende SkyKings-Menues unmittelbar vor dem Oeffnen

## Aktueller Core-Icon-Satz

Der echte Pack-Satz ist fest reserviert und wird vom Build automatisch auf Legacy-1.8-Dateinamen geschrieben. Fuenf Slots wurden nach einem Vollscan der aktuellen Plugin-JARs bewusst auf Materialien verschoben, die ausserhalb der Pack-Zuordnung nicht verwendet werden.

| SkyKings Icon | Bukkit Material | Legacy Texture |
|---|---|---|
| Home | `MINECART` | `minecart_normal.png` |
| Back | `POWERED_MINECART` | `minecart_furnace.png` |
| Next | `HOPPER_MINECART` | `minecart_hopper.png` |
| Locked | `BARRIER` | `barrier.png` |
| Ready | `SLIME_BALL` | `slimeball.png` |
| Completed | `GHAST_TEAR` | `ghast_tear.png` |
| Premium | `PRISMARINE_CRYSTALS` | `prismarine_crystals.png` |
| Coins | `GOLD_NUGGET` | `gold_nugget.png` |
| SkyKings Star | `NETHER_STAR` | `nether_star.png` |
| Battle Pass | `EMPTY_MAP` | `map_empty.png` |
| Quests | `PRISMARINE_SHARD` | `prismarine_shard.png` |
| Kits | `STORAGE_MINECART` | `minecart_chest.png` |
| Crates | `COMMAND_MINECART` | `minecart_command_block.png` |
| Jackpot | `DIODE` | `repeater.png` |
| Shop | `CARROT_STICK` | `carrot_on_a_stick.png` |
| Trade | `FIREWORK_CHARGE` | `firework_charge.png` |
| Clan | `WRITTEN_BOOK` | `book_written.png` |
| Duel | `SHEARS` | `shears.png` |
| Event | `MAGMA_CREAM` | `magma_cream.png` |

Das SkyKings-Logo ist der zwanzigste Atlas-Tile und wird als `pack.png` erzeugt.

## Design-Richtung

SkyKings soll wie ein eigenes Game-Produkt wirken:

- dunkles Anthrazit / Schwarz als Basis
- Aqua als primaerer SkyKings-Akzent
- Gold fuer Premium / besondere Rewards
- Hellviolett fuer Mythic / Season / Crates
- Rot nur fuer Danger / Delete / Failure
- klare Pixel-Icons statt visueller Unruhe
- keine fremden Server-/Pack-Designs 1:1 kopieren

## Was bewusst NICHT ueberschrieben wird

Damit Spieler weiterhin ihr eigenes PvP-Pack nutzen koennen, bleiben standardmaessig unangetastet:

- Schwerter
- Boegen
- Ruestung
- Angel
- Enderperlen
- Golden Apples
- normale PvP-Bloecke
- wichtige Partikel fuer PvP-Lesbarkeit
- Crosshair/HUD

Die einzigen bewusst semantisch globalen Server-Items sind `GOLD_NUGGET` fuer Coins und `NETHER_STAR` fuer den SkyKings-Stern. Die uebrigen reservierten Pack-Slots sind im aktuellen Plugin-Code ausserhalb der Pack-Zuordnung unbenutzt.

## 1.8.9-Strategie fuer Custom Icons

Da `CustomModelData` in 1.8.9 nicht zur Verfuegung steht, werden Icons ueber **bewusst reservierte Vanilla-Items** umgesetzt. Die zentrale Server-Zuordnung liegt in `plugins/SkyKings-Core/src/main/java/net/skykings/core/ui/ResourcePackIcon.java`; die Build-Zuordnung liegt in `scripts/tools/ResourcePackAtlasBuilder.java`. Beide muessen synchron bleiben.

Die Atlas-Quelle ist eine gzip-komprimierte binaere Datei mit rohen RGBA-Pixeln. Java 8 entpackt exakt das 160x128-Atlasbild und erzeugt die finalen PNGs selbst reproduzierbar. Der Build prueft zusaetzlich die 19 Java-Materialbindungen und bricht bei einer Abweichung ab.

## Build

Lokal:

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```

Beim Build wird der Atlas mit Java 8 in 19 Itemtexturen plus `pack.png` zerlegt. Der Build bricht ab, wenn ein Pflichtasset im finalen ZIP fehlt, leer ist oder das Java-Materialmapping nicht dem Pack-Manifest entspricht.

## Release-Regel

Der Pack ist fuer den Launch ein **fester visueller Pre-Launch-Baustein**, aber kein Gameplay-Dependency.

Vor Release muessen beide Modi getestet werden:

1. ohne SkyKings-Pack
2. mit SkyKings-Pack ueber einem normalen PvP-Pack

In beiden Modi muessen alle Kernsysteme nutzbar bleiben. Die letzten externen Gates sind ein frischer Minecraft-1.8.9-Clienttest und eine stabile direkte HTTPS-URL fuer das finale ZIP.
