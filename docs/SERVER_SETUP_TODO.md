# SkyKings – Server Setup TODO

Diese Datei ist die dauerhafte Checkliste fuer alle Dinge, die **nicht sinnvoll hart im Code stehen koennen**, weil sie an der finalen Map ingame gesetzt werden muessen.

> Regel: Keine finalen Koordinaten raten. Auf der echten Map an die gewuenschte Position stellen und dort den jeweiligen Setup-Befehl ausfuehren.

## 1. Produktionsmap SkyPvP

- `SkyPvP`-Weltordner muss unter `server/SkyPvP/level.dat` liegen.
- `/skymap load SkyPvP`
- richtigen Spawn pruefen
- `/setspawn`
- pruefen: Weizen/Farmland kann nicht zertrampelt werden
- pruefen: ohne `/buildmode` kein Map-Abbau/-Platzieren

## 2. Zentrale Map-Konfiguration

Als Staff zuerst:

```text
/mapsetup
```

Danach auf der echten SkyPvP-Map setzen:

### King / KOTH
```text
/kingaltar set <Radius>
/kingaltar info
```

### Hot Zones
```text
/hotzone add <Name> <Radius>
/hotzone list
```

### End Zone
```text
/endzone set <Radius>
/endzone info
```

### Secrets
```text
/secret add <Name> [Radius]
/secret list
```

### Secret Loot Rooms
Auf die gewuenschte versteckte Truhe schauen:
```text
/secretroom add <Name> <common|rare|epic> <CooldownMinuten>
/secretroom list
```

### Jump-/Pearl-Routen
```text
/route create <Name>
/route addpoint <Name>
/route list
```
`/route addpoint` an jedem Checkpoint in Reihenfolge ausfuehren.

### Gameplay-Landmarks
```text
/landmark set gold <Radius>
/landmark set level <Radius>
/landmark set blacksmith <Radius>
/landmark set merchant <Radius>
/landmark list
```

### Trash Bins
Auf den gewuenschten Hopper schauen:
```text
/trashbin add
/trashbin list
```

### Map Displays
An die gewuenschte Hologrammposition stellen:
```text
/mapdisplay set topkills
/mapdisplay set king
/mapdisplay set hotzones
/mapdisplay list
```

### Most Wanted PvP-Regionen
Je Region zwei Ecken setzen:
```text
/pvpregion pos1
/pvpregion pos2
/pvpregion create <Name>
/pvpregion list
```
Most Wanted darf nur innerhalb dieser Regionen laufen.

## 3. Shop NPCs

An den finalen NPC-Positionen auf der Main Map binden:

```text
/shopnpc bind system
/shopnpc bind pvp_restock
/shopnpc bind blacksmith
/shopnpc bind enchant
/shopnpc bind recycler
/shopnpc bind merchant
/shopnpc bind jackpot
```

Blacksmith/Merchant vorher in ihren Landmark-Bereichen platzieren.

**Offen:** Shoppreise gemeinsam final balancen. Aktuelle Preise sind noch Test-/Draftwerte.

## 4. Separate Combat-Event-Welt

Einmalig erzeugen:

```text
/skymap event SkyEvents
```

Diese Welt ist **nur fuer spielbare Combat-Events** gedacht und nicht fuer die Community-Giveaways.

Die Welt besteht aus:
- zentralem SkyKings Event-Hub
- Nord: Duel Courtyard
- Ost: LMS Ruins
- Sued: Tournament Colosseum
- West: Juggernaut Fortress

Die farbigen/auffaelligen Bodenmarker sind bewusst fuer Arena-Punkte vorgesehen.

### Duel
Auf Gold-Marker:
```text
/eventarena set duel a
```
Auf Diamant-Marker:
```text
/eventarena set duel b
```
Auf Zuschauerplattform:
```text
/eventarena set duel spectator
```

### LMS
Auf gruenem Lobby-Marker:
```text
/eventarena set lms lobby
```
Danach die acht Glowstone-Marker im Kreis ablaufen:
```text
/eventarena set lms spawn1
/eventarena set lms spawn2
/eventarena set lms spawn3
/eventarena set lms spawn4
/eventarena set lms spawn5
/eventarena set lms spawn6
/eventarena set lms spawn7
/eventarena set lms spawn8
```
Zuschauerplattform:
```text
/eventarena set lms spectator
```

