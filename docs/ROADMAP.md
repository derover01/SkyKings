# SkyKings Roadmap

## Phase 0 — Fundament

Ziel: reproduzierbare lokale Entwicklungsumgebung und saubere Projektstruktur.

- Repository lokal klonen
- Maven-Monorepo anlegen
- Java-/Spigot-Kompatibilität prüfen
- Parent-POM definieren
- Module anlegen:
  - SkyKings-Core
  - SkyKings-Combat
  - SkyKings-Crates
  - SkyKings-Admin
- lokaler Testserver
- Config-Ordner
- Gitignore
- Build-Script
- README für lokale Entwicklung

## Phase 1 — Core

- Player Profile
- Rank Enum / Rank Service
- Economy Service
- Permission Bridge zu LuckPerms/Vault
- Kit Registry
- Cooldown Service
- Config Service
- GUI Utility
- Item Utility
- Persistenzschicht
- Logging Service

Definition of Done:
- Server startet ohne Fehler
- Player kann geladen/gespeichert werden
- Ränge können intern gesetzt/gelesen werden
- Economy kann atomar buchen
- Cooldowns funktionieren nach Neustart

## Phase 2 — Combat

- kein Fallschaden
- Starter-Kit nach Tod
- Combat Tag
- Fly-Deaktivierung beim Betreten PvP
- Pearl Cooldown
- Kill-/Death-Verarbeitung
- Nethersterne
- Killstreaks
- Anti-Killfarm
- Loot Protection
- Newbie Protection

## Phase 3 — Kits & Rank Progression

- /kit GUI
- alle Rang-Kits
- rangabhängige Cooldowns
- höhere Ränge können niedrigere Kits verwenden
- Free-Rank-Kauf mit Coins
- /ränge GUI
- Paid-Rank-Hologramm-Anbindung
- /fly, /stack, /blöcke, /repair gemäß Berechtigungen

## Phase 4 — Crates & Gutscheine

- Head-Crates als echte Items
- Preview/Open GUI
- Reward Tables
- Expected-Value-Konfiguration
- /craterewards
- Open-All ab Exile
- Rank-/Kit-/Permission-/Prefix-Voucher
- Unique IDs
- Anti-Dupe
- /gutscheine Admin-GUI
- Gutschein-Logs

## Phase 5 — Economy & Shops

- globales Economy-Balancing
- Systemshop
- Villager-PlayerShops
- Shop-Mietflächen
- Gebühren / Money Sinks
- /trade + Logs
- Jackpot
- Black Market / Traveling Merchant ohne Paid-Ränge

## Phase 6 — Map Gameplay

- finale SkyPvP Map integrieren
- Loot Chests
- Hot Zones
- King Zone
- Booster-Druckplatten
- Blacksmith / Restock Stations
- serverweite Ankündigungen und Hologramme

## Phase 7 — Islands, Plots & Spawner

- /is System integrieren
- /plot integrieren
- Spawner-/AMS-System
- Mob-/Spawner-Stacking
- ROI-Balancing
- Shop-/Spawner-Logs

## Phase 8 — Retention

- /dailyrewards
- Daily/Weekly Quests
- Prefixe
- Death Messages mit Mehrfachauswahl
- Collection Book
- Achievements
- Season-System
- Hall of Fame
- Battle Pass

## Phase 9 — Events & Community

- /verlosung
- Freitagsevent
- KOTH
- LMS
- Tournament
- Juggernaut
- Clan Wars
- Discord Integration
- Event-Hologramme

## Phase 10 — Release Hardening

- Permission Audit
- Economy Exploit Audit
- Dupe Tests
- Load Tests
- Backup/Restore Tests
- Staff Workflow Tests
- Discord Log Tests
- Map Exploit Tests
- PvP Balance Sessions
- Soft Launch
- Season 1 Release
