# SkyKings Command & Setup Guide

Stand: 2026-09-01

Diese Datei ist die dauerhaft gepflegte Command-Quelle fuer Spieler, Staff und Server-Setup. Neue Commands muessen hier dokumentiert werden; der statische Release-Audit prueft das automatisch.

## 1. Hilfe & Navigation

- `/commands` - zentraler SkyKings Help Hub. Aliases: `/befehle`, `/cmds`.
- `/spawn` - 3-Sekunden-Teleport zum globalen Spawn; Combat/Bewegung/Schaden koennen abbrechen. Countdown, Fehler und erfolgreicher Teleport haben Soundfeedback.
- `/warp` - Warp-GUI.
- `/warp <Name>` - 3-Sekunden-Direktreise zu einem Warp.

## 2. Kits, Ranks & Perks

- `/kit [name]` - Kit Arsenal bzw. konkretes Kit.
- `/rankup` - naechsten Free-Rang mit Coins kaufen.
- `/raenge` - Rang-Uebersicht. Aliases: `/ränge`, `/ranks`, `/rang`.
- `/fly` - Flugmodus, rang-/permissionabhaengig.
- `/speed <1-10|reset>` - Fluggeschwindigkeit.
- `/stack` - stackbare Inventaritems verdichten.
- `/bloecke` - No-Sell-Baubloecke fuer berechtigte Raenge.
- `/repair` - gehaltenes Item reparieren.
- `/enderchest` - persoenliche mehrseitige Enderchest. Alias: `/ec`.
- `/anvil` - portabler Amboss.
- `/workbench` - portable Werkbank. Aliases: `/wb`, `/craft`.
- `/enchantmenttable` - portabler Verzauberungstisch.
- `/trash` - temporaerer Muellcontainer.

## 3. Economy, Shops & Handel

- `/shop` - zentraler Systemshop.
- `/worth` - Verkaufswert des gehaltenen Items.
- `/sell <hand|all>` - whitelisted Items verkaufen.
- `/trade <Spieler|accept|deny>` - sicherer Zwei-Spieler-Handel mit Items und Coins.
- `/playershop <kaufen|set|stock|withdraw|claim|info|remove>` - PlayerShop-Verwaltung.
- `/shoprent` - Market-Rentals-GUI mit geschuetzten Mietstaenden.
- `/shoprent rent <Stand>` - freien Marktstand fuer Coins mieten.
- `/shoprent release <Stand>` - eigene Miete vorzeitig beenden; keine anteilige Rueckerstattung.
- `/shoprent tp <Stand>` - zum Marktstand teleportieren.
- `/shoprent pos1` / `/shoprent pos2` - Staff: Cuboid-Ecken fuer einen Mietstand setzen.
- `/shoprent create <ID> [Preis] [Stunden]` - Staff: Mietstand persistent anlegen. Standard: 500.000 Coins / 24h; max. 168h.
- `/shoprent remove <ID>` / `/shoprent list` - Staff: Mietstand entfernen bzw. anzeigen.
- `/dailyrewards` - taeglichen Reward abholen.
- `/jackpot` - Coin-Jackpot-GUI.
- `/jackpot <Coins>` - optional direkter Jackpot-Einsatz.
- `/spawnerstack` - Stackgroesse des angesehenen Spawners anzeigen. Aliases: `/spawner`, `/ams`.

Mietstand-Regel: Nur der aktive Mieter darf im definierten Cuboid bauen und dort neue PlayerShops platzieren. Laeuft die Miete ab, verkauft der dortige PlayerShop nicht weiter; der letzte Mieter darf vorhandenen Stock/Einnahmen aber noch aufraeumen und den Shop entfernen.

## 4. Islands & Plots

- `/island` - registrierter Hauptcommand fuer das Island-System. Alias: `/is`.
- `/is` - Island Hub.
- `/is create` - Island erstellen.
- `/is home` - zur Island teleportieren.
- `/is sethome` - Island-Home setzen.
- `/is level` - Island-Level.
- `/is top` - Island-Topliste.
- `/is trust <Spieler>` - dauerhafte Rechte.
- `/is untrust <Spieler>` - Trust entfernen.
- `/is visit <Spieler>` - Island besuchen.
- `/is info` - Island-Info.