### Tournament
Der Gameplay-Controller ist aktiv und spielt ein Single-Arena-Bracket automatisch Runde fuer Runde aus.

Einmalig an den echten Punkten setzen:
```text
/eventarena set tournament lobby
/eventarena set tournament a
/eventarena set tournament b
/eventarena set tournament spectator
```

Spieler:
```text
/tournament join
/tournament leave
/tournament status
```

Staff:
```text
/tournament start
/tournament stop
```

Regeln:
- mindestens 4 Spieler zum Start
- Reihenfolge wird beim Start gemischt
- Matches laufen nacheinander ueber `a` und `b`
- ungerade Spielerzahl bekommt automatisch ein Freilos
- nur das aktive 1v1-Paar kann sich Schaden zufuegen
- Wartende/Lobby-Spieler sind voneinander isoliert
- Verlierer wird aus dem Event entfernt und an seine Rueckkehrposition geschickt
- letzter Spieler gewinnt 1.000.000 Coins
- Tournament-Kills zaehlen nicht als normale Open-World-Kills/Streaks/Bounties

Vor Release mit 4, 5 und 8 Spielern testen, inklusive Quit waehrend eines Matches.

### Juggernaut
Der Gameplay-Controller ist aktiv: ein zufaellig ausgewaehlter Boss kaempft gegen alle anderen Teilnehmer.

Einmalig setzen:
```text
/eventarena set juggernaut lobby
/eventarena set juggernaut boss
/eventarena set juggernaut spawn1
/eventarena set juggernaut spawn2
/eventarena set juggernaut spawn3
/eventarena set juggernaut spawn4
/eventarena set juggernaut spawn5
/eventarena set juggernaut spawn6
/eventarena set juggernaut spawn7
/eventarena set juggernaut spawn8
/eventarena set juggernaut spectator
```

Mindestens benoetigt werden `lobby`, `boss` und zwei `spawn`-Punkte.

Spieler:
```text
/juggernaut join
/juggernaut leave
/juggernaut status
```

Staff:
```text
/juggernaut start
/juggernaut stop
```

Regeln:
- mindestens 3 Spieler
- Boss wird zufaellig bestimmt
- Boss: 40 HP, Staerke I, Resistenz I
- Angreifer werden auf `spawn1..spawnN` verteilt
- kein Friendly Fire zwischen Angreifern
- nur Boss und aktive Angreifer koennen sich gegenseitig Schaden zufuegen
- Items droppen/pickup und fremde Commands sind waehrend des Events blockiert
- Boss besiegt: jeder ueberlebende Angreifer +250.000 Coins
- alle Angreifer besiegt: Boss +1.000.000 Coins
- Event-Kills bleiben aus Open-World-Stats/Streaks/Bounties heraus

Vor Release mit 3, 5 und 9 Spielern testen, ausserdem Boss-Quit, Angreifer-Quit, Pfeile und Restart/Stop waehrend des Events.

### Clan Wars
Der Gameplay-Controller ist aktiv und verwendet die echten persistenten Core-Clans.

Mindestens fuer 2v2 setzen:
```text
/eventarena set clanwar a1
/eventarena set clanwar a2
/eventarena set clanwar b1
/eventarena set clanwar b2
```

Fuer groessere Wars optional:
```text
/eventarena set clanwar a3
/eventarena set clanwar a4
/eventarena set clanwar a5
/eventarena set clanwar b3
/eventarena set clanwar b4
/eventarena set clanwar b5
```

Challenge durch Clan-Owner:
```text
/clanwar <gegnerischerOwner>
```

Ziel-Owner:
```text
/clanwar accept
/clanwar deny
```

Status / Staff:
```text
/clanwar status
/clanwar stop
```

Regeln:
- 2v2 bis 5v5
- beide echten Clan-Owner muessen online sein
- beide Clans brauchen mindestens 2 freie Online-Mitglieder
- Teamgroesse wird automatisch auf die kleinere verfuegbare Seite balanciert
- Friendly Fire innerhalb des eigenen Clans ist aus
- nur Gegner koennen Schaden machen
- tote Spieler sind eliminiert und kehren an ihre Rueckkehrposition zurueck
- Quit zaehlt als Ausscheiden
- letzter Clan gewinnt
- jeder ueberlebende Sieger erhaelt +500.000 Coins
- Clan-War-Kills bleiben aus Open-World-Stats/Streaks/Bounties heraus
- Commands, Drop/Pickup und fremde Inventare sind waehrend des Wars blockiert

