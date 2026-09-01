# SkyKings — Sobald du wieder am PC bist

Diese Datei ist die feste Reihenfolge fuer den naechsten lokalen Testblock.

## 1. Server sauber stoppen

Falls er noch laeuft, in der Serverkonsole:

```text
stop
```

Warten, bis Java/Spigot wirklich beendet ist.

## 2. Projekt aktualisieren

PowerShell:

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
$env:Path += ";C:\Users\marti\OneDrive\Desktop\SkyKings\tools\apache-maven-3.9.16\bin"
mvn -version
```

## 3. Neue Plugins testen + bauen + deployen

```powershell
.\scripts\deploy-server.ps1
```

Das Script muss selbst abbrechen, falls Spigot noch laeuft. Maven-Tests und Build muessen komplett gruen sein.

## 4. SkyPlots fuer den neuen Raster-Test einmal sauber resetten

Nur im aktuellen Pre-Release-Test. Das Script legt vorher automatisch ein Backup an.

```powershell
.\scripts\reset-skyplots.ps1
```

Erwartetes Raster danach:
- 65x65 Grasflaeche = genau eine Plotzelle
- 7 Block Stone-Brick = neutrale, unantastbare Strasse
- freie Plotgrenze = Holzstufe
- geclaimte Plotgrenze = Steinstufe
- Strasse gehoert keinem Plot
- Merge entfernt nur die Strasse zwischen zusammengefuehrten Plotzellen

## 5. Server starten

```powershell
cd ".\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Bis `Done` warten und auf rote Exceptions achten.

## 6. Runtime-Systemcheck

Ingame:

```text
/skcheck
```

Kritische Module/Services/Commands muessen OK sein.

## 7. Plot-Test — zuerst machen

1. `/p auto`
2. Pruefen: Grasflaeche liegt exakt zwischen vier Stone-Brick-Strassen.
3. Pruefen: Strasse kann nicht abgebaut/platziert werden, auch nicht als OP.
4. Pruefen: fremde/unclaimed Grasflaeche kann nicht bebaut werden.
5. Pruefen: Claim-Rand ist Steinstufe.
6. `/p rand` oeffnen.
7. Rand-Kauf mit Confirmation testen.
8. Restart: gekaufter Rand bleibt freigeschaltet.
9. Auf eigenem Plot stehen: `/p merge ost`.
10. Nur die Strasse zum oestlichen Nachbarplot muss verschwinden.
11. Auf der ehemaligen Strasse muss danach gebaut werden koennen.
12. Andere Strassen bleiben neutral.
13. Spaeter 2x2-Merge testen und Kreuzung pruefen.

## 8. Battle Pass / Quests testen

```text
/battlepass
/battlepass rewards
/battlepass quests
/quests
```

Pruefen:
- Hub wirkt sauber und nicht wie eine Item-Wand
- Free/Premium-Track getrennt
- Seitenwechsel funktioniert
- READY / LOCKED / COMPLETED korrekt
- Rewards nur einmal claimbar
- Premium-Rewards ohne Premium gesperrt
- legitime PvP-Kills erhoehen Questfortschritt
- Anti-Farm-Kills zaehlen nicht fuer PvP-Quests
- Enderperlen-Quest funktioniert ausserhalb von Events
- King-Altar-Captures zaehlen
- Quest-Abschluss gibt Coins + SkyKings Sterne + Season-XP
- Restart-Persistenz testen

## 9. Inventory-Sync-Bug provozieren

Item droppen und SOFORT hintereinander Menues oeffnen:

```text
/commands
/kit
/battlepass
/warp
```

Das gedroppte Item darf nicht wieder im alten Slot erscheinen.

## 10. Event-/Buildmode-Schutz pruefen

Auf Event-/Mainmap:
- Buildmode AN -> Staff darf bauen
- Buildmode AUS -> normaler Map-Schutz
- keine Plot-Schutzmeldung ausserhalb von `SkyPlots`

## Wenn im Chat steht: "ich bin am PC"

Dann diese Datei als feste Reihenfolge verwenden und den Nutzer Schritt fuer Schritt durch den Test fuehren. Erst den Plot-/Runtime-Test abschliessen, bevor weitere riskante Systeme lokal aktiviert werden.
