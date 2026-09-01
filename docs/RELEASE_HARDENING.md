# SkyKings Release Hardening

Stand: Phase 8/9 Systeme sind weitgehend verdrahtet; Phase 10 ist als echtes Release-Gate aktiv. Ein Feature gilt erst nach CI + Runtime-Test als release-fertig.

## 1. CI / Build Gate

GitHub Actions muss bei jedem Push auf `main`:

1. Java 8 laden
2. **alle Maven-Tests ausfuehren**
3. alle vier SkyKings-Module paketieren
4. nur bei Erfolg die JARs als Artifact hochladen

Lokaler Pre-Release-Build:

```powershell
$env:Path += ";C:\Users\marti\OneDrive\Desktop\SkyKings\tools\apache-maven-3.9.16\bin"
mvn -B -U package
```

`BUILD SUCCESS` ist Pflicht. `-DskipTests` ist fuer Release-Builds nicht erlaubt.

## 2. Boot Gate

1. `git pull`
2. `scripts/deploy-server.ps1`
3. Server mit Java 8 / Spigot 1.8.8 starten
4. auf `ERROR`, `SEVERE`, `Exception` achten
5. Ingame `/skcheck`

Erwartet:
- SkyKings-Core: OK
- SkyKings-Combat: OK
- SkyKings-Crates: OK
- SkyKings-Admin: OK
- LuckPerms: OK
- Vault: OK
- Core API: OK
- Island Access API: OK
- Plot Access API: OK
- SkyPvP: OK, sobald Produktionsmap geladen wurde
- SkyIslands: OK
- SkyPlots: OK

## 3. Kritische Dupe-/Inventory-Tests

### Drop -> Command Resync

Regression fuer den 1.8-Inventory-Ghost-Bug:
1. Item in festen Hotbar-/Inventory-Slot legen
2. Item mit Q droppen
3. sofort `/commands`, `/kit`, `/battlepass`, `/warp` oder anderes GUI-Kommando eingeben
4. Item darf **nicht** in den alten Slot zurueckspringen
5. gedropptes Item darf nur einmal in der Welt existieren
6. Test 20x schnell wiederholen

### Trade
- zwei Spieler, Items + Coins tauschen
- waehrend Countdown GUI schliessen
- waehrend Countdown disconnecten
- volles Zielinventar testen
- mehrfach schnell bestaetigen/klicken
- Serverstop waehrend offener Trade-Session simulieren

### PlayerShop
- Shop nur auf eigener Insel/eigenem Plot erstellbar
- fremde/trusted Claims testen
- Stock ein-/auszahlen
- Inventar voll beim Stock-Withdraw
- Kaeufer mit zu wenig Coins
- Kaeufer mit vollem Inventar
- Einnahmen claimen
- Shop mit Stock/Einnahmen darf nicht geloescht werden
- Villager darf keinen Schaden nehmen
- verschobener Villager muss deaktiviert sein

### SpawnerStack
- 1 -> 2 -> 64 stacken
- bei 64 darf kein Item verschwinden
- Stack abbauen: exakt dieselbe Anzahl Spawner muss droppen
- Serverrestart: Stackanzahl bleibt erhalten
- fremde Claims duerfen nicht veraendert werden
- Creature-Type in 1.8 separat testen

## 4. Economy / Persistenz

- `/sell hand`, `/sell all`
- Shops mit vollem Inventar
- Jackpot Neustart
- Daily Reward nur 1x pro Kalendertag
- BattlePass Reward nur 1x claimbar
- Quest Reward nur 1x pro Daily-/Weekly-Zyklus
- Premium BattlePass on/off + Neustart
- Plot-Rand einmal kaufen, danach kostenlos wechseln
- Plot-Rand Kauf mit zu wenig Coins
- Plot-Rand Besitz + Auswahl nach Neustart

## 5. Claim-Systeme

### Islands
- create
- home / sethome
- trust / untrust
- visit
- Owner/Trusted/Fremder
- Explosion, Feuer, Lava/Wasser ueber Claimgrenze
- Inselabstand und Rand

### SkyPlots – Blocker vor Release

Vor dem Test alte Pre-Refactor-Welt sauber sichern/resetten:

```powershell
.\scripts\reset-skyplots.ps1
```

Danach pruefen:
- eine komplette Grasflaeche zwischen Stone-Brick-Wegen = genau ein Plot
- freie Flaeche besitzt Holzstufen-Rand
- Claim wechselt Aussenrand auf Steinstufe
- Stone-Brick-Weg gehoert **keinem** Plot
- Owner, OP und Wildcard koennen neutralen Weg nicht versehentlich abbauen
- unclaimed Gras meldet unclaimed statt Fremdplot
- Fremdplot bleibt geschuetzt
- `/p rand` oeffnet Shop
- Kauf besitzt Confirm-Step
- gekaufter Rand bleibt permanent freigeschaltet
- verwalteter Rand kann nicht manuell zerstört werden

Merge:
- auf eigenem Plot stehen
- `/p merge ost` gegen freie Nachbarflaeche
- nur die 7-Block-Strasse zwischen diesen Zellen verschwindet
- entfernte Strasse wird Gras/Baugrund und gehoert zum Merge
- andere Strassen bleiben Stone Brick und neutral
- Merge gegen belegten Plot wird abgelehnt
- erneuter Merge derselben Zelle wird abgelehnt
- 2x2 Merge: Kreuzung erst entfernen, wenn alle vier Zellen verbunden sind
- Rand-Cosmetic liegt nur auf der aeusseren Kontur
- Home/Flags/Add/Trust gelten fuer den ganzen Merge

