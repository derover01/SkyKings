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
- physische Nethersterne als PvP-Währung
- persistente PvP-Stats
- Killstreaks / Beststreak
- Anti-Killfarm
- Loot Protection
- Newbie Protection
- Bounty-System mit Coins und Nethersternen
- Streak-Boni und Streak-Broadcasts
- besondere Kill-/Streak-Death-Messages
- Kill Sounds / Kill Effects als Cosmetics
- /top Leaderboards

## Phase 3 — Kits & Rank Progression

- /kit GUI
- alle Rang-Kits
- rangabhängige Cooldowns
- höhere Gameplay-Ränge können niedrigere Kits verwenden
- Teamränge geben keine Gameplay-Kit-Rechte
- Kit-Gutscheine ohne Cooldown
- Free-Rank-Kauf mit Coins
- /ränge GUI
- Paid-Rank-Hologramm-Anbindung
- /fly, /stack, /blöcke, /repair gemäß Berechtigungen
- /ec ab Gold
- /trash und /müll als temporäres 6x9-Müllinventar

## Phase 4 — Crates & Gutscheine

- Head-Crates als echte Items
- Preview/Open GUI
- Animation oder Sofortöffnung auswählbar
- stackbare Crates bis 64
- Reward Tables
- Expected-Value-Konfiguration
- /craterewards
- Open-All ab Exile/Recht
- Rank-/Kit-/Permission-/Prefix-Voucher
- Feature-Rechte als Gutscheine
- Unique IDs
- Anti-Dupe
- /gutscheine Admin-GUI
- Gutschein-Logs

## Phase 5 — Economy & Shops

- globales Economy-Balancing
- Coins als normale Server-/Economy-Währung
- physische Nethersterne als PvP-Shop-Währung
- Systemshop
- Netherstern-Villager-Shops für PvP-/Restock-Items
- Villager-PlayerShops
- Shop-Mietflächen
- Gebühren / Money Sinks
- /trade + sichere GUI + Logs
- Jackpot
- Black Market / Traveling Merchant ohne Paid-Ränge
- Blacksmith-/Repair-Shop
- Enchant-Shop mit XP/Lapis/Coins/Nethersternen
- Bottle Recycler

## Phase 6 — Map Gameplay

- finale SkyPvP Map integrieren
- Free Signs über die gesamte Map verteilen
- thematische Inseln mit echtem Gameplay-Zweck
  - End Island: Enderperlen / End-Loot
  - Blacksmith Island: Repair / Amboss
  - Level Island: XP / Lapis / Enchanting
  - Merchant Island: PvP-/Netherstern-Shops
  - Gold Island: Golden Apples / OP-Gap-Restock
  - King Zone: höchstes Risiko / bester Loot
  - Secret Islands: Parkour/Pearl-only/Secrets
- respawnende Loot Chests (Common/Rare/Epic/Legendary)
- Supply Drops auf zufälligen Inseln
- Hot Zones
- King Zone
- King Altar / KOTH-artiges Objective
- Booster-/Jump-Pads
- Secret Rooms, Fake Walls, Pearl-only Rooms, versteckte Leitern und risky Shortcuts
- Easter Eggs
- serverweite Ankündigungen und Hologramme
- physische Leaderboard-Ecke am Spawn
- Mülltonnen/Kessel zusätzlich zu /trash

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
- Achievements mit echten PvP-Zielen
  - First Blood
  - Unstoppable (10er Streak)
  - Sky Legend (50er Streak)
  - Untouchable
  - King Slayer
  - The Hunter
  - Rich Fighter
  - Clutch
- Season-System
- Hall of Fame
- PvP-Level 1–100, Prestige erst später
- Island Mastery (Kills/Zeit/Aktivitäten pro Insel)

### Battle Pass

SkyKings erhält einen PvP-/Map-fokussierten Battle Pass mit Free Track und Premium Track.
Der Premium Track soll kein hartes Pay-to-Win erzeugen und deshalb primär Cosmetics,
Crates, Komfort-Rechte und kleine Zusatzbelohnungen enthalten.

Grundstruktur:
- Season-basierter Battle Pass
- Free Track + Premium Track
- 50–100 Season-Level
- Season-XP durch passende SkyPvP-Aufgaben
- keine generischen Grind-Aufgaben wie „500 Blöcke abbauen“
- Fortschritt persistent
- übersichtliches /battlepass GUI
- Claim-Status pro Level persistent
- Season-Reset ohne vollständigen Serverreset

Beispiel-Aufgaben:
- Töte 10 Spieler
- Erreiche eine 5er Killstreak
- Sammle 25 Nethersterne
- Töte einen Spieler mit mindestens 10er Streak
- Öffne 3 Crates
- Gewinne ein Duel
- Erobere den King Altar
- Töte 3 Spieler auf der End Island
- Sammle eine Bounty ein
- Öffne eine Rare Map Chest
- Nutze 20 Enderperlen
- Gewinne ein Server-Event

Reward-Mix:
- Coins
- Crates
- Prefixe
- Kill Effects
- Cosmetics
- Komfort-/Feature-Gutscheine
- Season-exklusive Cosmetics/Titel
- keine garantierten starken Paid-Rank-Upgrades im Premium Track

## Phase 9 — Events & Community

- /verlosung
- Freitagsevent
- KOTH
- LMS
- Tournament
- Juggernaut
- Clan Wars
- /duel mit Kit-Auswahl
- optionale Duel-Wagers mit Coins/Nethersternen
- kleines Friend-/Peace-System als moderne Variante alter SkyPvP-/friede-Systeme
- Discord Integration
- Event-Hologramme
- Supply-Drop-Events

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
