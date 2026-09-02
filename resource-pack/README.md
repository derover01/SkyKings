# SkyKings 2.0 Resource Pack

Der SkyKings-Resource-Pack ist ein **UI-/Branding-Layer fuer Minecraft 1.8.9**. Er soll den Server moderner und eigenstaendiger wirken lassen, ohne die normalen PvP-, Block- und Item-Texturen des Spielers unnoetig zu ersetzen.

## Technische Basis

- Zielversion: Minecraft Java 1.8.9
- `pack_format`: `1`
- kein modernes `CustomModelData`
- keine modernen JSON-Font-/Glyph-Provider
- keine Abhaengigkeit der Serverlogik vom Pack
- alle Commands, GUIs und Statusinformationen bleiben ohne Pack voll bedienbar

## Design-Richtung

SkyKings soll wie ein eigenes Game-Produkt wirken:

- dunkles Anthrazit / Schwarz als Basis
- Aqua als primaerer SkyKings-Akzent
- Gold fuer Premium / besondere Rewards
- Hellviolett fuer Mythic / Season / Crates
- Rot nur fuer Danger / Delete / Failure
- klare Pixel-Icons statt visueller Unruhe
- keine fremden Server-/Pack-Designs 1:1 kopieren

## Was der Pack veraendern darf

Prioritaet A — UI/Branding:

- ausgewaehlte GUI-Texturen, sofern die Aenderung nicht Vanilla-/PvP-Nutzung verschlechtert
- gezielt reservierte Vanilla-Item-Texturen fuer reine SkyKings-UI-Icons
- SkyKings-spezifische Branding-Assets
- dezente Partikel-/Effekt-Texturen nur nach Runtime-Test
- optional `pack.png`

Prioritaet B — spaeterer Polish:

- Crate-/Voucher-Iconset
- Battle-Pass-/Quest-Iconset
- Kit-/Rank-/Season-Iconset
- Navigation: Home, Back, Next, Locked, Ready, Completed, Premium
- Economy: Coins, Netherstar, Shop, Jackpot, Trade
- Events: Duel, LMS, King/KOTH, Clan War, Giveaway

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
- Crosshair/HUD, solange kein eigener Test die Aenderung rechtfertigt

Wenn ein Vanilla-Item als SkyKings-Icon reserviert wird, darf es im normalen Gameplay nicht gleichzeitig eine relevante Funktion haben.

## 1.8.9-Strategie fuer Custom Icons

Da `CustomModelData` in 1.8.9 nicht zur Verfuegung steht, werden Icons ueber **bewusst reservierte Vanilla-Items bzw. vorhandene Data-Varianten** geplant. Ein Icon-Item darf erst texturiert werden, wenn im Servercode dokumentiert ist, dass diese Material-/Data-Kombination nicht fuer normales Gameplay gebraucht wird.

Das verhindert, dass z. B. ein echtes PvP-Item durch ein GUI-Icon ersetzt wird.

## Geplanter Asset-Baum

```text
resource-pack/
  pack.mcmeta
  pack.png                         # optional
  assets/
    minecraft/
      textures/
        gui/                       # nur gezielte, getestete Overrides
        items/                     # 1.8-Pfad, nur reservierte Icon-Items
        blocks/                    # standardmaessig leer
        particle/                  # nur bei bewusstem Polish
```

Die Verzeichnisse werden erst mit echten Assets committed. Keine Dummy-PNGs in Release-ZIPs.

## Asset-Manifest

Vor jedem neuen Icon-Override muss die Zuordnung in `docs/RESOURCE_PACK.md` eingetragen werden:

- Feature
- Bedeutung
- Material
- Data/Durability falls relevant
- Vanilla-Nutzung auf SkyKings
- Pack-Datei
- Fallback ohne Pack

## Build

Lokal:

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```

Die ZIP muss `pack.mcmeta` direkt im Root enthalten, nicht in einem zusaetzlichen Unterordner.

## Release-Regel

Der Pack ist fuer den Launch ein **fester visueller Pre-Launch-Baustein**, aber kein Gameplay-Dependency.

Vor Release muessen beide Modi getestet werden:

1. ohne SkyKings-Pack
2. mit SkyKings-Pack ueber einem normalen PvP-Pack

In beiden Modi muessen alle Kernsysteme nutzbar bleiben.