Vor Release 2v2, 3v3 und 5v5 testen, inklusive Owner-Quit, Member-Quit, Pfeile und `/clanwar stop`.

## 5. Community-/Giveaway-Eventmap

Diese Map ist fuer die typischen Freitag-/Samstag-Events gedacht, bei denen Staff Items, Ränge, Gutscheine, Crates oder andere Gewinne an Spieler verteilt.

Einmalig erzeugen:

```text
/skymap community SkyCommunityEvent
```

Ziel der Map:
- bewusst kompakt, keine riesige Arena
- PvP deaktiviert
- zentrale kleine SkyKings-Buehne
- freie Zuschauerflaeche
- Giveaway-/Preis-Podeste
- Host-/Staff-Bereich
- Void-/Sky-Feeling
- danach optional manuell mit WorldEdit/Handbau verfeinern

Nach dem Erzeugen pruefen:
- Spawnpunkt passt
- keine Mobs
- kein PvP
- Buehne und Zuschauerflaeche wirken fuer reale Spielerzahlen nicht zu gross
- spaeter Event-NPCs/Hologramme/Teleportzugang festlegen, wenn das Community-Eventsystem umgesetzt wird

## 6. Legacy Hall

Erst sinnvoll nach einer abgeschlossenen Season. An die gewuenschte permanente Hall-of-Fame-Position stellen:

```text
/legacyhall set <Season> 1
/legacyhall set <Season> 2
/legacyhall set <Season> 3
```

## 7. Islands

Aktuelle Regeln:
- Schutzregion: **129 x 129**
- vom Island-Center jeweils **64 Bloecke** in X/Z
- Island-Center liegen **256 Bloecke** auseinander
- neue Inseln: klassische SkyBlock-Insel mit Baum + Startertruhe
- `/is level`
- `/is top`
- Besuch nur mit `[Welcome]`-Schild; `/is visit <Owner>` teleportiert direkt dorthin

### Nur fuer den aktuellen Pre-Release-Test
Eine bereits vor dem neuen Generator erzeugte Testinsel behaelt ihre alte Form. Um die neue Starterinsel wirklich zu sehen, Server stoppen und **nur wenn die Testdaten weg duerfen** loeschen:

```text
server/SkyIslands/
server/plugins/SkyKings-Core/islands.yml
```

Danach Server starten und `/is create` neu testen.

## 8. Plots

Zielmechanik wie ein klassisches PlotSquared-Raster:
- **65 x 65 Grasflaeche = genau eine Plotzelle**
- danach **7 Block Stone-Brick-Strasse = neutral, geschuetzt und gehoert niemandem**
- freie Plotzelle hat einen Holzstufen-Rand
- beim Claim wird der verwaltete Rand zur Steinstufe
- `/p rand` kann diesen Aussenrand mit permanent gekauften Coin-Cosmetics wechseln
- ein Merge entfernt nur die Strasse zwischen zwei verbundenen Zellen
- nicht gemergte Stone-Brick-Wege bleiben unantastbar

Spielerbefehle:
- `/p auto`
- `/p h`
- `/p sethome`
- `/p merge <nord|ost|sued|west>`
- `/p rand`
- `/p add <Spieler>` = Baurecht solange Owner online
- `/p trust <Spieler>` = dauerhaftes Baurecht
- `/p remove <Spieler>`
- `/p deny <Spieler>`
- `/p undeny <Spieler>`
- `/p flag pvp <an|aus>`
- `/p flag explosions <an|aus>`
- `/p flag fire <an|aus>`
- `/p flag mob-spawn <an|aus>`

### Sauberer Pre-Release-Reset

Alte generierte Chunks koennen das neue Raster und die Holzstufen nicht rueckwirkend uebernehmen. Server zuerst mit `stop` beenden und danach im Projektroot ausfuehren:

```powershell
.\scripts\reset-skyplots.ps1
```

Das Script verschiebt **SkyPlots + plots.yml + plot-borders.yml zuerst in einen timestamped Backup-Ordner** unter `server/backups/` und startet den Plot-Test danach sauber bei null.

