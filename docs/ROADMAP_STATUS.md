# SkyKings Roadmap Status

Stand: 2026-09-01

Diese Datei trennt **implementiert**, **runtime-testpflichtig** und **noch offen**. Eine Roadmap-Phase wird nicht nur wegen vorhandener Klassen als release-fertig markiert.

| Phase | Status | Aktueller Stand |
|---|---|---|
| 0 Fundament | IMPLEMENTIERT | Maven-Monorepo, Java 8, vier Module, lokaler Testserver, Build/Deploy-Workflow vorhanden. |
| 1 Core | IMPLEMENTIERT | Profile, Ranks, Economy, Permissions, Kits, Cooldowns, Config, GUI/UI, Persistenz, Clans und Logging vorhanden. |
| 2 Combat | IMPLEMENTIERT / TESTEN | CombatTag, Killflow, Stats, Streak, Anti-Farm, Loot/Newbie Protection, Bounty und Cosmetics vorhanden; Runtime-PvP-Balance bleibt Gate. |
| 3 Kits & Ranks | IMPLEMENTIERT / RUNTIME-GATE | Kit Arsenal mit sichtbaren Rangstufen, LOCKED/READY/COOLDOWN, Preview und direktem Claim; echter Ingame-Visual-Test bleibt offen. |
| 4 Crates & Voucher | IMPLEMENTIERT / RUNTIME-SECURITY-GATE | Neue Crates/Voucher akzeptieren nur serverseitig registrierte Serials. Redemption wird vor Reward-Vergabe persistent reserviert; Rapid-Click- und Restart-Replay sind durch Regressionstests abgesichert. HMAC ist bei der autoritativen Issued-Registry nicht zusaetzlich erforderlich. Echter Spigot-Dupe-Test bleibt Gate. |
| 5 Economy & Shops | IMPLEMENTIERT / RUNTIME-TRANSACTION-GATE | Systemshops, PvP-Shop, PlayerShop, Trade, Merchant/Black Market etc. PlayerShop-Persistenz nutzt atomischen Dateiaustausch; Stock wird vor Ausgabe/Verkauf persistent reserviert, Revenue vor Auszahlung auf 0 persistiert. Trade-Sessions sind atomar und jede Angebotsaenderung invalidiert beide Zusagen. Runtime-Race-/Balance-Test bleibt Gate. |
| 6 Map Gameplay | IMPLEMENTIERT / MAP-SETUP | Map-Systeme, Objectives, Loot, Zones, Secrets, Routes, Displays vorhanden; finale Produktionsmap muss ingame eingerichtet und exploit-getestet werden. |
| 7 Islands / Plots / Spawner | IMPLEMENTIERT / RUNTIME-GATE | Islands, Plot-System, SpawnerStack und automatisches Mob-Stacking fuer Spawner-Mobs. Plot-Raster wurde auf exakte 65x65-Zellen + neutrale 7er Roads + Merge-Road-Removal umgebaut. Runtime-/Performance-Test bleibt Gate. |
| 8 Retention | IMPLEMENTIERT / BALANCE-GATE | Daily, Seasons, Achievements, Collection, Hall/Medals sowie Custom-Panel Battle Pass, Free/Premium Reward Track und tab-basiertes Quest Center. Eigener Resource-Pack-Pixel-Layer bleibt optionaler Ausbau nach Release-Hardening. |
| 9 Events & Community | IMPLEMENTIERT / MULTIPLAYER-GATE | Giveaway, Peace, Duel/Wager, LMS, King/KOTH, Most Wanted und Clan Wars sind die final vorgesehenen Eventsysteme. Tournament und Juggernaut wurden bewusst entfernt. Event-Kills sind von Open-World-Stats getrennt; Commands/Drop/Pickup werden in isolierten Events geschützt. Jetzt echte Multiplayer-Runtime-Tests. |
| 10 Release Hardening | AKTIV | CI fuehrt echte Tests aus; Deploy blockiert laufenden Server; Preflight, Plot-Reset, Backup + Guarded Restore, Event-Isolation-, Voucher/Crate-Anti-Dupe-, Trade-, Lore- und Plot-Regressionstests sowie erweiterte Release-Gates vorhanden. Runtime-, Load-, Backup-Restore-, Balance- und Soft-Launch-Tests bleiben bewusst manuell. |

## Aktueller Fokus ab Phase 10

1. frische `SkyPlots`-Welt mit dem neuen Raster testen
2. `/p rand` und `/p merge` inklusive Restart-Persistenz testen
3. Battle Pass / Quest Center / Kit Arsenal / Crate Center / Commands Hub visuell und funktional testen
4. Drop->Command Inventory-Regression reproduzieren und verifizieren
5. Crate/Voucher Rapid-Click-, Restart- und Inventar-Move-Dupeversuche ingame testen
6. PlayerShop Kauf vs. Stock-Withdraw, Revenue-Doppelclaim und Restart-Fenster testen
7. Trade-Offer waehrend Countdown aendern, Quit/Close und Coin-Aenderungen testen
8. Duel/Wager und LMS im echten Multiplayer testen
9. Clan Wars als 2v2, 3v3 und 5v5 inklusive Quit/Forfeit testen
10. Mob-Stacking mit grossen Spawner-Farmen auf Performance, Drops und Restart-Verhalten testen
11. Backup -> Daten veraendern -> Restore -> Persistenz vergleichen
12. Economy-/Reward-Balance unter realistischen Spielerzahlen testen
13. danach Soft-Launch-Gate statt weitere Kernsysteme blind aufzubauen

## UI-Richtung ab jetzt

Der im Projekt dokumentierte **Custom Panel UI Standard** ist verbindlich. Battle Pass, Quests, Kits, Crates, Commands und zukuenftige grosse Systeme folgen demselben Produktmuster:
- Hero/Header
- klare Tabs oder Rails
- grosse Content-Cards statt Item-Wand
- Free/Premium/Locked/Ready/Cooldown sofort erkennbar
- wenige starke Farben
- kurze automatisch umgebrochene Lore
- Footer-Navigation
- Resource-Pack-ready, aber ohne Pack vollständig bedienbar

## Definition von „fertig“

Ein System ist fuer SkyKings erst fertig, wenn:
- CI mit Tests gruen ist
- es auf Spigot 1.8.8 bootet
- Persistenz nach Restart stimmt
- kein einfacher Dupe/Exploit bekannt ist
- Navigation und Lore zum SkyKings UI-System passen
- der reale Multiplayer-Test erfolgreich war

Phase 9 ist code-seitig auf die final gewuenschten Eventsysteme reduziert und bleibt bis zum Multiplayer-Test im Runtime-Gate. Phase 10 ist der aktive Fahrplan.
