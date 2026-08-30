# SkyKings Plugin Strategy

## Grundsatz

Nicht alles selbst entwickeln. Externe Plugins werden verwendet, wenn sie robuste Infrastruktur liefern. Eigene Plugins übernehmen alle SkyKings-spezifischen Spielmechaniken, die eng miteinander verzahnt sind oder besondere GUIs/Logging/Balancing benötigen.

## Kandidaten für externe Plugins

### Infrastruktur
- LuckPerms — Gruppen und Permissions
- Vault — Bridge für Economy/Permissions
- ProtocolLib — Packet-/Legacy-Integration
- PlaceholderAPI — Platzhalter für Scoreboards/Hologramme/GUIs
- WorldEdit — Build-Workflow
- WorldGuard — Regions/Safezones/PvP-Grenzen

### Welten / Systeme
- PlotSquared Legacy — /plot
- Island-System — noch offen; muss 1.8.8 stabil unterstützen und API/Events liefern
- Villager-Shop-Lösung — noch offen; Shopkeeper-artig, möglichst 1.8.8 stabil
- Hologramm-Plugin — noch offen; nur falls wir Hologramme nicht selbst abstrahieren
- AntiCheat — noch offen; muss bewusst für 1.8 PvP ausgewählt werden

## Eigenentwicklung

### SkyKings-Core
- Rank Service
- Economy Service
- Kit Registry
- Cooldowns
- Player Profiles
- Config/GUI/Item Utilities
- Persistenz
- Logging

### SkyKings-Combat
- Starterkit
- Combat Tag
- Nethersterne
- Killstreaks
- Anti-Killfarm
- Lootschutz
- Newbie Protection
- Hot Zone
- King Zone
- Booster
- Loot Chests

### SkyKings-Crates
- Head Crates
- Preview/Open
- /craterewards
- Open-All
- Gutscheine
- Unique IDs
- Prefixe/Death Message Rewards

### SkyKings-Admin
- /gutscheine
- /verlosung
- Audit Logs
- Discord Log Bridge

## Auswahlkriterien für Legacy Plugins

Jedes externe Plugin muss vor Aufnahme geprüft werden auf:
1. echte 1.8.8-Kompatibilität
2. bekannte Abhängigkeiten
3. API-Verfügbarkeit
4. Lizenz
5. Stabilität / bekannte Exploits
6. Performance
7. letzte funktionierende Version
8. Konfigurationsmöglichkeiten
9. ob es unsere Economy oder Permissions überschreibt
10. Backup-/Migrationsfähigkeit

## Regel für Downloads

Keine zufälligen JARs in das Repository committen.

Unter `server/plugins/README.md` wird später für jedes Plugin dokumentiert:
- Pluginname
- exakte Version
- offizielle Downloadquelle
- SHA256 der verwendeten JAR
- Lizenz
- notwendige Abhängigkeiten
- Konfigurationshinweise

Binärdateien und Server-JARs bleiben grundsätzlich außerhalb von Git, außer eine Lizenz erlaubt dies ausdrücklich und es gibt einen klaren Grund.
