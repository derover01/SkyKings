# SkyKings Battle Pass UI

## Zielbild

Der Battle Pass soll sich wie ein eigenes Produkt im Server anfuehlen und nicht wie eine weitere 54-Slot-Truhe.

Die serverseitige 1.8.9-Basis besteht deshalb aus drei getrennten Ansichten:

1. **Pass Hub** – dein Pass, Season-Level, Progress, Quests, Rewards, Premium.
2. **Quest Center** – Free- und Premium-Aufgaben als getrennte Panels mit sichtbarer Progress-Bar.
3. **Reward Track** – horizontaler Free-/Premium-Pfad mit 20 Meilensteinen bis Level 100.

Die Logik bleibt auch ohne Resource Pack voll bedienbar. Ein spaeteres SkyKings-Resource-Pack darf diese Shell optisch in ein echtes Bitmap-/Pixel-UI verwandeln, ohne die Gameplay-Logik neu zu bauen.

## UX-Regeln

- wenige grosse Entscheidungen statt einer Wand gleichwertiger Items
- Free und Premium jederzeit visuell unterscheidbar
- Premium erweitert den Free Pass; Free-Inhalte werden niemals ersetzt oder versteckt
- jeder Reward hat genau einen Zustand: `READY`, `LOCKED`, `COMPLETED`
- Progress wird als kurze horizontale Bar dargestellt
- Quests zeigen Aufgabe, aktuellen Fortschritt und Reward in scanbaren Zeilen
- Back/Home/Next bleiben mit dem globalen SkyKings-Navigationssystem konsistent
- Sounds bestaetigen Menu-Open, Claim, Fehler und Level-Up
- technische IDs/Dateipfade erscheinen nie im Spieler-UI

## Aktuelle Season-Struktur

### Reward Track

20 Milestones:

`5, 10, 15, ... 100`

Jeder Milestone besitzt:
- Free Reward
- Premium Reward
- sichtbaren Track-Zustand
- persistentes Claim-Flag

Die letzte Stufe besitzt einen zusaetzlichen Season-Finale-Bonus.

### Free Quests

- Daily: 5 legitime PvP-Kills
- Daily: 20 Enderperlen ausserhalb von Events
- Daily: 5er Killstreak
- Weekly: 30 legitime PvP-Kills
- Weekly: King Altar 3x erobern

### Premium Quests

- Daily: 10 legitime PvP-Kills
- Daily: 40 Enderperlen ausserhalb von Events
- Weekly: 75 legitime PvP-Kills
- Weekly: King Altar 7x erobern

Quest-Rewards geben Coins, physische SkyKings-Sterne und Season-XP.

## Anti-Farm / Integritaet

PvP-Quest-Fortschritt wird nicht direkt aus einem beliebigen `PlayerDeathEvent` gezaehlt. Er kommt aus dem bereits validierten `SkyKingsPlayerKillEvent`. Dadurch greifen dieselben legitimen Kill-/Anti-Farm-Regeln fuer Stats, Rewards und Battle Pass.

Event-Teilnahmen duerfen Pearl-/Open-World-Aufgaben nicht ungewollt farmen.

Claim-Status wird in `battlepass.yml` persistiert. Daily-/Weekly-Quest-Zyklen werden in `quests.yml` persistiert.

## Resource-Pack-Ausbaustufe

Fuer eine Darstellung wie ein echtes eigenes Pixel-Panel braucht Minecraft 1.8.9 clientseitige Texturen/Font-Assets in einem Resource Pack. Die serverseitige Navigation ist bereits so getrennt, dass spaeter beispielsweise folgende visuelle Layer darueber gelegt werden koennen:

- eigener Battle-Pass-Hintergrund
- Tab-/Button-Texturen fuer Pass, Quests, Rewards, Premium
- Free-/Premium-Rails
- Progress-Bar Segmente
- Quest-Kartenrahmen
- Locked/Ready/Claimed Icons
- Season-Emblem

Dabei keine fremden gekauften UI-Assets kopieren. Referenzen dienen nur als UX-/Layout-Inspiration; SkyKings bekommt eine eigene Pixel-Art-Sprache passend zum aqua/gold/dark Design System.
