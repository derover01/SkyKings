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

## 3. Alles vorbereiten

Normaler Testlauf ohne Plot-Reset:

```powershell
.\scripts\prepare-local-test.ps1
```

Nur fuer einen bewusst komplett frischen Plot-Raster-Test:

```powershell
.\scripts\prepare-local-test.ps1 -ResetSkyPlots
```

Erwartetes Plot-Raster:
- 65x65 Grasflaeche = genau eine Plotzelle
- 7 Block Stone-Brick = neutrale, unantastbare Strasse
- freie Plotgrenze = Holzstufe
- geclaimte Plotgrenze = Steinstufe
- WICHTIG: Plotboden liegt auf Y=64, der sichtbare Rand exakt eine Blockebene hoeher auf Y=65
- unter dem Rand bleibt Gras; der Rand ersetzt nicht mehr den Plotboden
- ueber der 7-Block-Strasse bleibt Y=65 frei
- Strasse gehoert keinem Plot
- Merge entfernt nur die Strasse und den internen erhoehten Rand zwischen zusammengefuehrten Plotzellen
- bestehende Claims werden beim Start auf das erhoehte Randmodell migriert; dafuer ist kein Plot-Reset noetig

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
/skymap list
```

Kritische Module/Services/Commands muessen OK sein. Discord-Channels duerfen OPTIONAL sein, solange Discord noch nicht eingerichtet ist.

Falls beim Jackpot nach einem unsauberen Abbruch ein Recovery-Fall erkannt wurde, muss `/skcheck` deutlich `REVIEW` anzeigen. In diesem Zustand nicht blind weiter auszahlen, sondern Gewinner/Payout zuerst pruefen.

## 6. Plot-Test — zuerst machen

1. Bestehenden Claim ansehen oder `/p auto` nutzen.
2. Pruefen: Grasflaeche liegt exakt zwischen vier Stone-Brick-Strassen.
3. Pruefen: der Holz-/Stein-/Cosmetic-Rand steht sichtbar EINE Blockebene ueber dem Gras.
4. Pruefen: unter dem Rand liegt Gras und nicht der Randblock selbst.
5. Pruefen: ueber der Strasse steht kein Randblock.
6. Pruefen: Rand ist exakt eine Blockreihe breit und liegt auf der aeussersten Plot-Koordinatenreihe.
7. Pruefen: Strasse kann nicht abgebaut/platziert werden, auch nicht als OP.
8. Pruefen: fremde/unclaimed Grasflaeche kann nicht bebaut werden.
9. `/p rand` oeffnen und Cosmetic-Rand wechseln/kaufen.
10. Jeder Cosmetic-Rand muss ebenfalls auf Y+1 liegen.
11. Restart: gekaufter Rand bleibt freigeschaltet und auf richtiger Hoehe.
12. Auf eigenem Plot stehen: `/p merge ost`.
13. Nur die Strasse zum oestlichen Nachbarplot und der interne Rand muessen verschwinden.
14. Auf der ehemaligen Strasse muss danach gebaut werden koennen.
15. Andere Strassen und Aussenraender bleiben neutral/erhoeht.
16. Spaeter 2x2-Merge testen und Kreuzung pruefen.

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
- jedes Level 1-100 hat einen Free-Reward und einen zusaetzlichen Premium-Reward
- Daily/Weekly/Premium-Tabs klar erkennbar
- Kit Arsenal zeigt READY / LOCKED / COOLDOWN sauber
- Crate Center zeigt Rang-Rail und Reward-Cards sauber
- Kit-Preview und direkter Claim funktionieren
- Seitenwechsel und Zurueck/Home funktionieren
- Rewards nur einmal claimbar
- Premium-Rewards ohne Premium gesperrt
- `/premiumpass give <Spieler>` und `/premiumpass remove <Spieler>` testen
- legitime PvP-Kills erhoehen Questfortschritt
- Anti-Farm-Kills zaehlen nicht fuer PvP-Quests
- Enderperlen-Quest funktioniert ausserhalb von Events
- King-Altar-Captures zaehlen
- Duel-Sieg, echter Crate-Open, Bounty-Claim und Rare/Epic Map Chest pruefen
- Quest-Abschluss gibt Coins + SkyKings Sterne + Season-XP
- Restart-Persistenz testen

## 8. Prefix / Clan-Tag / Chat testen

```text
/prefix
```

Pruefen:
- kosmetischen Prefix separat AN/AUS schalten
- Rang im Chat separat AN/AUS schalten, unabhaengig vom kosmetischen Prefix
- Clan-Tag im Chat separat AN/AUS schalten
- Kombinationen duerfen keine leeren `|`-Trenner erzeugen
- direkte Commands `/prefix prefix an|aus`, `/prefix rang an|aus`, `/prefix clan an|aus` pruefen
- `/prefix an|aus` schaltet nur den kosmetischen Prefix
- im Tab steht kein Clan-Tag; Rang + vollstaendiger Spielername muessen sichtbar bleiben
- auch lange Spieler-/Rangnamen duerfen den Spielernamen im Tab niemals abschneiden
- Spieler ohne Clan haben keinen leeren/kaputten Platzhalter

## 9. Spawn / Warp Sound & Inventory-Sync testen

Spawn:

```text
/spawn
```

Pruefen:
- Start des 3-Sekunden-Teleports hat Sound
- erfolgreicher Teleport hat Teleport- + Success-Sound
- Bewegung/Schaden/Combat-Abbruch hat Error-Sound
- bereits laufender Teleport hat Warning-Sound

Danach Item droppen und SOFORT hintereinander Menues oeffnen:

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

## 11. Freitags-Community-Event Runtime-Gate

Einmalig an der gewuenschten Mitte der Drop-Area:

```text
/setwarp Event
```

Ohne diesen Warp darf `/freitag` nicht starten.

Kompletten Ablauf testen:

```text
/freitag
/freitag status
```

Pruefen:
- serverweite Intro-Announcement
- starke, aber nicht dauerhaft nervige Sounds
- Feuerwerk an der Event-Area
- automatische Ziehung unter Online-Spielern
- Auto-Reward ist Coins, SkyKings Sterne oder eine echte server-issued Crate
- Crate-Fallback verursacht keinen Fehler, falls Crates nicht bereit waeren

Danach als Staff einen Item-Stack inkl. gewuenschter Menge in die Hand nehmen:

```text
/verlosen
```

Pruefen:
- exakter Item-Typ, Meta/Enchantments und Stackmenge werden als Gewinn verwendet
- 3-2-1-Countdown mit Sound
- genau ein Gewinner bekommt den Gewinn
- mehrere `/verlosen`-Runden nacheinander funktionieren
- waehrend einer laufenden Ziehung kann nicht parallel eine zweite gestartet werden

Manuelle Phase beenden:

```text
/verlosen fertig
```

Pruefen:
- klare Drop-Event-Announcement mit `/warp Event`
- 15-Sekunden-Countdown
- Drop-Event startet nur am gespeicherten Event-Warp
- ca. 42 hochwertige Drops kommen verteilt von oben
- dabei echte Crates mit gueltiger Issued-Serial, PvP-Schwerter, God Apples, Gear, Pearls, Blocks, XP/Potions
- keine Fake-/ungelisteten Crates
- `/freitag stop` bricht einen laufenden Ablauf/Tasks sauber ab
- Server `stop` waehrend des Freitags-Events darf keine Tasks nach Restart wiederbeleben

## 12. Duel / LMS Multiplayer-Gate

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

## 13. Clan Wars Multiplayer-Gate

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

## 14. Spawner-/Mob-Stacking testen

Auf eigener Island und eigenem Plot:
- mehrere Spawner stacken
- Spawner-Mobs derselben Art in der Naehe muessen zu einem Mob-Stack zusammengehen
- sichtbarer Stackcounter stimmt
- ein Kill reduziert den Stack genau um 1
- Drops/XP nur fuer den einen Kill
- grosse Farm auf TPS/Entity-Anzahl beobachten
- Chunk unload/reload und Serverrestart testen

## 15. Crate/Voucher Security-Gate

Pruefen:
- schneller Doppelklick / mehrfacher Rechtsklick
- Shift-Klick / Inventar verschieben waehrend der Einloesung
- Voucher/Crate direkt nach Klick droppen oder Slot wechseln
- Voucher kopieren und zweimal versuchen
- Restart direkt nach Einloesung bzw. Claim-Reservierung
- Reward darf pro Serial/Batch nur einmal kommen
- bereits eingeloeste kopierte Serial bleibt auch nach Restart wertlos

## 16. PlayerShop / Trade Transaction-Gate

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

## 17. Jackpot / Map Mastery Runtime-Gate

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

## 18. Discord-Gate (nur wenn eingerichtet)

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

## 19. Phase-10 Backup-/Restart-Gate

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

## 20. Finale Balance / Soft-Launch-Gate

Erst wenn die technischen Gates sauber sind:
- Economy und Shoppreise
- Kit-Cooldowns
- Battle-Pass-XP und Rewards
- Crate-Wahrscheinlichkeiten
- Event-/Freitags-Rewards
- Mob-/Spawner-Limits
- reale Spielerzahl / TPS

Der Resource-Pack-Layer fuer die Custom UI kommt danach als Polish und ist kein Blocker fuer den ersten stabilen Serverstand.

## Wenn im Chat steht: "ich bin am PC"

Dann diese Datei als feste Reihenfolge verwenden und den Nutzer Schritt fuer Schritt durch den Test fuehren. Der Nutzer gilt aktuell als am PC, bis er ausdruecklich schreibt, dass er nicht mehr am PC ist.
