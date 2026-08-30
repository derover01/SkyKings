# server/plugins/

Laufzeit-Ordner fuer Plugin-JARs des lokalen Testservers.

**Es werden keine JARs in dieses Verzeichnis committet** (siehe `.gitignore` und
`CLAUDE.md` Punkt 4) — weder eigene Build-Artefakte (`plugins/*/target/*.jar`) noch
extern bezogene Plugins.

## Download-Protokoll fuer externe Plugins

Gemaess `docs/PLUGIN_STRATEGY.md`: *"Keine zufaelligen JARs in das Repository
committen"*. Fuer jedes extern bezogene Plugin (Kandidaten siehe
`docs/ARCHITECTURE.md` / `docs/PLUGIN_STRATEGY.md`: LuckPerms, Vault, ProtocolLib,
PlaceholderAPI, WorldEdit, WorldGuard, PlotSquared Legacy, ggf. Island-/Shop-/
AntiCheat-Loesungen) hier dokumentieren, sobald es eingebunden wird:

- **Name:**
- **Version:**
- **Quelle (URL):**
- **SHA256:**
- **Lizenz:**
- **Abhaengigkeiten:**
- **Konfigurationshinweise:**

## Phase 0

Es sind noch keine externen Plugins eingebunden. Die eigenen SkyKings-Module werden
lokal per `mvn clean package` gebaut und ihre JARs manuell (oder spaeter per
Build-Skript) in diesen Ordner kopiert, um sie auf dem lokalen Testserver zu laden.