## 6. Map Gameplay / Buildmode

- Eventmap im Buildmode: bauen/abbauen moeglich, **keine Plot-Meldung**
- Eventmap ohne Buildmode: Map-Schutz greift
- `/skymap load SkyPvP`
- `/setspawn`
- `/mapsetup`
- King Altar
- Hot Zones
- End Zone
- Landmarks
- Map-Loot Common/Rare/Epic
- Supply-Punkte
- Secrets
- Pearl-/Jump-Routen
- Trash-Bins
- Map Displays

## 7. Battle Pass / Quests – Phase 8 Gate

### Hub/UI
- `/battlepass` oeffnet Pass Hub
- Your Pass / Quests / Rewards / Premium klar voneinander getrennt
- `/battlepass quests`
- `/battlepass rewards`
- Back/Home/Next auf allen Seiten
- Lore bleibt bei kleiner GUI-Aufloesung lesbar

### Reward Track
- 20 Milestones von Level 5 bis 100
- Free und Premium getrennt
- LOCKED -> READY -> COMPLETED
- Free Reward ohne Premium claimbar
- Premium Reward ohne Premium nicht claimbar
- Claim nur einmal
- Seite 1/2/3 Navigation
- Level-100-Finalbonus
- Restart-Persistenz

### Quest Center
Free:
- 5 legitime Kills
- 20 Pearls ausserhalb Events
- 5er Killstreak
- 30 legitime Weekly Kills
- 3 King-Altar Captures

Premium:
- 10 legitime Daily Kills
- 40 Pearls
- 75 Weekly Kills
- 7 King-Altar Captures

Integritaet:
- PvP-Fortschritt kommt nur aus validiertem `SkyKingsPlayerKillEvent`
- Fake-/Event-/nicht verarbeitete Kills zaehlen nicht
- King Altar zaehlt ueber das zentrale Capture-Event
- Quest Completion gibt Coins + physische SkyKings Sterne + Season-XP
- Season-Level aktualisiert sich nach Quest-XP
- Daily/Weekly Reset korrekt

## 8. Community / Events – Phase 9 Gate

Bereits verdrahtete Systeme einzeln testen:
- `/peace <Spieler>` accept/deny/remove
- Peace-Hits ohne Schaden/CombatTag
- `/duel` Wager/Escrow + Arena-Rueckgabe
- LMS Join/Start/Elimination/Reward
- King Altar / KOTH
- Most Wanted / Target Event
- `/verlosung start <Sekunden> <Coins>` + `/verlosung join`
- Community-/Giveaway-Map ohne PvP

Noch nicht als Gameplay-fertig deklarieren:
- Tournament Controller/Brackets
- Juggernaut Controller
- Clan-Wars Controller

Die Arenabereiche duerfen bereits gebaut/vorbereitet sein; ohne Controller sind diese drei Roadmap-Punkte weiterhin offen.

## 9. Crates / Voucher Security

- Preview-Chancen gegen Config
- Animation / Disconnect waehrend Open
- volles Inventar
- Voucher Serial nur intern
- alte Voucher-Lore kompatibel
- gleicher Voucher darf nicht mehrfach eingeloest werden
- GiveAll nur einmal pro Redemption
- Rank/Rechte/Kit/Coins jeweils separat testen

HMAC/Issued-Serial-Registry bleibt ein Release-Hardening-Kandidat, solange Replay-Schutz nicht kryptografisch abgeschlossen ist.

## 10. Warps / Travel

- `/warp` und `/warps`
- `/warp <Name>` ohne GUI
- 3-Sekunden-Countdown
- Combat blockiert Start
- Combat waehrend Countdown bricht ab
- Bewegung bricht ab
- Schaden bricht ab
- Logout/Tod bricht ab
- `/maptp main|plots|islands|events|community`
- unloaded event/community world sauber testen

## 11. Backup / Restore Gate

Vor Release einmal echte Wiederherstellung testen:
- Server stoppen
- Welt-/Plugin-Daten sichern
- gezielt eine Testdatei veraendern/entfernen
- Backup zurueckspielen
- Server starten
- Coins, Claims, BattlePass, Crates und Shops kontrollieren

Der Plot-Reset-Helper erzeugt fuer seinen Scope bereits ein timestamped Backup unter `server/backups/`.

## 12. Performance / Load Gate

Mindestens:
- 30–60 Minuten Soak-Test
- Scoreboard + Hologramme + Shops + MapLoot gleichzeitig
- viele Item-Entities vor Ground Clear
- mehrere PlayerShops geladen
- Spawner-Stapel
- wiederholte GUI-Oeffnungen
- wiederholte Warp-Abbrueche
- Event mit mehreren Spielern

Beobachten:
- TPS
- Heap
- Entity Count
- Scheduler-Warnungen
- Exceptions
- merkbare Inventory-Desyncs

## 13. Release Gate – Phase 10

Public/Soft-Launch erst, wenn mindestens gruen:
- GitHub CI inklusive Tests
- kompletter Serverboot ohne Exception
- `/skcheck`
- Plot-Raster + Road + Merge Regression
- BattlePass/Quest Persistenz
- Drop->Command Dupe-/Ghost-Test
- 2-Spieler Trade-Test
- 2-Spieler PvP/Peace-Test
- PlayerShop Dupe-Test
- SpawnerStack Mengen-Test
- Crate/Voucher Replay-Test
- Warp Combat-Abbruch
- Restart-Persistenztest
- Backup/Restore-Test
- 30–60 Minuten Soak-Test
- finaler Economy-/XP-Balancepass

Erst danach Season 1 als release-faehig markieren.
