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
Ist architektonisch vorbereitet. Gameplay-Controller kommt noch. Vorgesehene Punkte spaeter:
- lobby
- a / b bzw. Match-Spawns
- spectator

### Juggernaut
Ist architektonisch vorbereitet. Gameplay-Controller kommt noch. Vorgesehene Punkte spaeter:
- lobby
- boss
- spawn1..spawn8
- spectator

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

Neue Zielmechanik:
- 65 x 65 Plot
- 7 Block breite Strassen
- `/p auto`
- `/p h`
- `/p add <Spieler>` = Baurecht solange Owner online
- `/p trust <Spieler>` = dauerhaftes Baurecht
- `/p remove <Spieler>`
- `/p deny <Spieler>`
- `/p undeny <Spieler>`
- `/p flag pvp <an|aus>`
- `/p flag explosions <an|aus>`
- `/p flag fire <an|aus>`
- `/p flag mob-spawn <an|aus>`

### Nur fuer den aktuellen Pre-Release-Test
Der alte `SkyPlots`-Weltordner enthaelt bereits Void-Chunks. Der neue Strassen-Generator kann bestehende Chunks nicht rueckwirkend ersetzen. Fuer einen echten Test der neuen Plot-Welt Server stoppen und **nur wenn Testdaten weg duerfen** loeschen:

```text
server/SkyPlots/
server/plugins/SkyKings-Core/plots.yml
```

Danach neu starten und `/p auto` testen.

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

## 13. Noch offene Entwicklungs-/Balancepunkte

- Shoppreise final mit echter Economy balancen
- Tournament Gameplay-Controller
- Juggernaut Gameplay-Controller
- Community-Eventmap nach erstem Ingame-Test optisch manuell verfeinern
- Event-NPCs/Hologramme/Teleportzugang fuer Community-Events definieren
- echtes AMS/Mob-Stacking + Performanceprofil
- Clan Tag final in Chat/Tab integrieren, ohne Rangdarstellung zu zerstoeren
- Discord-Events aus allen Modulen final anschliessen
- komplette Restart-/Dupe-/Load-Testserie vor Release
