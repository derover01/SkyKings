# SkyKings – PC Test Checklist

Diese Checkliste ist der feste Runtime-Test fuer den aktuellen Pre-Release-Stand.
Die dauerhafte Setup-Liste steht zusaetzlich in `docs/SERVER_SETUP_TODO.md`.

## 1. Repository aktualisieren und bauen

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
.\scripts\deploy-server.ps1
```

Erwartung: `BUILD SUCCESS`.

## 2. Server starten

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Beim Start auf `SEVERE`, `Exception`, `NoSuchMethodError` und deaktivierte SkyKings-Module achten.

## 3. Runtime-Schnellcheck

```text
/skcheck
```

Erwartet insbesondere:
- SkyKings-Core: OK
- SkyKings-Combat: OK
- SkyKings-Crates: OK
- SkyKings-Admin: OK
- LuckPerms / Vault: OK
- Core API / Island API / Plot API: OK

## 4. Aktuelle Regressionstests

### GUI-Crashfix
Diese Befehle duerfen **keinen ArrayIndexOutOfBoundsException Slot 45** mehr werfen:
```text
/prefix
/top
/gutscheine
```
In `/gutscheine` jede Kategorie einmal oeffnen.

### Gutschein-/Crate-Tooltip
Neu erzeugte Items testen:
- technische Serial-/Batch-ID darf Tooltip nicht mehr ueber den Bildschirm ziehen
- Ranggutschein zeigt konkreten Rang im Namen
- Rechtegutschein zeigt konkretes Recht
- Coin/GiveAll zeigt konkreten Betrag
- Rang/Rechte = Buch
- Kit = Papier
- Coin/GiveAll = Sonnenblume
- Coin-Gewinn aus Crate kommt als Gutschein statt Sofortbuchung

### Map-Schutz
- auf `SkyPvP` Weizenfeld mehrfach anspringen -> Farmland bleibt Farmland
- ohne `/buildmode` kein Bauen/Abbauen
- mit `/buildmode` als Staff editierbar

### Profile / Navigation
```text
/stats
/achievements
/collection
/legacyhall
/top
```
- Achievements aus `/stats` muss dasselbe `/achievements`-Menue oeffnen wie der Direktcommand
- Collection Seite 1 -> Zurueck zu Profile
- Legacy Hall Seite 1 -> Zurueck zu Profile
- Top -> Zurueck zu Profile

### Kopfgeld
Mit einem zweiten bekannten Spieler:
```text
/kopfgeld <Spieler> 25k
```
- 25.000 Coins werden beim Setzer abgezogen
- Ziel erscheint im `/kopfgeld` Board
- Board zeigt Spieler-Kopfgeld + ggf. Streak-Bounty getrennt
- legitimer Kill zahlt Spieler-Kopfgeld aus
- Farm-/Repeat-Kill darf gesetztes Kopfgeld nicht verbrauchen

### Islands
```text
/is
/is level
/is top
```
- Grenze in Info: 129x129 / Center +/-64
- `/is top` zeigt Top 10 nach Island-Level
- wertvolle Bloecke steigern Level; Abbauen reduziert es wieder
- `[Welcome]` bleibt Voraussetzung fuer `/is visit <Owner>`
- Besucher landet direkt am Schild

**Achtung:** Eine alte Testinsel behaelt ihre alte Form. Fuer den neuen klassischen Baum/Chest-Generator siehe Reset-Hinweis in `SERVER_SETUP_TODO.md`.

### Plots
Nach einmaligem Pre-Release-Reset der alten `SkyPlots`-Testwelt:
```text
/p auto
/p h
/p add <Spieler>
/p trust <Spieler>
/p deny <Spieler>
/p flag pvp an
```
- 65x65 Plot + 7 Block Strassen
- `add`: Baurecht nur solange Owner online
- `trust`: dauerhaft
- `deny`: Zutritt blockiert
- Flags im GUI testbar

### Kits
```text
/kit
```
- READY / COOLDOWN sichtbar
- Linksklick = claim
- Rechtsklick = echte Item-/Potion-Vorschau
- Zurueck funktioniert

### Quests / Battle Pass
```text
/quests
/battlepass
```
- jedes Quest-Item beschreibt die konkrete Aufgabe
- Free-Spieler sieht keine Premium-Aufgaben
- Premium-Spieler hat zusaetzliche Daily-/Weekly-Quests
- Quest- und Battle-Pass-Sterne sind echte gebrandete SkyKings Sterne

### PlayerShops
```text
/playershop kaufen
```
- Haendler-Ei nur auf eigener Island/eigenem Plot platzierbar
- `/playershop set <Menge> <Coins>`
- Item halten + `/playershop stock <Menge>`
- Kauf mit zweitem Spieler
- Owner-Rechtsklick -> Control Center
- Claim / Withdraw testen

### Friede
`/peace` und `/friede` sind dasselbe System.
- Anfrage / Accept / Deny
- Partner koennen sich nicht verletzen
- kein CombatTag durch geblockten Peace-Hit
- nach Remove normales PvP

### Event-Welt
Einmalig spaeter:
```text
/skymap event SkyEvents
```
- Hub, Duel, LMS, Tournament, Juggernaut optisch pruefen
- Map darf von normalen Spielern nicht zerstoert werden
- Event-Welt hat PvP technisch aktiv; Session-Schutz entscheidet, wer kaempfen darf
- Arena-Punkte danach gemaess `SERVER_SETUP_TODO.md` setzen

## 5. Dauerhaft noch ingame zu konfigurieren

Siehe:
```text
docs/SERVER_SETUP_TODO.md
```

Dort stehen insbesondere `/mapsetup`, King/HotZone/End/Secrets/Routen/Landmarks, NPC-Bindings, PvP-Regionen, Event-Arenen, Legacy Hall und Discord.
