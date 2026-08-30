# Lokale Entwicklung — SkyKings (Phase 0)

Diese Anleitung deckt **ausschliesslich Phase 0** aus `docs/ROADMAP.md` ab:
reproduzierbare lokale Entwicklungsumgebung und saubere Projektstruktur. Es sind
noch keine Gameplay-Features implementiert (siehe `CLAUDE.md`).

## Voraussetzungen

- **Git**
- **JDK** — Phase 0 setzt in den POMs `1.8` als Compiler-Source/-Target an (siehe
  "Offene Punkte" unten — das ist bewusst der konservative Standardwert fuer den
  1.8.8-Legacy-Stack, keine eigenmaechtige moderne Wahl).
- **Apache Maven**

Bitte lokal pruefen, was tatsaechlich installiert ist:

```
java -version
where java
mvn -version
git --version
```

## Projektstruktur

```
SkyKings/
├── pom.xml                          # Parent-/Monorepo-POM
├── plugins/
│   ├── SkyKings-Core/                # Player-Profile, Rank-/Economy-API, Kits, Persistenz
│   ├── SkyKings-Combat/              # Combat Tag, Nethersterne, Killstreaks, Zones
│   ├── SkyKings-Crates/              # Head-Crates, Gutscheine
│   └── SkyKings-Admin/               # Admin-GUI, Verlosung, Audit, Discord-Bridge
├── server/
│   ├── plugins/                      # Laufzeit-Plugin-JARs (nicht committed)
│   ├── maps/                         # Welt-Daten fuer Testserver (nicht committed)
│   └── configs/                      # Server-/Plugin-Configs
├── tools/                            # z. B. BuildTools.jar (nicht committed)
└── docs/                             # Architektur, Gameplay, Roadmap, Plugin-Strategie
```

Jedes Modul unter `plugins/` ist aktuell ein minimales, noch feature-loses
Grundgeruest (eine `JavaPlugin`-Klasse mit `onEnable`/`onDisable` + `plugin.yml`),
damit es spaeter kompilierbar ist (siehe `CLAUDE.md`, Prioritaet: nur Phase 0).

## Spigot-1.8.8-Abhaengigkeit

Die Spigot-API ist nicht auf Maven Central verfuegbar. Zwei dokumentierte Wege
(siehe `tools/README.md`):

1. **Empfohlen (offiziell):** `BuildTools.jar` lokal ausfuehren — installiert
   `spigot-api` automatisch in dein lokales Maven-Repository (`~/.m2`).
2. Alternativ ist im Parent-POM das SpigotMC-Nexus-Repository
   (`https://hub.spigotmc.org/nexus/content/repositories/snapshots/`) als
   Repository hinterlegt. Ob das aus deinem Netzwerk erreichbar ist, muss lokal
   geprueft werden — in der Cloud-Sandbox, in der dieses Grundgeruest erstellt
   wurde, war sowohl Maven Central als auch dieses Nexus-Repository blockiert.

## Bauen

```
mvn clean install
```

Phase 0 verlangt laut Auftrag nur, dass jedes Modul **spaeter** baubar ist. Ohne
aufgeloeste Spigot-API (siehe oben) schlaegt die Abhaengigkeitsaufloesung fuer die
vier Plugin-Module erwartungsgemaess fehl, bis Schritt "Spigot-1.8.8-Abhaengigkeit"
erledigt ist. Die Struktur selbst (Parent-POM, Module, Vererbung) ist unabhaengig
davon korrekt aufgesetzt.

## Lokalen Testserver aufsetzen (Vorbereitung)

1. Spigot-1.8.8-Server-JAR per BuildTools erzeugen (s. o.) und z. B. nach
   `server/spigot-1.8.8.jar` legen (nicht committen).
2. `server/plugins/`, `server/maps/`, `server/configs/` existieren bereits als
   vorbereitete, leere Struktur (siehe jeweilige README.md dort).
3. Gebaute Plugin-JARs aus `plugins/*/target/*.jar` nach `server/plugins/` kopieren.
4. Server starten (Beispiel, Java-Version siehe "Offene Punkte"):
   ```
   java -jar server/spigot-1.8.8.jar --nogui
   ```

Ein automatisiertes Start-Skript ist in Phase 0 bewusst noch nicht enthalten
(kein Feature-Vorgriff) und kann in einer spaeteren Phase ergaenzt werden.

## Offene Punkte / Entscheidungen (nicht eigenmaechtig getroffen)

- **Finale Java-Version**: `1.8` ist der Startwert in den POMs, aber noch nicht
  gegen die tatsaechlich lokal installierten JDKs verifiziert (siehe CLAUDE.md
  Punkt 8+13 — keine voreilige Wahl einer modernen Version).
- **Spigot-1.8.8-Artefakte** muessen lokal per BuildTools erzeugt werden (siehe
  oben) — wurde in dieser Session nicht automatisiert ausgefuehrt/ersetzt, um
  nicht zu improvisieren, sondern sauber zu dokumentieren.
- **Lokaler Testserver-Start** ist nur als Ordnerstruktur vorbereitet, noch nicht
  automatisiert (kein Start-Skript in Phase 0).

## Branching (siehe CLAUDE.md)

- `main` bleibt stabil.
- Entwicklungsarbeit vorzugsweise ueber kurze, thematisch klar abgegrenzte
  Feature-Branches.
