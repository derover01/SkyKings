# SkyKings Gameplay Specification

## Identität

SkyKings ist OP-SkyPvP mit 1.8.9-Combat-Gefühl. SkyPvP bleibt immer Hauptmodus. Islands, Plots, Shops und Spawner unterstützen die Economy und Community, ersetzen aber nicht den PvP-Fokus.

## Combat

- kein Fallschaden
- OP-Goldäpfel stackweise als wichtige PvP-Ressource
- Stärke II und Schnelligkeit II gehören in jedes Rank-Kit
- Enderperlen mit ca. 3 Sekunden Cooldown
- kein Auto-GApple
- kein automatischer Armor-Wechsel
- Kämpfe sollen durch verpasste OP-GApple-Timings oder brechende Rüstung entschieden werden
- Armor-Durability ist zentral
- Combat Tag verhindert Teleports, Fly und Logout-Abuse
- nach einem Kill kurze Loot-Priorität für den Killer

## Starter-Kit nach jedem Tod

Bei jedem Tod erhält der Spieler automatisch ein kostenloses Starter-Kit:
- Full Iron
- normales Basisschwert
- normale Goldäpfel
- keine OP-Goldäpfel

Dieses Starter-Kit ist strikt vom Rang-Kit `/kit Spieler` getrennt.

## Rang-Kits

Ränge:

Free:
- Spieler
- Iron
- Gold
- Epic
- Diamond

Paid:
- Knight
- Phoenix
- Eternal
- Exile
- Endling
- King

Regeln:
- höhere Ränge können die Kits niedrigerer Ränge nutzen
- nicht jeder Rang muss höhere Sharpness/Protection erhalten
- Upgrades können stattdessen aus mehr OP-Goldäpfeln, Enderperlen, Stärke-II-/Speed-II-Tränken oder zusätzlicher Utility bestehen
- Unbreaking ist ein zentraler Balancewert
- Kit-Cooldowns müssen bewusst lang genug sein, damit Loot, Shops und Economy relevant bleiben

## Paid-Komfortrechte

- `/fly` ab Knight, aber nur Spawn/Safezones/Island/Plot und ausdrücklich nicht in der PvP-Map
- `/stack` ab Knight; stackt stapelbare Items im Inventar
- `/blöcke` ab Phoenix; GUI für unbegrenzt entnehmbare Baublöcke, die nicht verkauft werden können
- Crate Open-All ab Exile
- `/repair` als höherwertiges Recht; repariert alle reparierbaren Items im Inventar

## Crates

- keine Keys
- Crates sind angepasste Köpfe als Inventaritems
- überall nutzbar
- GUI: links Preview, rechts Öffnen
- Open-All ab Exile
- Quellen: Votes, Events, Quests, Playtime, Loot, Daily Rewards, Battle Pass, Season Rewards
- enthalten können: Money, Items, Spawner, Prefixe, Rank-Voucher, Kit-Voucher, Permission-Voucher
- Paid-Rank-Voucher nur extrem selten in sehr hochwertigen Crates

## /craterewards

Paid-Ränge können rangspezifische Crates claimen. Höhere Ränge besitzen auch Zugriff auf niedrigere Rewards. Je stärker die Crate, desto länger der Cooldown.

Startwerte:
- Knight: ca. 2h
- Phoenix: ca. 4h
- Eternal: ca. 8h
- Exile: ca. 12h
- Endling: ca. 18h
- King: ca. 24h

Finale Werte werden gegen den Expected Value der jeweiligen Crate gebalanced.

## Nethersterne

Nethersterne sind die PvP-Währung.
- Basis: 1 Netherstern pro gültigem Kill
- Killstreaks erhöhen den Ertrag
- Meilensteine wie 5, 10, 20, 50 Kills geben Zusatzsterne
- Anti-Killfarm: gleicher Gegner mehrfach hintereinander liefert bis Kill 5 normal, Kill 6 nur 50 %, ab Kill 7 keinen Reward mehr, bis das Anti-Farm-Fenster zurückgesetzt ist

## King Zone

- zentrale PvP-Zone
- wird nur aktiv, wenn mindestens 5 echte aktive Spieler gleichzeitig auf der Island sind
- unter 5 Spielern pausiert die Eroberung
- Alt-/AFK-Abuse muss verhindert werden

## Hot Zones

Rotierende PvP-Inseln mit temporären Boni:
- mehr Kill-Money
- bessere Loot-Chancen
- mehr Season XP

Ziel: Spieler aktiv über die Map bewegen.

## Loot Chests

- über die PvP-Map verteilt
- verschiedene Raritäten und Respawnzeiten
- hochwertige Chests werden serverweit angekündigt
- Spieler sollen bewusst um wertvolle Chest-Spawns kämpfen

## Booster

Druckplatten als Movement-System:
- horizontaler Boost nach vorne
- vertikaler Boost nach oben

Sie bilden feste Movement-Routen zwischen Inseln.

## Islands und Plots

### /is
- private Base
- Storage
- Spawner/Farmen
- eigener Shop
- Trophy Room
- Community-/Clan-Nutzung

### /plot
- Bauen
- Community
- kreative Projekte
- Nutzung von `/blöcke`

## Player Shops

- eigene Shop-Zone am Spawn
- Spieler mieten Shopflächen gegen Coins
- dort können Villager-Shops im Stil von Shopkeeper platziert werden
- verschiedene Shopgrößen und Mietpreise möglich
- Shopmiete dient als Money Sink

## Spawner / AMS

- Tier-System für Tier- und Mob-Spawner
- Spawner und Mobs sollen stackbar sein
- Spawner sind Investments mit bewusst langer ROI-Zeit
- Spawner dürfen die PvP-Economy nicht überschwemmen

## Prefixe

- statt Titles gibt es sammelbare Prefixe
- aus Crates, Events, Achievements, Seasons, Battle Pass etc.
- mehrere Prefixe können besessen werden
- ein aktiver Prefix wird angezeigt

## Death Messages

- Spieler können mehrere freigeschaltete Death Messages aktivieren
- bei jedem Kill wird zufällig eine der aktiven Nachrichten gewählt

## Daily Rewards

`/dailyrewards` öffnet ein eigenes Menü.
- Login-Streak
- Coins, Nethersterne, Crates, Prefix-Chancen, Spawner etc.
- hochwertiges Hologramm am Spawn

## Seasons / Battle Pass

- Season-System mit Leaderboards und Hall of Fame
- Battle Pass mit Free und optional Premium Track
- kein aggressiver Full-Wipe notwendig
- permanente Accountwerte, Ränge und Sammlerstücke bleiben grundsätzlich bestehen

## Hall of Fame

Am Spawn werden vergangene Season-Sieger dauerhaft sichtbar gemacht, z. B.:
- Season King
- Most Kills
- Richest Player
- Highest Killstreak
- Best Clan

## Collection Book

`/sammlung` zeigt Fortschritt bei:
- Prefixen
- Death Messages
- Special Items
- Event Items
- Crates
- Achievements

## Events

- fester großer Freitag-Eventslot um ca. 18 Uhr
- Phoenix als möglicher großer Gewinn bei geeigneter Spielerzahl
- weitere Events: KOTH, Last Man Standing, Tournament, Juggernaut, Drop Party, Treasure Hunt, Clan War

## Logging

Mindestens folgende Dinge werden geloggt und optional an Discord gesendet:
- Gutschein-Erzeugung und Einlösung
- große Geldtransfers
- Trades
- Spawner-Transaktionen
- Crate-Jackpots
- Rangänderungen
- Admin-Give
- Staff-Aktionen
- Economy-Auffälligkeiten
