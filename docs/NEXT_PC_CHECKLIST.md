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

## 3. Alles vorbereiten: Preflight + Tests + Build + Deploy + einmaliger Plot-Reset

Fuer den ersten Test des neuen Plot-Rasters genau einmal:

```powershell
.\scripts\prepare-local-test.ps1 -ResetSkyPlots
```

Der Befehl fuehrt Release-Preflight, statischen Release-Audit, Maven-Tests, Build und Deploy aus. Mit `-ResetSkyPlots` wird die alte Testwelt vorher sicher in einen timestamped Backup-Ordner verschoben.

Bei spaeteren Testlaeufen ohne Plot-Reset reicht:

```powershell
.\scripts\prepare-local-test.ps1
```

Erwartetes Plot-Raster:
- 65x65 Grasflaeche = genau eine Plotzelle
- 7 Block Stone-Brick = neutrale, unantastbare Strasse
- freie Plotgrenze = Holzstufe
- geclaimte Plotgrenze = Steinstufe
- Strasse gehoert keinem Plot
- Merge entfernt nur die Strasse zwischen zusammengefuehrten Plotzellen

## 4. Server starten

```powershell
cd ".\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Bis `Done` warten und auf rote Exceptions achten.

## 5. Runtime-Systemcheck

Ingame:

```text
/skcheck
```

Kritische Module/Services/Commands muessen OK sein. Der Check umfasst Core API, CombatTag, Clan Service, Event Participation, Discord Bridge, Duel, LMS und Clan Wars. Discord-Channels duerfen OPTIONAL sein, solange Discord noch nicht eingerichtet ist.

Falls beim Jackpot nach einem unsauberen Abbruch ein Recovery-Fall erkannt wurde, muss `/skcheck` deutlich `REVIEW` anzeigen. In diesem Zustand nicht blind weiter auszahlen, sondern Gewinner/Payout zuerst pruefen.

## 6. Plot-Test — zuerst machen

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

## 7. Battle Pass / Quests / Kits / Crates / Commands testen

```text
/battlepass
/battlepass rewards
/battlepass quests
/quests
/kit
/craterewards
/commands
```

Pruefen:
- alle grossen Menues folgen demselben Custom-Panel-Stil
- Hub wirkt sauber und nicht wie eine Item-Wand
- Free/Premium-Track getrennt
- Daily/Weekly/Premium-Tabs klar erkennbar
- Kit Arsenal zeigt READY / LOCKED / COOLDOWN sauber
- Crate Center zeigt Rang-Rail und Reward-Cards sauber
- Kit-Preview und direkter Claim funktionieren
- Seitenwechsel und Zurueck/Home funktionieren
- Rewards nur einmal claimbar
- Premium-Rewards ohne Premium gesperrt
- legitime PvP-Kills erhoehen Questfortschritt
- Anti-Farm-Kills zaehlen nicht fuer PvP-Quests
- Enderperlen-Quest funktioniert ausserhalb von Events
- King-Altar-Captures zaehlen
- Quest-Abschluss gibt Coins + SkyKings Sterne + Season-XP
- Restart-Persistenz testen

## 8. Prefix / Clan-Tag / Chat testen

```text
/prefix
```

Pruefen:
- kosmetischen Prefix separat AN/AUS schalten
- Rang neben kosmetischem Prefix separat AN/AUS schalten
- Rang bleibt ohne kosmetischen Prefix sichtbar
- Clan-Mitglieder zeigen den Clan-Tag sauber im Chat
- im Tab steht kein Clan-Tag; Rang + vollstaendiger Spielername muessen sichtbar bleiben
- auch lange Spieler-/Rangnamen duerfen den Spielernamen im Tab niemals abschneiden
- Spieler ohne Clan haben keinen leeren/kaputten Platzhalter

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

## 11. Duel / LMS Multiplayer-Gate

Duel-Arena setzen:

```text
/eventarena set duel a
/eventarena set duel b
/eventarena set duel spectator
```

LMS-Arena setzen:

```text
/eventarena set lms lobby
/eventarena set lms spawn1
/eventarena set lms spawn2
/eventarena set lms spawn3
/eventarena set lms spawn4
/eventarena set lms spectator
```

Danach Duel testen:
- Duel normal und mit Coin-Wager
- Duel-Setup-GUI: Gegner, identisches Kit fuer beide, Einsatz und Challenge
- vor dem Duel Health/Hunger/Saturation notieren; Sieger darf danach keinen kostenlosen Heal/Food-Reset behalten
- Originalinventar, Armor, Hotbar-Slot, XP und Potion-Effekte nach Kit-Duel exakt vergleichen
- Quit waehrend Duel
- Tod im Duel und normal respawnen
- auf dem Death-Screen ausloggen, wieder verbinden und erst danach respawnen
- Server `stop` waehrend eines aktiven Duels: kein Kit-Inventar und kein doppelter Wager darf uebrig bleiben
- Wager-Auszahlung exakt einmal
- kein Drop/Pickup/Command-Escape waehrend des Duels

Danach LMS testen:
- LMS Join/Leave/Start/Stop
- LMS-Elimination und letzter Spieler
- normales Spielerinventar darf beim LMS-Tod niemals geloescht werden
- Tod -> Respawn muss an die gespeicherte Rueckkehrposition fuehren
- Quit waehrend laufendem LMS zaehlt als Aufgabe und Spieler darf beim Join nicht in der Event-Arena festhaengen
- Quit auf dem Death-Screen und anschliessender Join/Respawn
- Staff-Stop waehrend Teilnehmer lebt bzw. auf Death-Screen liegt
- Event-Kills bleiben aus normalen PvP-Stats/Streaks/Bounties heraus
- keine Command-/Drop-/Pickup-Umgehung

## 12. Clan Wars Multiplayer-Gate

Mindestens zwei echte Clans mit jeweils 2+ Online-Mitgliedern erstellen. Beide Clan-Owner muessen online sein.

Arena einmalig setzen:

```text
/eventarena set clanwar a1
/eventarena set clanwar a2
/eventarena set clanwar b1
/eventarena set clanwar b2
```

Fuer 3v3 bis 5v5 optional `a3..a5` und `b3..b5` setzen.

Challenge:

```text
/clanwar <gegnerischerOwner>
/clanwar accept
/clanwar status
```

Pruefen:
- 2v2, spaeter 3v3 und 5v5
- eigene Clanmitglieder koennen sich nicht treffen
- nur Gegner koennen Schaden machen
- tote Spieler sind eliminiert und kehren sauber ueber den Respawn zurueck
- Quit zaehlt als Ausscheiden
- nach Quit und erneutem Join muss die urspruengliche Rueckkehrposition wiederhergestellt werden
- Quit auf dem Death-Screen darf die Rueckkehrposition nicht verlieren
- Match-Ende/Staff-Stop darf noch ausstehende Death-Screen-Returns nicht loeschen
- letzter Clan gewinnt
- Siegerreward exakt einmal
- Clan-War-Kills laufen nicht in normale Open-World-Stats
- `/clanwar stop` mit Staff funktioniert

Tournament und Juggernaut gehoeren bewusst nicht mehr zum Feature-Set und werden nicht getestet/eingerichtet.

## 13. Spawner-/Mob-Stacking testen

Auf eigener Island und eigenem Plot:
- mehrere Spawner stacken
- Spawner-Mobs derselben Art in der Naehe muessen zu einem Mob-Stack zusammengehen
- sichtbarer Stackcounter stimmt
- ein Kill reduziert den Stack genau um 1
- Drops/XP nur fuer den einen Kill
- grosse Farm auf TPS/Entity-Anzahl beobachten
- Chunk unload/reload und Serverrestart testen

## 14. Crate/Voucher Security-Gate

Pruefen:
- schneller Doppelklick / mehrfacher Rechtsklick
- Shift-Klick / Inventar verschieben waehrend der Einloesung
- Voucher/Crate direkt nach Klick droppen oder Slot wechseln
- Voucher kopieren und zweimal versuchen
- Restart direkt nach Einloesung bzw. Claim-Reservierung
- Reward darf pro Serial/Batch nur einmal kommen
- bereits eingelöste kopierte Serial bleibt auch nach Restart wertlos

## 15. PlayerShop / Trade Transaction-Gate

PlayerShop:
- zweiter Spieler kauft waehrend Owner Stock entnimmt
- Revenue claimen und sofort erneut versuchen
- Restart direkt nach Kauf/Claim testen
- wenn moeglich Save-Fehler simulieren: kein Item-/Coin-Dupe und keine doppelte Revenue-Auszahlung

Trade:
- Items und Coins anbieten
- beide bestaetigen
- waehrend Countdown Angebot oder Coins aendern
- beide muessen danach erneut bestaetigen
- nach erneuter Bestaetigung mindestens volle 3 Sekunden warten: ein alter Countdown darf den neuen Trade nicht vorzeitig abschliessen
- einen Coin-Kontostand waehrend Countdown absichtlich unter das Angebot bringen; Confirmations muessen zurueckgesetzt werden
- Quit/Inventory-Close waehrend Trade
- keine doppelte Auszahlung / keine verlorenen Escrow-Items

## 16. Jackpot / Map Mastery Runtime-Gate

Jackpot:

```text
/jackpot
```

Pruefen:
- mindestens zwei echte Teilnehmer; mit nur einem Teilnehmer darf keine normale Gewinnerziehung stattfinden
- unterschiedliche Einzahlungen und angezeigte Gewinnchance vergleichen
- Auszahlung exakt einmal, inklusive 5%-Sink
- Command und gebundener Jackpot-NPC zeigen denselben Pot / dieselbe Runde
- `/shopnpc bind jackpot` an Test-Villager pruefen
- Restart mit laufendem Pot: Einsatzdaten bleiben konsistent
- Crash-/Recovery-Situation pruefen; unsicherer Zustand muss ueber `/skcheck` als REVIEW sichtbar sein

Map Mastery:

```text
/mapmastery
```

Pruefen:
- Zeit an Landmark/Island erhoeht sich nur am passenden Ort
- Visits zaehlen plausibel und nicht sekundenweise als neue Besuche
- abgeschlossene Gold-/Level-Aktivitaeten zaehlen als Activities
- Hot-Zone-Kills, King-Captures, End-Kills und Secrets bleiben sichtbar
- Restart: Zeit, Visits und Activities bleiben erhalten

## 17. Discord-Gate (nur wenn eingerichtet)

Wenn `SKYKINGS_DISCORD_BOT_TOKEN` und Channel-IDs gesetzt sind:

```text
/discordtest events
/discordtest status
```

Pruefen:
- Start/Stop-Status kommt im Status-Channel an
- King-Altar-Capture kommt im Events-Channel an
- 10er/25er/50er/100er legitime Killstreak wird gemeldet
- Anti-Farm-Kills erzeugen keinen Discord-Meilenstein

## 18. Phase-10 Backup-/Restart-Gate

Vor groesseren Daten-Tests:

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
.\scripts\backup-server.ps1
```

Danach mindestens einmal:
- Coins/Quest/Pass/Plot/Clan Daten veraendern
- PlayerShop-Revenue, Jackpot und Map-Mastery-Daten veraendern
- Server sauber `stop`
- neu starten
- Persistenz pruefen
- Backup -> Daten veraendern -> Guarded Restore -> Datenstand vergleichen

## 19. Finale Balance / Soft-Launch-Gate

Erst wenn die technischen Gates sauber sind:
- Economy und Shoppreise
- Kit-Cooldowns
- Battle-Pass-XP und Rewards
- Crate-Wahrscheinlichkeiten
- Event-Rewards
- Mob-/Spawner-Limits
- reale Spielerzahl / TPS

Der Resource-Pack-Layer fuer die Custom UI kommt danach als Polish und ist kein Blocker fuer den ersten stabilen Serverstand.

## Wenn im Chat steht: "ich bin am PC"

Dann diese Datei als feste Reihenfolge verwenden und den Nutzer Schritt fuer Schritt durch den Test fuehren. Erst Plot-/Runtime-/Persistenztests abschliessen, bevor weitere riskante Systeme lokal aktiviert werden.
