# SkyKings Roadmap Status

Stand: 2026-09-05

Diese Datei trennt **implementiert**, **runtime-testpflichtig** und **noch offen**. Eine Roadmap-Phase wird nicht nur wegen vorhandener Klassen oder geplanter UI-/Pack-Slots als release-fertig markiert.

| Phase | Status | Aktueller Stand |
|---|---|---|
| 0 Fundament | IMPLEMENTIERT | Maven-Monorepo, Java 8, vier Module, lokaler Testserver, Build/Deploy-Workflow vorhanden. |
| 1 Core | IMPLEMENTIERT | Profile, Ranks, Economy, Permissions, Kits, Cooldowns, Config, GUI/UI, Persistenz, Clans, Clan-Tags und Logging vorhanden. |
| 2 Combat | IMPLEMENTIERT / TESTEN | CombatTag, Killflow, Stats, Streak, Anti-Farm, Loot/Newbie Protection, Bounty und Cosmetics vorhanden; Runtime-PvP-Balance bleibt Gate. |
| 3 Kits & Ranks | IMPLEMENTIERT / RUNTIME-GATE | Kit Arsenal mit sichtbaren Rangstufen, LOCKED/READY/COOLDOWN, Preview und direktem Claim; echter Ingame-Visual-Test bleibt offen. Rank-Up und Kit-Grant/Cooldown sind ueber das Core-Gameplay-Journal gegen unklare Restart-Zustaende abgesichert. |
| 4 Crates & Voucher | IMPLEMENTIERT / RUNTIME-SECURITY-GATE | Neue Crates sind pro Tier stackbar; neue v2-Voucher sind bei gleichem Typ + Ziel stackbar und werden ueber persistente serverseitige Issued-Claim-Limits gegen kopierte Zusatzclaims abgesichert. Redemption wird vor Reward-Vergabe persistent reserviert; Legacy-Serials bleiben kompatibel und fail-closed. Rapid-Click-/Restart-Regressionen sind automatisiert abgedeckt, echter Spigot-Dupe-Test bleibt Gate. |
| 5 Economy & Shops | IMPLEMENTIERT / RUNTIME-TRANSACTION-GATE | Systemshops, PvP-Shop, PlayerShop, Trade, Merchant/Black Market und geschuetzte Market-Rentals vorhanden. SystemShop und PlayerShop besitzen persistente Settlement-Journals. PlayerShop reserviert Stock vor Ausgabe, Pending Revenue vor Claim und blockiert `long`-Overflows bei Gebuehr, Revenue-Akkumulation, Recovery und Auszahlung fail-closed. Haendler-Eier sind ohne per-Item-UUID stackbar; Legacy-Eier bleiben erkennbar. Trade-Sessions sind atomar und jede Angebotsaenderung invalidiert beide Zusagen. Runtime-Race-/Balance-Test bleibt Gate. |
| 6 Map Gameplay | IMPLEMENTIERT / MAP-SETUP | Map-Systeme, Objectives, Loot, Zones, Secrets, Routes, Displays vorhanden; finale Produktionsmap muss ingame eingerichtet und exploit-getestet werden. Offizielle Welten sind SkyPvP, SkyPlots, SkyIslands und SkyCommunityEvent; `/skymap list` zeigt den Ladezustand. SkyEvents wurde retired. |
| 7 Islands / Plots / Spawner | IMPLEMENTIERT / RUNTIME-GATE | Islands, Plot-System, SpawnerStack und automatisches Mob-Stacking fuer Spawner-Mobs. Island-Starterloot ist spielerbezogen einmalig und restart-persistent; Delete->Create kann keinen neuen Starterloot farmen und Persistenzfehler werden fail-closed behandelt. Plot-Raster wurde auf exakte 65x65-Zellen + neutrale 7er Roads + Merge-Road-Removal umgebaut. Spawner-Stack-Add/Break wird ueber das Core-Gameplay-Journal abgesichert; `spawner-stacks.yml` wird atomar geschrieben. Runtime-/Performance-Test bleibt Gate. |
| 8 Retention | TEILWEISE IMPLEMENTIERT / OFFEN | Daily Rewards sind implementiert und als persistente Claim/Coins/Item-Transaktion ueber das Core-Gameplay-Journal gehaertet. **Nicht im aktuellen Codebestand implementiert** sind Quest Center, Battle Pass / Premium Pass, Seasons, Achievements, Collections und Wildpunkte. Dafuer existieren aktuell keine aktiven Quest-/BattlePass-/Season-Klassen oder Commands; reservierte Resource-Pack-Icons sind nur Presentation-Vorbereitung und kein Implementierungsnachweis. Diese Systeme bleiben Roadmap-TODO und werden bis zur echten Implementierung nicht als `/skcheck`- oder Runtime-Pflichtgate behandelt. |
| 9 Events & Community | IMPLEMENTIERT / MULTIPLAYER-GATE | Giveaway, Peace, Duel/Wager, LMS, King/KOTH, Most Wanted und Clan Wars sind die final vorgesehenen Eventsysteme. Tournament und Juggernaut wurden bewusst entfernt. Standalone- und Friday-Giveaway-Gewinner werden zentral auditiert; der natuerliche Friday-Abschluss kann die finale Firework-Serie trotz bereits beendeter Phase ausfuehren, waehrend Generation-Tokens alte Tasks weiterhin stoppen. Event-Kills sind von Open-World-Stats getrennt; Commands/Drop/Pickup werden in isolierten Events geschuetzt. Multiplayer-Test bleibt Gate. |
| 10 Release Hardening | CODE-GATE GESCHLOSSEN / RUNTIME-GATES AKTIV | CI fuehrt echte Tests + statischen Release-Audit aus; Tournament/Juggernaut koennen nicht versehentlich wieder als Commands/Services committed werden; Deploy blockiert laufenden Server; Preflight, One-Command-Testvorbereitung, automatischer Legacy-SkyEvents-Backup/Cleanup, Plot-Reset, Backup + Guarded Restore, Event-Isolation-, Voucher/Crate-Anti-Dupe-, Trade-, Lore-, PlayerShop- und Plot-Regressionstests sowie erweiterte `/skcheck`-Diagnose vorhanden. Rank-Up, Kit, EnderChest-Seitenkauf, Spawner-Stacking und Daily nutzen das persistente Core-Gameplay-Settlement-Journal; BuildBlock-No-Sell-Marker werden synchron/atomar gespeichert. Runtime-, Load-, Backup-Restore-, Balance- und Soft-Launch-Tests bleiben bewusst manuell. |
| 11 Resource Pack / Presentation | CORE ICONSET IMPLEMENTIERT / CLIENT-RUNTIME-GATE | Minecraft-1.8.9-`pack_format: 1`, reproduzierbarer Java-8-Build aus binaerem RGBA-Atlas, 19 echte Legacy-Itemtexturen + `pack.png`, CI-Artefakt, Join-Auslieferung, `/pack`, URL-Diagnose und GUI-Decorator sind implementiert. Navigation, Status, Coins/Stern, Kits, Crates, Jackpot, Shop, Trade, Clan, Duel und Event besitzen echte Pack-Slots; Battle-Pass-/Quest-Slots sind fuer die noch offenen Phase-8-Systeme reserviert. PvP-Kernitems bleiben geschuetzt. Offen sind echter 1.8.9-Clienttest, visuelle Feinkorrekturen falls dort noetig und finale Produktions-HTTPS-URL. |

