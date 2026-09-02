# SkyKings 2.0 Resource Pack

Der SkyKings-Resource-Pack ist ein **UI-/Branding-Layer fuer Minecraft 1.8.9**. Er soll den Server moderner und eigenstaendiger wirken lassen, ohne die normalen PvP-, Block- und Item-Texturen des Spielers unnoetig zu ersetzen.

## Technische Basis

- Zielversion: Minecraft Java 1.8.9
- `pack_format`: `1`
- kein modernes `CustomModelData`
- keine modernen JSON-Font-/Glyph-Provider
- keine Abhaengigkeit der Serverlogik vom Pack
- alle Commands, GUIs und Statusinformationen bleiben ohne Pack voll bedienbar
- die echten UI-Texturen werden beim Build aus `resource-pack-source/skykings-ui-atlas.png` erzeugt

## Aktueller Core-Icon-Satz

Der erste echte Pack-Satz ist fest reserviert und wird vom Build automatisch auf Legacy-1.8-Dateinamen geschrieben:

| SkyKings Icon | Bukkit Material | Legacy Texture |
|---|---|---|
| Home | `MINECART` | `minecart_normal.png` |
| Back | `POWERED_MINECART` | `minecart_furnace.png` |
| Next | `HOPPER_MINECART` | `minecart_hopper.png` |
| Locked | `BARRIER` | `barrier.png` |
| Ready | `SLIME_BALL` | `slimeball.png` |
| Completed | `FIREWORK` | `fireworks.png` |
| Premium | `EYE_OF_ENDER` | `ender_eye.png` |
| Coins | `GOLD_NUGGET` | `gold_nugget.png` |
| SkyKings Star | `NETHER_STAR` | `nether_star.png` |
| Battle Pass | `EMPTY_MAP` | `map_empty.png` |
| Quests | `BOOK_AND_QUILL` | `book_writable.png` |
| Kits | `STORAGE_MINECART` | `minecart_chest.png` |
| Crates | `COMMAND_MINECART` | `minecart_command_block.png` |
| Jackpot | `DIODE` | `repeater.png` |
| Shop | `HOPPER` | `hopper.png` |
| Trade | `NAME_TAG` | `name_tag.png` |
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

## 1.8.9-Strategie fuer Custom Icons

Da `CustomModelData` in 1.8.9 nicht zur Verfuegung steht, werden Icons ueber **bewusst reservierte Vanilla-Items** umgesetzt. Die zentrale Server-Zuordnung liegt in `ResourcePackIcon.java`; die Build-Zuordnung liegt in `ResourcePackAtlasBuilder.java`. Beide muessen synchron bleiben.

## Build

Lokal:

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```

Beim Build wird der Atlas mit Java 8 in 19 Itemtexturen plus `pack.png` zerlegt. Der Build bricht ab, wenn ein Pflichtasset im finalen ZIP fehlt.

## Release-Regel

Der Pack ist fuer den Launch ein **fester visueller Pre-Launch-Baustein**, aber kein Gameplay-Dependency.

Vor Release muessen beide Modi getestet werden:

1. ohne SkyKings-Pack
2. mit SkyKings-Pack ueber einem normalen PvP-Pack

In beiden Modi muessen alle Kernsysteme nutzbar bleiben.