- `/plot` - Plot Hub. Alias: `/p`.
- `/p auto` - freien Plot claimen.
- `/p home` - Plot-Home.
- `/p sethome` - Plot-Home setzen.
- `/p add <Spieler>` - Owner-abhaengige Baurechte.
- `/p trust <Spieler>` - dauerhafte Baurechte.
- `/p remove <Spieler>` - Rechte entfernen.
- `/p deny <Spieler>` / `/p undeny <Spieler>` - Zutritt sperren/freigeben.
- `/p flags` - Plot-Flags anzeigen.
- `/p flag <pvp|explosions|fire|mob-spawn> <an|aus>` - Flag aendern.
- `/p visit <Spieler>` - Plot besuchen.
- `/p info` - Plotinfo.
- `/p merge <nord|ost|sued|west>` - angrenzende Plotzellen verbinden; nur die Zwischenstrasse wird Teil des Merges.
- `/p rand` - Plot-Rand-Shop / freigeschaltete Rand-Cosmetics wechseln. Der sichtbare Rand liegt eine Blockebene ueber dem Grasboden; die 7-Block-Strasse bleibt frei.

## 5. Clan & Social

- `/clan` - Clan Hub.
- `/clan create <Name>` - Clan erstellen.
- `/clan invite <Spieler>` - Spieler einladen.
- `/clan accept` - Einladung annehmen.
- `/clan leave` - Clan verlassen.
- `/clan kick <Spieler>` - Mitglied entfernen.
- `/clan disband` - Clan aufloesen.
- `/clan info` - Claninfo.
- `/peace <Spieler|accept|deny|remove|list>` - gegenseitiges Peace-System. Aliases: `/friede`, `/friend`.

## 6. Profil, Progression & Cosmetics

- `/stats [Spieler]` - Spielerprofil/PvP-Stats.
- `/top` - Leaderboards und Bounty Board. Aliases beinhalten `/kopfgeld`, `/bounty`.
- `/collection [Seite]` - Head Collection.
- `/achievements` - Achievements.
- `/mapmastery [Spieler]` - Landmark-Zeit/Besuche/Aktivitaeten plus Map-Combat-/Discovery-Mastery.
- `/season` - Season-XP.
- `/pvplevel` - PvP-Level 1-100.
- `/medals [Spieler]` - permanente Medaillen.
- `/legacyhall` - Hall of Fame.
- `/battlepass [rewards|quests]` - Battle Pass Hub / Reward Track / Quests. Jedes Level 1-100 hat einen Free-Reward; Premium hat pro Level einen zusaetzlichen Reward.
- `/premiumpass give <Spieler>` - Staff: Premium Pass vergeben.
- `/premiumpass remove <Spieler>` - Staff: Premium Pass entfernen.
- `/quests [daily|weekly|premium]` - Quest Center.
- `/killeffect` - Cosmetics Center fuer Kill Effects und Death Messages.
- `/stattrack [apply|give]` - Weapon History / StatTrack.
- `/seasonadmin finish` - Admin: aktuelle Season sicher abschliessen und Hall of Fame/Medaillen archivieren.

## 7. PvP & Community Events

- `/duel <Spieler>` - Duel Setup Panel mit Gegner, Kit und Einsatz.
- `/duel <Spieler> <Coins> [Arena]` - direkter Legacy-/Wager-Startpfad.
- `/duel accept` / `/duel deny` - Challenge annehmen/ablehnen.
- `/lms <join|leave|start|stop>` - Last Man Standing.
- `/clanwar <Clan-Owner|accept|deny|status|stop>` - sichere 2v2- bis 5v5-Clan Wars.
- `/targetevent <status|start|stop> [Spieler]` - Most Wanted.
- `/verlosung <join|start|stop>` - normale serverweite Coin-Verlosung.
- `/freitag` - Staff: startet den kompletten Freitags-Community-Abend mit Intro, Auto-Verlosung, manueller Gewinnphase und Drop-Event.
- `/freitag status` - aktuelle Freitags-Eventphase anzeigen.
- `/freitag stop` - laufenden Freitags-Flow und seine Tasks sauber abbrechen.
- `/verlosen` - waehrend der Freitags-Gewinnphase den aktuell gehaltenen Item-Stack inklusive Menge an einen zufaelligen Online-Spieler verlosen.
- `/verlosen fertig` - manuelle Gewinnphase abschliessen und den 15-Sekunden-Countdown zum Drop-Event bei `/warp Event` starten.

