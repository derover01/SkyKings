# SkyKings Architektur

## Plattform

- Zielclient: Minecraft 1.8.9
- Serverbasis: 1.8.8-kompatibler Spigot/Paper-Legacy-Stack
- Java: wird nach Kompatibilitätsprüfung mit allen Legacy-Abhängigkeiten festgelegt
- Buildsystem für Eigenplugins: Maven
- Repository: Monorepo

## Modulgrenzen

### SkyKings-Core
Verantwortlich für:
- Player Profile
- Rangmodell und interne Rank-API
- Economy-API
- Kit-Registry und Cooldown-Basis
- zentrale Configs
- gemeinsame GUI-/Item-Utilities
- Datenpersistenz
- Events/API für andere SkyKings-Module

### SkyKings-Combat
Verantwortlich für:
- kein Fallschaden
- Starter-Kit nach jedem Tod
- Combat Tag
- 3-Sekunden-Enderpearl-Cooldown
- Kill-/Death-Verarbeitung
- Nethersterne pro Kill und Killstreak-Multiplikatoren
- Anti-Killfarm
- Lootschutz nach Kill
- Newbie Protection
- Hot Zones
- King Zone
- Loot-Chest-Events
- Booster-Druckplatten

### SkyKings-Crates
Verantwortlich für:
- Crates als Custom-Heads im Inventar
- Linksklick Preview / Rechtsklick Öffnen
- Open-All ab Exile
- /craterewards
- rankabhängige Claim-Cooldowns
- Rank-, Kit-, Permission- und Prefix-Gutscheine
- Unique Voucher IDs
- Anti-Dupe
- Reward- und Crate-Logs

### SkyKings-Admin
Verantwortlich für:
- /gutscheine GUI
- /verlosung
- Admin-/Economy-Audit
- Staff Utility Commands
- Discord Logging Bridge
- sensible Aktionen und Berechtigungsprüfungen

## Externe Plugins

Externe Plugins werden nur verwendet, wenn sie eine stabile Infrastrukturaufgabe besser lösen als Eigenentwicklung. Kandidaten:
- LuckPerms: Permissions und Gruppen
- Vault: Economy-/Permission-Bridge
- ProtocolLib: Packet-/Legacy-Integration
- WorldEdit / WorldGuard: Build und Regionen
- PlaceholderAPI: Platzhalter
- PlotSquared Legacy: /plot
- Island-System: wird separat evaluiert
- Shopkeeper/Villager-Shop-Lösung: wird separat evaluiert
- AntiCheat: wird separat evaluiert

## Datenhaltung

Ziel:
- lokale Entwicklung zunächst SQLite oder H2
- produktiv MySQL/MariaDB
- alle wertvollen Gutscheine besitzen serverseitige eindeutige IDs
- Economy-, Voucher-, Trade-, Staff- und Admin-Aktionen sind auditierbar

## Integrationen

- Discord Webhooks/Bot für Logs, Events und Serverstatus
- PlaceholderAPI für Scoreboard, Hologramme und Menüs
- LuckPerms für externe Permission Enforcement

## Entwicklungsregeln

1. Keine Geschäftslogik direkt in Commands oder Listenern.
2. Services besitzen klare APIs.
3. Config-Werte statt Hardcoding für Balanceparameter.
4. Kein Modul greift direkt auf interne Datenstrukturen eines anderen Moduls zu.
5. Kritische Economy- und Gutscheinaktionen müssen atomar und logbar sein.
6. Legacy-Kompatibilität hat Vorrang vor modernen Java-Spielereien.
7. Erst funktional testen, danach Polish und Animationen.