Nach Neustart zwingend testen:
1. komplette Grasflaeche zwischen den Wegen claimbar
2. Stone-Brick-Weg nicht abbaubar/platzierbar
3. Weg meldet, dass er keinem Plot gehoert
4. fremde Grasflaeche nicht bebaubar
5. `/p rand` Kauf/Wechsel/Restart-Persistenz
6. auf eigenem Plot `/p merge ost` (oder andere Richtung)
7. genau die Strasse zum freien Nachbarplot verschwindet
8. entfernte Merge-Strasse danach bebaubar
9. alle anderen Strassen bleiben geschuetzt

## 9. PlayerShops

Kein kostenloses `/playershop create` mehr.

Testflow:
```text
/playershop kaufen
```
- SkyKings Haendler-Ei auf eigener Island/eigenem Plot platzieren
- `/playershop set <Menge> <Coins>`
- Verkaufsitem halten: `/playershop stock <Menge>`
- Kauf mit zweitem Spieler
- Owner-Rechtsklick -> Control Center
- Einnahmen claimen
- Stock withdraw testen

Haendler-Eier koennen auch aus Crates kommen.

## 10. Crates / Gutscheine

Testen:
- neue Voucher-Titel zeigen konkreten Inhalt
- technische Serial-/Batch-ID ist bei neu erzeugten Gutscheinen komplett aus der sichtbaren Lore entfernt
- alte Gutschein-Lore bleibt rueckwaertskompatibel einloesbar
- Rang/Rechte = Buch
- Kit = Papier
- Coin/GiveAll = Sonnenblume
- Coin-Rewards aus Crates kommen als Gutscheine
- GiveAll zahlt an alle gerade Online-Spieler
- Crate-Preview-Chancen gegen `crates.yml` pruefen

## 11. Announcements / Boden-Clear

- `/announcement <Text>` nutzt das einheitliche SkyKings-Broadcast-Design
- automatischer Boden-Clear entfernt alle 30 Minuten ausschliesslich gedroppte Item-Entities
- zwei Minuten vor dem Clear startet der Chat-Countdown
- zusaetzliche Hinweise bei 1 Minute, 30 Sekunden, 10 Sekunden und 5..1 Sekunden
- `/clear` startet denselben 2-Minuten-Countdown manuell
- waehrend ein Countdown laeuft darf kein zweiter gestartet werden
- nach dem Clear wird die Anzahl entfernter Items announced
- benoetigte Staff-Permission: `skykings.staff.clear`

## 12. Discord

Spaeter lokal setzen, niemals in GitHub committen:
- Umgebungsvariable `SKYKINGS_DISCORD_BOT_TOKEN`
- Channel IDs in `discord.yml`
- `/discordtest staff|audit|events|status`

## 13. Warps / Map-Travel

Finale Warp-Namen und Koordinaten **nicht raten**. Auf der echten Map an die gewuenschte Position stellen:

```text
/setwarp <Name>
```

Verwalten:
```text
/delwarp <Name>
```

Spieler:
- `/warp` oder `/warps` oeffnet die Auswahl
- `/warp <Name>` startet direkt die sichere 3-Sekunden-Schnellreise
- aktueller Combat blockiert den Warp
- Bewegung, Schaden oder neu beginnender Combat brechen den Countdown ab

Staff Map-Travel:
```text
/maptp main
/maptp plots
/maptp islands
/maptp events
/maptp community
```

Permissions:
- `skykings.admin.warps`
- `skykings.admin.maptp`

## 14. Noch offene Entwicklungs-/Balancepunkte

- Shoppreise final mit echter Economy balancen
- Community-Eventmap nach erstem Ingame-Test optisch manuell verfeinern
- Event-NPCs/Hologramme/Teleportzugang fuer Community-Events definieren
- echtes AMS/Mob-Stacking + Performanceprofil
- Clan Tag final in Chat/Tab integrieren, ohne Rangdarstellung zu zerstoeren
- Discord-Events aus allen Modulen final anschliessen
- kompletter Restart-/Dupe-/Load-/Backup-Test vor Release
- Battle-Pass-Economy/XP nach echtem Spieler-Test balancen
- optionaler eigener Resource-Pack-Layer fuer die Pixel-Battle-Pass-UI