## Aktueller Fokus ab Phase 10

Die automatisierbaren Hardening- und Resource-Pack-Punkte sind code-seitig weitgehend umgesetzt. Die verbleibenden Punkte sind echte Runtime-/Map-/Multiplayer-/Presentation-Gates. Phase 8 darf nicht mehr als fertig behandelt werden, solange Quest/BattlePass/Season/Achievement/Collection/Wildpunkte nicht im aktuellen Code existieren.

1. `docs/NEXT_RUNTIME_CHECKLIST.md` auf einem frischen 1.8.9-Testserver mit mindestens zwei Clients komplett durchlaufen
2. universelle Stackability ingame pruefen: Crates, identische v2-Voucher, Haendler-Eier, Friday-Rewards und normale Rewards; keine sichtbaren Serial-/UUID-/Debug-Reste
3. Island `/is create -> delete -> create` inklusive Restart und GUI-Weg testen; Starterloot darf nur einmal pro Spieler erscheinen
4. alle Voucher-Typen testen: Rank, Rankup, Kit, Permission, Prefix, Coins, GiveAll
5. Standalone-Giveaway und kompletten Friday-Flow inklusive Audit, Drop-Phase und finaler Firework-Serie natuerlich durchlaufen lassen
6. Casino mit Gewinn/Verlust, Rate-Limit, zu wenig Coins und schnellen Mehrfachklicks testen
7. Jackpot mit zwei Spielern, 5-%-Sink, Restart und bewusstem PENDING-Settlement/REVIEW_REQUIRED testen
8. PlayerShop mit zwei Spielern testen: echter Villager-Merchant, letzter Stock, 2x64-Zustellung, voller Inventory-Fall, Revenue-Doppelclaim, Restart-Fenster sowie Overflow-/Recovery-Guards
9. `/commands` und `/skcheck` ingame auf Casino, Jackpot, PlayerShop, `/pack`, Persistenzdateien und Welten pruefen
10. frische `SkyPlots`-Welt mit dem neuen Raster testen; Plot-Rand exakt eine Blockreihe auf dem aeussersten Plotblock, niemals auf der 7er Road
11. `/p rand` und `/p merge` inklusive Restart-Persistenz testen
12. `/skymap list` pruefen: SkyPvP, SkyPlots, SkyIslands, SkyCommunityEvent geladen; SkyEvents darf nicht aktiv sein
13. Core-Gameplay-Transaktionen einzeln mit Restart/Abbruch testen: Rank-Up, Kit-Claim/Cooldown, EnderChest-Seitenkauf, Spawner-Stack-Add/Break und Daily-Claim; `/skcheck` darf danach kein ungeklärtes Gameplay-Settlement zeigen
14. BuildBlock-No-Sell-Schutz ueber Platzieren, Abbauen und Restart testen; Gratis-Baubloecke duerfen nie als normale Economy-Bloecke verkaufbar werden
15. Kit Arsenal / Crate Center / Commands Hub visuell und funktional testen
16. Prefix + Rang-Anzeige + Clan-Tag in Chat/Tab testen
17. Drop->Command Inventory-Regression reproduzieren und verifizieren
18. Trade-Offer waehrend Countdown aendern, Quit/Close, sauberer Server-Stop und Coin-Aenderungen testen
19. Duel/Wager und LMS im echten Multiplayer testen
20. Clan Wars als 2v2, 3v3 und 5v5 inklusive Quit/Forfeit testen
21. Mob-Stacking mit grossen Spawner-Farmen auf Performance, Drops und Restart-Verhalten testen
22. Discord-Relay optional mit echtem Bot/Channel testen
23. Backup -> Daten veraendern -> Restore -> Persistenz vergleichen
24. Economy-/Reward-Balance unter realistischen Spielerzahlen testen
25. `SkyKings-ResourcePack-1.8.9.zip` auf echtem 1.8.9-Client testen: alle 19 Icons, `pack.png`, keine Missing Textures und GUIs Small/Normal
26. mit und ohne Pack testen; PlayerShop-Merchant muss Coin-Icon statt Stern zeigen und der virtuelle Token darf nicht entnehmbar sein
27. finale Pack-ZIP unter stabiler HTTPS-URL hosten, `resource-pack.enabled: true` setzen und Join-Auslieferung + `/pack` mit frischem Client testen
28. **Phase 8 offen:** Quest Center, Battle Pass/Premium Pass, Seasons, Achievements, Collections und Wildpunkte erst nach echter Implementierung mit eigenen Tests und `/skcheck`-Gates versehen; bis dahin nicht als bestanden markieren
29. danach Soft-Launch-Gate statt weitere nicht verifizierte Kernsysteme blind als fertig zu dokumentieren