Freitags-Ablauf: Das Intro nutzt serverweite Sounds und Feuerwerk. Die automatische Ziehung vergibt einen zufaelligen Reward aus Coins, SkyKings-Sternen oder echten server-issued Crates. Danach kann Staff beliebig viele gehaltene Item-Stacks verlosen. Das Finale droppt kontrolliert hochwertige Crates/PvP-Gear/Consumables rund um den Warp `Event`.

Tournament und Juggernaut gehoeren bewusst nicht mehr zum finalen Feature-Set.

## 8. Crates & Voucher

- `/crate give <Spieler> <Typ> [Anzahl]` - Crates administrativ ausgeben.
- `/craterewards` - Crate-/Paid-Rank-Reward Center.
- `/gutscheine` - Admin-GUI zur Voucher-Erzeugung. Aliases: `/voucher`, `/vouchers`.

## 9. Staff / Administration

- `/rang <Spieler> <Rang>` - Rang setzen.
- `/rechte <Spieler> <Recht>` - freigegebenes Voucher-Recht vergeben.
- `/announcement <Nachricht>` - SkyKings-Ankuendigung.
- `/clearchat` - Chat leeren.
- `/clear` - angekuendigter Boden-Clear. Aliases: `/groundclear`, `/bodenclear`.
- `/gm <0|1|2|3> [Spieler]` - Gamemode.
- `/buildmode` - persoenlicher Map-Baumodus fuer geschuetzte Produktions-/Communitymaps.
- `/skcheck` - Runtime-Systemcheck.
- `/discordtest [staff|audit|events|status]` - Discord-Bridge testen.

## 10. Warps & Welten

Die offiziellen SkyKings-Welten sind `SkyPvP`, `SkyPlots`, `SkyIslands` und `SkyCommunityEvent`. `SkyEvents` ist nicht mehr Bestandteil des Servers.

- `/setwarp <Name>` - Warp an aktueller Position setzen.
- `/delwarp <Name>` - Warp loeschen. Alias: `/deletewarp`.
- `/maptp <main|plots|islands|community>` - Staff-Weltreise.
- `/setspawn` - globalen SkyKings Spawn setzen.
- `/skymap list` - zeigt alle offiziellen Maps und ihren Ladezustand.
- `/skymap load [Weltname]` - vorhandene Welt manuell laden; normalerweise nicht noetig, da offizielle Maps beim Start automatisch geladen werden.
- `/skymap community [Weltname]` - Community-/Giveaway-Map erstmalig erzeugen, falls sie noch fehlt.
- `/skymap generate [Weltname]` - alte lokale Test-Arena V3 erzeugen; kein Produktionsbestandteil.

**Freitags-Setup Pflicht:** Ein persistenter Warp mit exakt dem Namen `Event` muss gesetzt sein. An der Mitte der gewuenschten Drop-Area stehen und einmal `/setwarp Event` ausfuehren. `/freitag` startet nicht, solange dieser Warp fehlt oder seine Welt nicht geladen ist.

Auto-Load-Regel: `SkyPlots` und `SkyIslands` werden beim Core-Start mit ihren Spezialgeneratoren geladen. `SkyPvP` und `SkyCommunityEvent` werden beim Combat-Start automatisch geladen, sobald ihre Weltordner existieren.

## 11. Map-Gameplay Setup

