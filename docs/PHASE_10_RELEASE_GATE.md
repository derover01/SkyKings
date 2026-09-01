# SkyKings Phase 10 — Release Gate

Phase 10 ist kein Feature-Dump. Ab hier wird jeder vorhandene Kernpfad unter realen Bedingungen abgesichert.

## Gate A — Build / Preflight

Server muss gestoppt sein.

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
$env:Path += ";C:\Users\marti\OneDrive\Desktop\SkyKings\tools\apache-maven-3.9.16\bin"
.\scripts\release-preflight.ps1
.\scripts\deploy-server.ps1
```

Erwartung:
- Java 8
- main branch
- Maven Tests komplett gruen
- vier SkyKings-JARs vorhanden
- kein laufender Spigot-Prozess

## Gate B — Boot / Runtime

```powershell
cd ".\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Danach ingame:

```text
/skcheck
```

Kein kritischer `[FEHLT]`-Eintrag erlaubt. Ein `[RECOVERY]`-Eintrag fuer Event Returns ist kein normaler Fehler, muss aber bewusst geprueft werden. Ein Jackpot-`[REVIEW]`-Status blockiert weitere Auszahlungen, bis Gewinner/Payout geprueft wurden.

## Gate C — Persistenz

Nach Aenderungen an mindestens Coins, Rank, Quest, Battle Pass, Plot, Rand, Clan und Warps:
1. Server sauber `stop`
2. Server neu starten
3. Daten vergleichen
4. keine doppelte Auszahlung / kein Reset

Zusätzlich pruefen:
- Jackpot mit laufendem Pot
- Map Mastery Zeit/Visits/Activities
- Event-Return-Recovery nach Quit/Death-Screen/Restart

## Gate D — Event Isolation

Mit mehreren Spielern testen:
- Duel
- LMS
- Clan Wars

Tournament und Juggernaut sind bewusst retired und gehoeren nicht zum finalen Feature-Set.

Muss fuer jedes isolierte Event gelten:
- Spieler kann nicht gleichzeitig einem zweiten isolierten Event beitreten
- Event-Kills werden nicht als normale Open-World-Kills/Bounties/Streaks gewertet
- kein `/spawn`, `/warp`, `/kit` oder anderes Escape aus dem laufenden Event
- kein Drop/Pickup-Transfer zur Manipulation
- Quit/Death fuehrt zu sauberem Forfeit/Elimination
- Rewards werden exakt einmal vergeben
- Rueckkehrposition bleibt auch bei Quit, Death-Screen und Serverrestart erhalten

Duel zusaetzlich:
- beide Spieler erhalten exakt das ausgewaehlte identische Kit
- Originalinventar, Armor, XP, Effekte und Vitalwerte werden sauber restauriert
- kein kostenloser Heal-/Hunger-Reset nach dem Duel
- Wager wird exakt einmal settled

## Gate E — Plot Regression

Nur auf frischem `SkyPlots`-Teststand:

```powershell
.\scripts\reset-skyplots.ps1
```

Muss gelten:
- Blockbereich Plot = 65x65 Gras
- Stone-Brick-Road = 7 Block neutral
- Roads gehoeren keinem Plot
- Roads nicht abbaubar/platzierbar
- Claim setzt Standardrand von Holzstufe auf Steinstufe
- `/p rand` Kauf + Persistenz
- Merge entfernt ausschliesslich die Road zwischen gemergten Zellen
- 2x2 Merge beseitigt innere Kreuzung nur bei vollstaendigem Verbund

## Gate F — Inventory / Economy Exploits

Gezielt provozieren:
- Item droppen -> sofort Command/GUI oeffnen
- Reward doppelklicken
- GUI spam-klicken
- Quit waehrend Claim/Kauf/Event
- Restart direkt nach Auszahlung
- unzureichende Coins bei Confirm
- Voucher erneut einloesen
- Trade abbrechen / Quit
- PlayerShop Kauf bei gleichzeitigem Stock-Withdraw
- Jackpot-Restart mit laufendem Pot

Keine Duplikation, kein negativer Bestand, keine doppelte Auszahlung.

## Gate G — Backup / Restore

Backup bei gestopptem Server:

```powershell
.\scripts\backup-server.ps1
```

Restore nur bewusst:

```powershell
.\scripts\restore-server-backup.ps1 -BackupPath ".\server\backups\server-YYYYMMDD-HHMMSS" -ConfirmRestore
```

Das Restore-Script erzeugt vorher automatisch ein weiteres Safety-Backup.

Nach Restore:
- Boot bis `Done`
- `/skcheck`
- Coins
- Clans
- Plots
- Battle Pass / Quests
- Event-Arena-Punkte
- Warps
- Jackpot
- Map Mastery
stichprobenartig vergleichen.

## Gate H — UI / Product Quality

Verbindlicher Look fuer grosse Systeme:
- dunkles SkyKings-Panel
- Hero/Header
- Tabs / Rails
- grosse Cards statt Item-Wand
- klare Free/Premium/Ready/Locked/Cooldown-Zustaende
- kurze Lore mit zentralem Word-Wrap
- konsistente Footer-Navigation

Battle Pass, Quest Center und Kit Arsenal sind Referenz fuer weitere UI-Passes.

## Gate I — Soft Launch

Erst wenn A–H bestanden sind:
- kleiner geschlossener Spieler-Test
- TPS / RAM / Entity-Zahlen beobachten
- Economy-Flows und Reward-Hoehen protokollieren
- Event-Balance mit echten Spielern anpassen
- erst danach Season 1 / groesserer Launch

Phase 10 gilt erst als abgeschlossen, wenn diese Gates real auf dem Spigot-Testserver bestanden wurden. CI allein reicht bewusst nicht.
