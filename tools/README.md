# tools/

Lokale Entwickler-Tools fuer den 1.8.8-Legacy-Serverstack. Inhalte dieses Ordners
werden **nicht committet** (siehe `.gitignore`) — hier landen nur lokal heruntergeladene
Binaries.

## BuildTools.jar (Spigot) — offizieller Weg zur 1.8.8-Abhaengigkeit

Die Spigot-API-/Server-JARs fuer 1.8.8 sind **nicht** auf Maven Central verfuegbar.
Der offizielle, von SpigotMC dokumentierte Weg ist `BuildTools.jar`:

1. `BuildTools.jar` von `https://hub.spigotmc.org/jenkins/job/BuildTools/` in diesen
   Ordner herunterladen (nicht committen).
2. Mit einer fuer die Ziel-Revision kompatiblen JDK ausfuehren, z. B.:
   ```
   java -jar BuildTools.jar --rev 1.8.8
   ```
3. BuildTools erzeugt lokal `spigot-api-1.8.8-R0.1-SNAPSHOT.jar` (und die volle
   Server-JAR) und installiert sie automatisch in das lokale Maven-Repository
   (`~/.m2`), sodass die Module unter `plugins/` dagegen bauen koennen.

## Offen dokumentiertes Problem (siehe Auftrag: "nicht improvisieren")

In der Cloud-Sandbox, in der dieses Phase-0-Grundgeruest erstellt wurde, ist der
Netzwerkzugriff sowohl auf Maven Central (`repo.maven.apache.org`) als auch auf
`hub.spigotmc.org` durch die Egress-Policy der Sandbox blockiert (HTTP 403 bereits
auf Proxy-Ebene, vor jeder Anwendungslogik). Ein echter `mvn`-Build inklusive
Abhaengigkeitsaufloesung konnte dort deshalb **nicht** verifiziert werden — auch
nicht fuer Standard-Maven-Plugins ohne jeden Spigot-Bezug.

Es wurde bewusst **keine** Ersatzloesung improvisiert (kein Fake-Jar, kein
Drittanbieter-Mirror). Schritt 1+2 oben muessen auf einem Rechner mit normalem
Internetzugang (z. B. deinem lokalen Windows-Rechner) ausgefuehrt werden; erst
danach ist ein vollstaendiger `mvn clean install` moeglich.
