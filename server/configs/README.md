# server/configs/

Konfigurationsdateien fuer den lokalen Testserver, z. B. `server.properties`,
`bukkit.yml`, `spigot.yml`/`paper.yml` sowie Configs der SkyKings-Plugins.

## Regeln (siehe CLAUDE.md)

- **Keine Secrets, Tokens, Passwoerter oder Webhook-URLs committen** (Punkt 5) —
  z. B. Discord-Webhook-URLs oder Datenbank-Zugangsdaten gehoeren in lokale,
  nicht committete Dateien (siehe `.gitignore`).
- **Balancewerte immer konfigurierbar machen, nicht hardcoden** (Punkt 3) — alle
  spaeteren Gameplay-Parameter (Cooldowns, Preise, Drop-Chancen etc.) gehoeren in
  diese Configs statt in Plugin-Code.

## Phase 0

Es existieren noch keine konkreten Server- oder Plugin-Configs. Dieser Ordner dient
als vorbereitete Ablage fuer Phase 1 und spaeter.