- `/mapsetup` - zentraler Staff Map-Setup Hub.
- `/maploot <set|remove|refill>` - Map-Loot-Chests.
- `/supplydrop <addpoint|points|trigger>` - Supply Drops.
- `/kingaltar <set|remove|info> [Radius]` - King Altar / KOTH.
- `/hotzone <add|remove|list> [Name] [Radius]` - PvP-Hot-Zones.
- `/endzone <set|remove|info> [Radius]` - End-Zone.
- `/pvpregion <pos1|pos2|create|remove|list>` - Open-World-PvP-Cuboids.
- `/secret <add|remove|list> [Name] [Radius]` - Map-Secrets.
- `/secretroom <add|remove|list>` - Secret Loot Rooms.
- `/route <create|addpoint|remove|list> [Name]` - Jump-/Pearl-Routen.
- `/landmark <set|remove|list> [Typ] [Radius]` - Gold-/Level-/Blacksmith-/Merchant-Landmarks.
- `/trashbin <add|remove|list>` - Map-Muelleimer.
- `/mapdisplay <set|remove|list> [topkills|king|hotzones|ranks]` - Map-Hologramme/Displays; `ranks` setzt das Paid-Rank-Board fuer Knight bis King.

## 12. Event-Arenen einmalig setzen

Duel, LMS und Clan Wars benoetigen weiterhin persistente Arena-Punkte, aber keine eigene `SkyEvents`-Welt. Die Punkte koennen in einer geeigneten vorhandenen Welt gesetzt werden. Finale Koordinaten niemals raten; ingame an die exakte Position stellen.

### Duel

```text
/eventarena set duel a
/eventarena set duel b
/eventarena set duel spectator
```

### LMS

```text
/eventarena set lms lobby
/eventarena set lms spawn1
/eventarena set lms spawn2
/eventarena set lms spawn3
/eventarena set lms spawn4
/eventarena set lms spectator
```

### Clan Wars

```text
/eventarena set clanwar a1
/eventarena set clanwar a2
/eventarena set clanwar b1
/eventarena set clanwar b2
```

Fuer 3v3 bis 5v5 optional `a3..a5` und `b3..b5` setzen.

## 13. Shop-NPCs

In der Naehe des gewuenschten Villagers:

```text
/shopnpc bind system
/shopnpc bind pvp
/shopnpc bind blacksmith
/shopnpc bind enchant
/shopnpc bind recycler
/shopnpc bind merchant
/shopnpc bind jackpot
```

- `/shopnpc info` - Bindung des naechsten Villagers pruefen.
- `/shopnpc unbind` - Bindung entfernen.

## 14. Empfohlene erste Setup-Reihenfolge

1. Build/Deploy, Serverstart, `/skcheck`.
2. Mit `/skymap list` pruefen, dass `SkyPvP`, `SkyPlots`, `SkyIslands` und `SkyCommunityEvent` geladen sind.
3. `/setspawn`.
4. finale `/setwarp`-Punkte setzen; fuer den Community-Abend zwingend auch `/setwarp Event`.
5. PvP-Regionen definieren.
6. King Altar, Hot Zones, End Zone.
7. Gold-/Level-/Blacksmith-/Merchant-Landmarks.
8. Secrets, Loot Rooms, Routes, Map Loot, Supply Drops, Displays, Trashbins.
9. Shop-NPCs binden und Markt-Mietstaende mit `/shoprent pos1|pos2|create` definieren.
10. Duel/LMS/Clan-War-Arenen in einer gewuenschten bestehenden Welt setzen.
11. Plot-/Island-Schutz, erhoehten Rand, Merge, Trust testen.
12. Kits, 100-Level-Battle-Pass, Quests, Crates, Voucher, Jackpot testen.
13. `/freitag` einmal mit Auto-Ziehung, mindestens einer `/verlosen`-Runde und `/verlosen fertig` bis zum kompletten Drop-Finale testen.
14. Multiplayer: Duel, LMS, Clan Wars, Trade, PlayerShop und Market Rentals.
15. Restart-/Persistenz- und Backup/Restore-Gates.
16. Economy-/Reward-Balance, dann Soft Launch.

## 15. Pflege-Regel

- Bei jedem neuen/entfernten Command diese Datei aktualisieren.
- `plugin.yml` und `/commands` muessen dieselbe Spielerrealitaet widerspiegeln.
- PDF/Handbook wird aus diesem aktuellen Repo-Stand neu erzeugt, sobald sich Commands oder Setup aendern.
- Keine retired Features (Tournament/Juggernaut/SkyEvents) wieder in Help/UI/Doku aufnehmen.
