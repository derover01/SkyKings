# SkyKings Roadmap Status

Stand: 2026-09-01

Diese Datei trennt **implementiert**, **runtime-testpflichtig** und **noch offen**. Eine Roadmap-Phase wird nicht nur wegen vorhandener Klassen als release-fertig markiert.

| Phase | Status | Aktueller Stand |
|---|---|---|
| 0 Fundament | IMPLEMENTIERT | Maven-Monorepo, Java 8, vier Module, lokaler Testserver, Build/Deploy-Workflow vorhanden. |
| 1 Core | IMPLEMENTIERT | Profile, Ranks, Economy, Permissions, Kits, Cooldowns, Config, GUI/UI, Persistenz, Clans, Clan-Tags und Logging vorhanden. |
| 2 Combat | IMPLEMENTIERT / TESTEN | CombatTag, Killflow, Stats, Streak, Anti-Farm, Loot/Newbie Protection, Bounty und Cosmetics vorhanden; Runtime-PvP-Balance bleibt Gate. |
| 3 Kits & Ranks | IMPLEMENTIERT / RUNTIME-GATE | Kit Arsenal mit sichtbaren Rangstufen, LOCKED/READY/COOLDOWN, Preview und direktem Claim; echter Ingame-Visual-Test bleibt offen. |
| 4 Crates & Voucher | IMPLEMENTIERT / RUNTIME-SECURITY-GATE | Neue Crates/Voucher akzeptieren nur serverseitig registrierte Serials. Redemption wird vor Reward-Vergabe persistent reserviert; Rapid-Click- und Restart-Replay sind durch Regressionstests abgesichert. Erfolgreiche Crate-Opens feuern erst nach Redemption + Reward-Vergabe das modulneutrale Quest-Event. Echter Spigot-Dupe-Test bleibt Gate. |
| 5 Economy & Shops | IMPLEMENTIERT / RUNTIME-TRANSACTION-GATE | Systemshops, PvP-Shop, PlayerShop, Trade, Merchant/Black Market und geschuetzte Market-Rentals vorhanden. PlayerShop-Persistenz nutzt atomischen Dateiaustausch; Stock wird vor Ausgabe/Verkauf persistent reserviert, Revenue vor Auszahlung auf 0 persistiert. Trade-Sessions sind atomar und jede Angebotsaenderung invalidiert beide Zusagen. Runtime-Race-/Balance-Test bleibt Gate. |
| 6 Map Gameplay | IMPLEMENTIERT / MAP-SETUP | Map-Systeme, Objectives, Loot, Zones, Secrets, Routes, Displays vorhanden; finale Produktionsmap muss ingame eingerichtet und exploit-getestet werden. Offizielle Welten sind SkyPvP, SkyPlots, SkyIslands und SkyCommunityEvent; `/skymap list` zeigt den Ladezustand. SkyEvents wurde retired. |
| 7 Islands / Plots / Spawner | IMPLEMENTIERT / RUNTIME-GATE | Islands, Plot-System, SpawnerStack und automatisches Mob-Stacking fuer Spawner-Mobs. Plot-Raster wurde auf exakte 65x65-Zellen + neutrale 7er Roads + Merge-Road-Removal umgebaut. Runtime-/Performance-Test bleibt Gate. |
| 8 Retention | IMPLEMENTIERT / BALANCE-GATE | Daily, Seasons, Achievements, Collection, Hall/Medals sowie Custom-Panel Battle Pass mit 100 Free-Level-Rewards + 100 Premium-Level-Rewards und tab-basiertem Quest Center. Systemquests umfassen u. a. legitime Kills, Pearls, Streak, King Altar, Duel-Siege, Crate-Opens, Bounty-Claims und frische Rare/Epic Map Chests. `/premiumpass give|remove <Spieler>` verwaltet Premium. Eigener Resource-Pack-Pixel-Layer bleibt optionaler Ausbau nach Release-Hardening. |
| 9 Events & Community | IMPLEMENTIERT / MULTIPLAYER-GATE | Giveaway, Peace, Duel/Wager, LMS, King/KOTH, Most Wanted und Clan Wars sind die final vorgesehenen Eventsysteme. Tournament und Juggernaut wurden bewusst entfernt. Event-Kills sind von Open-World-Stats getrennt; Commands/Drop/Pickup werden in isolierten Events geschuetzt. Duel/LMS/Clan-War-Arenapunkte werden persistent in einer geeigneten bestehenden Welt gesetzt; eine separate SkyEvents-Welt gibt es nicht mehr. |
| 10 Release Hardening | CODE-GATE WEITGEHEND GESCHLOSSEN / RUNTIME-GATES AKTIV | CI fuehrt echte Tests + statischen Release-Audit aus; Tournament/Juggernaut koennen nicht versehentlich wieder als Commands/Services committed werden; Deploy blockiert laufenden Server; Preflight, One-Command-Testvorbereitung, automatischer Legacy-SkyEvents-Backup/Cleanup, Plot-Reset, Backup + Guarded Restore, Event-Isolation-, Voucher/Crate-Anti-Dupe-, Trade-, Lore- und Plot-Regressionstests sowie erweiterte `/skcheck`-Diagnose vorhanden. Discord-Bridge meldet Status, King-Altar-Captures und kuratierte Killstreak-Meilensteine. Runtime-, Load-, Backup-Restore-, Balance- und Soft-Launch-Tests bleiben bewusst manuell. |

