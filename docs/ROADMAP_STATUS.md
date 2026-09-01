# SkyKings Roadmap Status

Stand: 2026-09-01

Diese Datei trennt **implementiert**, **runtime-testpflichtig** und **noch offen**. Eine Roadmap-Phase wird nicht nur wegen vorhandener Klassen als release-fertig markiert.

| Phase | Status | Aktueller Stand |
|---|---|---|
| 0 Fundament | IMPLEMENTIERT | Maven-Monorepo, Java 8, vier Module, lokaler Testserver, Build/Deploy-Workflow vorhanden. |
| 1 Core | IMPLEMENTIERT | Profile, Ranks, Economy, Permissions, Kits, Cooldowns, Config, GUI/UI, Persistenz, Clans und Logging vorhanden. |
| 2 Combat | IMPLEMENTIERT / TESTEN | CombatTag, Killflow, Stats, Streak, Anti-Farm, Loot/Newbie Protection, Bounty und Cosmetics vorhanden; Runtime-PvP-Balance bleibt Gate. |
| 3 Kits & Ranks | IMPLEMENTIERT / RUNTIME-GATE | Kit Arsenal mit sichtbaren Rangstufen, LOCKED/READY/COOLDOWN, Preview und direktem Claim; echter Ingame-Visual-Test bleibt offen. |
| 4 Crates & Voucher | IMPLEMENTIERT / SECURITY-GATE | Crates, Rewards, Voucher und Serialisierung vorhanden; Replay/HMAC-Hardening bleibt Release-Thema. |
| 5 Economy & Shops | WEITGEHEND IMPLEMENTIERT | Systemshops, PvP-Shop, PlayerShop, Trade, Merchant/Black Market etc.; finale Preise/Money-Sinks und einzelne Mietfeatures bleiben Balance-Arbeit. |
| 6 Map Gameplay | IMPLEMENTIERT / MAP-SETUP | Map-Systeme, Objectives, Loot, Zones, Secrets, Routes, Displays vorhanden; finale Produktionsmap muss ingame eingerichtet und exploit-getestet werden. |
| 7 Islands / Plots / Spawner | IMPLEMENTIERT / RUNTIME-GATE | Islands, Plot-System, SpawnerStack. Plot-Raster wurde auf exakte 65x65-Zellen + neutrale 7er Roads + Merge-Road-Removal umgebaut. Vollautomatisches AMS/Mob-Stacking bleibt offen. |
| 8 Retention | IMPLEMENTIERT / BALANCE-GATE | Daily, Seasons, Achievements, Collection, Hall/Medals sowie Custom-Panel Battle Pass, Free/Premium Reward Track und tab-basiertes Quest Center. Eigener Resource-Pack-Pixel-Layer bleibt optionaler Ausbau. |
| 9 Events & Community | IMPLEMENTIERT / MULTIPLAYER-GATE | Giveaway, Peace, Duel/Wager, LMS, King/KOTH, Most Wanted, Tournament, Juggernaut und Clan Wars sind als Controller vorhanden. Event-Kills sind von Open-World-Stats getrennt; Commands/Drop/Pickup werden in isolierten Events geschützt. Jetzt echte Multiplayer-Runtime-Tests. |
| 10 Release Hardening | AKTIV | CI fuehrt echte Tests aus; Deploy blockiert laufenden Server; Plot-Reset erstellt Backup; kompletter Server-Backup-Helper, Lore-/Plot-Regressionstests und erweiterte Release-Gates vorhanden. Runtime-, Load-, Backup-Restore-, Balance- und Soft-Launch-Tests bleiben bewusst manuell. |

## Aktueller Fokus ab Phase 10

1. frische `SkyPlots`-Welt mit dem neuen Raster testen
2. `/p rand` und `/p merge` inklusive Restart-Persistenz testen
3. Battle Pass / Quest Center / Kit Arsenal visuell und funktional testen
4. Drop->Command Inventory-Regression reproduzieren und verifizieren
5. Tournament mit 4/5/8 Spielern testen
6. Juggernaut mit 3+ Spielern, Boss-Quit und Team-Damage testen
7. Clan Wars als 2v2, 3v3 und 5v5 inklusive Quit/Forfeit testen
8. Backup -> Daten veraendern -> Restore -> Persistenz vergleichen
9. Economy-/Reward-Balance unter realistischen Spielerzahlen testen
10. danach Soft-Launch-Gate statt weitere Kernsysteme blind aufzubauen

## UI-Richtung ab jetzt

Der im Projekt dokumentierte **Custom Panel UI Standard** ist verbindlich. Battle Pass, Quests, Kits und zukuenftige grosse Systeme folgen demselben Produktmuster:
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

Phase 9 ist damit code-seitig geschlossen, bleibt aber bis zum Multiplayer-Test im Runtime-Gate. Phase 10 ist ab jetzt der aktive Fahrplan.
