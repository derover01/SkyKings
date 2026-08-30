# CLAUDE.md — SkyKings Working Agreement

Diese Datei definiert, wie Claude/Cowork in diesem Repository arbeiten soll.

## Projektziel

SkyKings ist ein Minecraft OP-SkyPvP Server für 1.8.9-Clients auf einem 1.8.8-kompatiblen Legacy-Serverstack. Das Gameplay und die Architektur sind in `docs/` spezifiziert.

## Arbeitsregeln

1. Vor jeder Änderung `README.md` und relevante Dateien unter `docs/` lesen.
2. Keine Features erfinden, die nicht dokumentiert oder ausdrücklich beauftragt wurden.
3. Balancewerte immer konfigurierbar machen, nicht hardcoden.
4. Keine Binärdateien/JARs ohne ausdrückliche Freigabe committen.
5. Keine Secrets, Tokens, Passwörter oder Webhook-URLs committen.
6. Änderungen klein, nachvollziehbar und modular halten.
7. Maven verwenden.
8. Legacy-Kompatibilität mit 1.8.8/Java-Version des Projekts respektieren.
9. Business-Logik nicht direkt in Listener oder Commands schreiben.
10. Kritische Economy-/Voucher-Aktionen müssen transaktional/logbar sein.
11. Vor größeren Refactorings zuerst den Ist-Zustand erklären.
12. Nach jeder Implementierung Build/Test ausführen und Ergebnis dokumentieren.
13. Wenn eine Dependency unsicher ist, zuerst prüfen und nicht blind die neueste Version verwenden.
14. Keine bestehenden Dokumentationsentscheidungen überschreiben, außer der Auftrag verlangt es.

## Branching

- `main` bleibt stabil.
- Entwicklungsarbeit vorzugsweise über kurze Feature-Branches.
- Ein Feature-Branch soll nur einen klaren Themenbereich enthalten.

## Priorität

Aktuell gilt ausschließlich **Phase 0** aus `docs/ROADMAP.md`, bis diese ausdrücklich abgeschlossen wurde.

Phase 0 bedeutet:
- Repo lokal klonen
- Maven-Monorepo/Parent-POM vorbereiten
- Modulordner anlegen
- Gitignore
- lokale Dev-Dokumentation
- Buildstruktur

Noch NICHT implementieren:
- Ränge
- Economy
- Combat
- Crates
- Gutscheine
- Islands
- Plots
- Shops
- Spawner
- Seasons

## Rückmeldung

Nach Abschluss eines Auftrags immer kompakt angeben:
- was geändert wurde
- welche Dateien betroffen sind
- ob Build/Tests erfolgreich waren
- offene Blocker oder Entscheidungen