## UI-Richtung ab jetzt

Der im Projekt dokumentierte **Custom Panel UI Standard** ist verbindlich. Implementierte und zukuenftige grosse Systeme folgen demselben Produktmuster:
- Hero/Header
- klare Tabs oder Rails
- grosse Content-Cards statt Item-Wand
- Free/Premium/Locked/Ready/Cooldown sofort erkennbar, wo fachlich vorhanden
- wenige starke Farben
- kurze automatisch umgebrochene Lore
- Footer-Navigation
- Resource-Pack-ready, aber ohne Pack vollstaendig bedienbar

Der SkyKings Resource Pack ist **kein Post-Launch-Vielleicht**, sondern Teil des Presentation-Gates vor dem groesseren Launch. Der Core-Art-Pass ist jetzt implementiert. Die Pack-Schicht bleibt bewusst UI-/Branding-spezifisch, damit Schwerter, Bow, Rod, Ruestung, Enderperlen und Golden Apples nicht durch SkyKings-Grafiken ersetzt werden. Wegen Minecraft 1.8.9 werden keine modernen `CustomModelData`- oder JSON-Font/Glyph-Systeme vorausgesetzt; die Icon-Strategie arbeitet mit 19 bewusst reservierten Vanilla-Materialien und klaren Fallback-Namen/Lore.

## Definition von „fertig“

Ein System ist fuer SkyKings erst fertig, wenn:
- CI mit Tests und statischem Release-Audit gruen ist
- es auf Spigot/Paper 1.8.x bootet
- Persistenz nach Restart stimmt
- kein einfacher Dupe/Exploit bekannt ist
- Navigation und Lore zum SkyKings UI-System passen
- der reale Multiplayer-Test erfolgreich war

Der Gesamtserver ist fuer den groesseren Launch erst fertig, wenn zusaetzlich:
- alle Systeme im festgelegten Launch-Scope tatsaechlich im aktuellen Code vorhanden sind; reine Roadmap-/Pack-Eintraege zaehlen nicht als Implementierung
- der Resource-Pack-Build alle 19 Pflichttexturen + `pack.png` erzeugt
- der Pack auf einem frischen 1.8.9-Client ohne Missing Textures laedt
- Join-Auslieferung und `/pack` mit der finalen HTTPS-URL funktionieren
- der Server ohne Pack weiterhin voll spielbar bleibt
- Backup/Restore, Load und Soft-Launch bestanden sind

Phase 9 ist auf das finale gewuenschte Eventset reduziert. Phase 10 hat code-seitig die wichtigsten automatisierbaren Gates erreicht; Phase 11 ist code-/asset-seitig bis auf echten Clienttest und Produktionshosting umgesetzt. Phase 8 bleibt bewusst teilweise offen, bis die dort aufgefuehrten Retention-Systeme wirklich im Code vorhanden und getestet sind.