## Aktueller Fokus ab Phase 10

Die verbleibenden Punkte sind absichtlich echte Runtime-/Map-/Multiplayer-Gates und keine weiteren blinden Feature-Baustellen:

1. frische `SkyPlots`-Welt mit dem neuen Raster testen; Plot-Rand exakt eine Blockreihe auf dem aeussersten Plotblock, niemals auf der 7er Road
2. `/p rand` und `/p merge` inklusive Restart-Persistenz testen
3. `/skymap list` pruefen: SkyPvP, SkyPlots, SkyIslands, SkyCommunityEvent geladen; SkyEvents darf nicht aktiv sein
4. Battle Pass Level 1-100, Free/Premium Claims und `/premiumpass give|remove` testen
5. Quest Center inklusive Duel-, Crate-, Bounty- und Rare/Epic-Map-Chest-Fortschritt testen
6. Kit Arsenal / Crate Center / Commands Hub visuell und funktional testen
7. Prefix + Rang-Anzeige + Clan-Tag in Chat/Tab testen
8. Drop->Command Inventory-Regression reproduzieren und verifizieren
9. Crate/Voucher Rapid-Click-, Restart- und Inventar-Move-Dupeversuche ingame testen
10. PlayerShop Kauf vs. Stock-Withdraw, Revenue-Doppelclaim und Restart-Fenster testen
11. Trade-Offer waehrend Countdown aendern, Quit/Close und Coin-Aenderungen testen
12. Duel/Wager und LMS im echten Multiplayer testen
13. Clan Wars als 2v2, 3v3 und 5v5 inklusive Quit/Forfeit testen
14. Mob-Stacking mit grossen Spawner-Farmen auf Performance, Drops und Restart-Verhalten testen
15. Discord-Relay optional mit echtem Bot/Channel testen
16. Backup -> Daten veraendern -> Restore -> Persistenz vergleichen
17. Economy-/Reward-Balance unter realistischen Spielerzahlen testen
18. danach Soft-Launch-Gate statt weitere Kernsysteme blind aufzubauen

## UI-Richtung ab jetzt

Der im Projekt dokumentierte **Custom Panel UI Standard** ist verbindlich. Battle Pass, Quests, Kits, Crates, Commands und zukuenftige grosse Systeme folgen demselben Produktmuster:
- Hero/Header
- klare Tabs oder Rails
- grosse Content-Cards statt Item-Wand
- Free/Premium/Locked/Ready/Cooldown sofort erkennbar
- wenige starke Farben
- kurze automatisch umgebrochene Lore
- Footer-Navigation
- Resource-Pack-ready, aber ohne Pack vollstaendig bedienbar

Der optionale Resource-Pack-Layer kommt **nach** dem stabilen Serverstand. Er soll moeglichst UI-/Branding-spezifisch bleiben, damit Spieler ihre normalen PvP-/Block-/Item-Textures weiterhin mit eigenen Packs kombinieren koennen.

## Definition von „fertig“

Ein System ist fuer SkyKings erst fertig, wenn:
- CI mit Tests und statischem Release-Audit gruen ist
- es auf Spigot 1.8.8 bootet
- Persistenz nach Restart stimmt
- kein einfacher Dupe/Exploit bekannt ist
- Navigation und Lore zum SkyKings UI-System passen
- der reale Multiplayer-Test erfolgreich war

Phase 9 ist auf das finale gewuenschte Eventset reduziert. Phase 10 hat code-seitig die wichtigsten automatisierbaren Gates erreicht; ab jetzt entscheidet der reale lokale Test, was noch korrigiert werden muss.
