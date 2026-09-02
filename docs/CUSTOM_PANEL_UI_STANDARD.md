# SkyKings Custom Panel UI Standard

Ergaenzt `UI_UX_DESIGN_SYSTEM.md` fuer die hochwertigen Hauptsysteme. Zielbild ist kein klassisches Slot-Menue, sondern ein modernes Game-Interface, das innerhalb der Grenzen von Minecraft 1.8.9 moeglichst wie ein eigenes Produkt wirkt.

## 1. Wann Custom Panel UI Pflicht ist

Diese Systeme sollen nicht als einfache Item-Wand gebaut werden:

- Battle Pass
- Quests
- Kit Arsenal
- Profile / Progression
- Achievements
- Events: Duel, LMS, King/KOTH, Most Wanted, Clan Wars
- grosse Shops / Black Market
- Clan Hub
- Season / Hall of Fame

Kleine Utility-GUIs duerfen bewusst einfacher bleiben.

## 2. Aufbau

Groessere Interfaces verwenden wenn sinnvoll vier Ebenen:

1. Hero/Header: Feature, Season/Status, wichtigste Zahl.
2. Tabs/Navigation: wenige grosse Kategorien.
3. Content Panel: Karten, Rails oder Fortschrittsbereiche.
4. Footer Navigation: Back / Home / Next an den bekannten Positionen.

Nicht alles gleichzeitig zeigen. Ein Tab soll eine klare Aufgabe haben.

## 3. Panel statt Slot-Wand

Slots werden als Layout-Raster behandelt, nicht als 54 gleichwertige Buttons.

Erlaubte Patterns:

- Hero Card: 1 grosses Statusobjekt + benachbarte Panel-Slots.
- Tab Bar: 3-5 Kategorien, ACTIVE deutlich markiert.
- Reward Rail: Free oben, Level/Progress in der Mitte, Premium unten.
- Quest Cards: wenige grosse Karten mit Task, Progress, Reward, Status.
- Arsenal: Rang-/Kit-Pfad mit READY, COOLDOWN, LOCKED und eigener Preview.
- Event Board: Queue, Runde, Spieler, Reward und Join/Leave als klare Bereiche.

Glass Panes duerfen Panelgrenzen oder Rails visualisieren, aber niemals jedes freie Feld fuellen.

## 4. Visual States

Jedes interaktive Element besitzt genau einen klaren Zustand:

- ACTIVE = Aqua
- READY = Gruen
- COMPLETED = Gruen / reduziert
- COOLDOWN = Gelb
- LOCKED = Dunkelgrau
- PREMIUM = Gold
- MYTHIC / Season Special = Hellviolett
- DANGER = Rot

Status zuerst erfassbar, Details erst in der Lore.

## 5. Progress

Progress soll moeglichst visuell statt nur numerisch sein.

Beispiele:

- Quest: 12 Segment-Bar + 17/25
- Battle Pass: horizontale Milestone-Rail
- Season: Level + XP bis zum naechsten Level
- Event: Round / Remaining Players

Keine progress-bar aus 30 Zeichen, wenn 10-12 Segmente denselben Zweck sauberer erfuellen.

## 6. Hero Information

Pro Hauptansicht genau eine dominante Information:

Battle Pass: Season-Level.
Quest Center: erledigte Daily/Weekly/Premium-Aufgaben.
Kit Arsenal: aktueller Rang + freigeschaltete Kits.
Event Hub: Queue/Status des ausgewaehlten Events.

Sekundaere Informationen werden auf Tabs oder Detailansichten verteilt.

## 7. Microinteractions

Nach einem Klick wird die Ansicht sofort aktualisiert:

- Claim -> COMPLETED
- Kauf -> OWNED / SELECTED
- Join -> JOINED
- Cooldown -> sichtbarer Restwert
- Tab -> ACTIVE

Passender zentraler SkyKings-Sound begleitet die Aktion.

## 8. Resource-Pack Layer — Minecraft 1.8.9

Der SkyKings Resource Pack ist ein fester **Pre-Launch-Presentation-Layer**, aber keine Gameplay-Abhaengigkeit. Ohne Pack bleiben Navigation, Namen, Lore, Preise, Status und Claims voll nutzbar.

Technische Regeln:

- `pack_format: 1`
- kein modernes `CustomModelData`
- keine modernen JSON-Font-/Glyph-Provider
- eigene Pixel-Icons nur ueber bewusst reservierte Vanilla-Material-/Data-Kombinationen
- keine wichtigen PvP-Items fuer UI-Icons global ueberschreiben
- GUI-/Branding-Overrides nur nach echtem 1.8.9-Clienttest

Moegliche Bausteine:

- eigene Pixel-Icons fuer Navigation und Status
- Economy-/Shop-/Trade-/Jackpot-Icons
- Battle-Pass-/Quest-/Kit-/Season-Icons
- Crate-/Voucher-Branding
- dezente GUI-Rahmen/Akzente, sofern sie mit normalen PvP-Packs sauber funktionieren
- eigenes `pack.png`

Die konkrete Asset-Zuordnung und die reservierten Icon-Items werden in `docs/RESOURCE_PACK.md` gepflegt.

## 9. Referenz-Zielbild

Die visuelle Richtung ist ein dunkles, reduziertes Game-HUD mit:

- klaren schwarzen/dunkelgrauen Panels
- Aqua als SkyKings-Akzent
- Gold fuer Premium
- Hellviolett fuer Season/Mythic
- horizontalen Reward Rails
- grossen, klaren Tabs
- wenigen starken Cards statt vieler gleichwertiger Items

Nicht 1:1 fremde Packs kopieren. Die UX-Idee wird in eine eigene SkyKings-Sprache uebersetzt.

## 10. Pflichtcheck vor Merge

Vor jeder grossen GUI pruefen:

- Wirkt es wie eine App/Game-Oberflaeche und nicht wie eine Item-Kiste?
- Gibt es eine dominante Information?
- Sind Kategorien getrennt?
- Ist der Status ohne Lore lesbar?
- Sind lange Texte auf Unteransichten verteilt?
- Funktioniert alles ohne Resource Pack?
- Back/Home/Next konsistent?
- 1.8.9 kompatibel und performant?
- verwendet die GUI nur dokumentierte Pack-Icon-Kombinationen?

Dieser Standard ist fuer alle neuen Premium-SkyKings-UIs verbindlich.
