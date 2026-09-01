# SkyKings Roadmap Status

Stand: 2026-09-01

Diese Datei trennt **implementiert**, **runtime-testpflichtig** und **noch offen**. Eine Roadmap-Phase wird nicht nur wegen vorhandener Klassen als release-fertig markiert.

| Phase | Status | Aktueller Stand |
|---|---|---|
| 0 Fundament | IMPLEMENTIERT | Maven-Monorepo, Java 8, vier Module, lokaler Testserver, Build/Deploy-Workflow vorhanden. |
| 1 Core | IMPLEMENTIERT | Profile, Ranks, Economy, Permissions, Kits, Cooldowns, Config, GUI/UI, Persistenz und Logging vorhanden. |
| 2 Combat | IMPLEMENTIERT / TESTEN | CombatTag, Killflow, Stats, Streak, Anti-Farm, Loot/Newbie Protection, Bounty und Cosmetics vorhanden; Runtime-PvP-Balance bleibt Gate. |
| 3 Kits & Ranks | IMPLEMENTIERT / UI-PASS OFFEN | Kit-/Rank-Funktionalitaet vorhanden; Kit-GUI bekommt noch einen eigenen Premium-Visual-Pass. |
| 4 Crates & Voucher | IMPLEMENTIERT / SECURITY-GATE | Crates, Rewards, Voucher und Serialisierung vorhanden; Replay/HMAC-Hardening bleibt Release-Thema. |
| 5 Economy & Shops | WEITGEHEND IMPLEMENTIERT | Systemshops, PvP-Shop, PlayerShop, Trade, Merchant/Black Market etc.; finale Preise/Money-Sinks und einzelne Mietfeatures bleiben Balance-Arbeit. |
| 6 Map Gameplay | IMPLEMENTIERT / MAP-SETUP | Map-Systeme, Objectives, Loot, Zones, Secrets, Routes, Displays vorhanden; finale Produktionsmap muss ingame eingerichtet und exploit-getestet werden. |
| 7 Islands / Plots / Spawner | IMPLEMENTIERT / RUNTIME-GATE | Islands, Plot-System, SpawnerStack. Plot-Raster wurde auf exakte 65x65-Zellen + neutrale 7er Roads + Merge-Road-Removal umgebaut. Vollautomatisches AMS/Mob-Stacking bleibt offen. |
| 8 Retention | IMPLEMENTIERT / BALANCE-GATE | Daily, Seasons, Achievements, Collection, Hall/Medals sowie neuer Battle-Pass-Hub, Free/Premium Reward Track und Quest Center. Eigener Resource-Pack-Pixel-Layer ist optionaler visueller Ausbau. |
| 9 Events & Community | TEILWEISE IMPLEMENTIERT | Giveaway, Peace, Duel/Wager, LMS, King/KOTH, Most Wanted, Event-Arena/Maps vorhanden. Tournament-Controller, Juggernaut-Controller und Clan-Wars-Controller bleiben offen. |
| 10 Release Hardening | AKTIV | CI fuehrt jetzt echte Tests aus; Deploy blockiert laufenden Server; Plot-Reset erstellt Backup; kompletter Server-Backup-Helper und erweiterte Release-Gates vorhanden. Runtime-, Load-, Backup-Restore-, Balance- und Soft-Launch-Tests bleiben bewusst manuell. |

## Aktueller Fokus nach diesem Night-Build

1. frische `SkyPlots`-Welt mit dem neuen Raster testen
2. `/p rand` und `/p merge` inklusive Restart-Persistenz testen
3. Battle Pass / Quests mit zwei Spielern und Premium on/off testen
4. Drop->Command Inventory-Regression reproduzieren und verifizieren
5. Phase-10-Release-Checkliste durchlaufen
6. danach Kit-GUI Visual Pass und die noch fehlenden Phase-9-Controller priorisieren

## Definition von „fertig“

Ein System ist fuer SkyKings erst fertig, wenn:
- CI mit Tests gruen ist
- es auf Spigot 1.8.8 bootet
- Persistenz nach Restart stimmt
- kein einfacher Dupe/Exploit bekannt ist
- Navigation und Lore zum SkyKings UI-System passen
- der reale Multiplayer-Test erfolgreich war

Deshalb bleiben Phase 9/10 trotz viel implementiertem Code absichtlich mit offenen Runtime-Gates markiert.
