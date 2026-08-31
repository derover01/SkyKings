# SkyKings Release Hardening

Stand: Phase 6–9 Foundations implementiert, Runtime-/Balancing-Test steht aus.

## 1. Build & Boot Gate

1. `git pull`
2. `scripts/deploy-server.ps1`
3. Build muss mit Java 8 / Spigot 1.8.8 erfolgreich sein.
4. Server starten und auf rote Exceptions achten.
5. Ingame `/skcheck` ausführen.

Erwartet:
- SkyKings-Core: OK
- SkyKings-Combat: OK
- SkyKings-Crates: OK
- SkyKings-Admin: OK
- LuckPerms: OK
- Vault: OK
- Core API: OK
- Island Access API: OK
- Plot Access API: OK
- SkyPvP: OK, sobald Produktionsmap geladen wurde
- SkyIslands: OK
- SkyPlots: OK

## 2. Kritische Dupe-/Datenverlust-Tests

### Trade
- zwei Spieler, Items + Coins tauschen
- während Countdown GUI schließen
- während Countdown disconnecten
- volles Zielinventar testen
- mehrfach schnell bestätigen/klicken
- Serverstop während offener Trade-Session simulieren

### PlayerShop
- Shop nur auf eigener Insel/eigenem Plot erstellbar
- fremde/trusted Claims testen
- Stock ein-/auszahlen
- Inventar voll beim Stock-Withdraw
- Käufer mit zu wenig Coins
- Käufer mit vollem Inventar
- Einnahmen claimen
- Shop mit Stock/Einnahmen darf nicht gelöscht werden
- Villager darf keinen Schaden nehmen
- verschobener Villager muss deaktiviert sein

### SpawnerStack
- 1 → 2 → 64 stacken
- bei 64 darf kein Item verschwinden
- Stack abbauen: exakt dieselbe Anzahl Spawner muss droppen
- Serverrestart: Stackanzahl bleibt erhalten
- fremde Claims dürfen nicht verändert werden
- WICHTIG: Creature-Type der Spawner in 1.8 separat testen; aktuelle NMS-freie Version garantiert nur die Anzahl, nicht den Mob-Typ im gedroppten Item.

### Economy
- `/sell hand`, `/sell all`
- Shops mit vollem Inventar
- Jackpot Neustart
- Daily Reward nur 1x pro Kalendertag
- BattlePass Reward nur 1x claimbar
- Quest Reward nur 1x pro Daily-/Weekly-Zyklus

## 3. Claim-Systeme

### `/is`
- create
- home / sethome
- trust / untrust
- visit
- Owner bauen
- Trusted bauen
- Fremder darf nicht bauen
- Explosion, Feuer, Lava/Wasser über Claimgrenze
- Inselabstand und Rand testen

### `/plot`
- identische Tests wie `/is`
- PlayerShop auf Plot testen

## 4. Map Gameplay

- `/skymap load SkyPvP`
- `/setspawn`
- `/mapsetup`
- King Altar setzen/testen
- Hot Zones setzen/testen
- End Zone setzen/testen
- Gold/Level/Blacksmith/Merchant Landmarks setzen
- Map-Loot Common/Rare/Epic setzen
- Supply-Punkte setzen
- Secrets setzen
- Pearl-/Jump-Routen setzen
- Trash-Bins setzen
- Map Displays setzen

## 5. Retention

- `/daily`
- `/season`
- `/pvplevel`
- `/achievements`
- `/battlepass`
- `/quests`
- Kill-XP und Same-Victim-Anti-Farm testen
- Pearl-Quest testen
- Premium BattlePass nur über Staff-Berechtigung aktivieren

## 6. Community

- `/peace <Spieler>` → accept/deny/remove
- Peace-Hits dürfen weder Schaden noch CombatTag auslösen
- `/verlosung start <Sekunden> <Coins>`
- `/verlosung join`
- Gewinner-Auszahlung prüfen

## 7. Noch nicht als Release-fertig markieren

Diese Features brauchen echte Arena-/NMS-/Runtime-Arbeit und sind bewusst nicht blind als fertig deklariert:
- `/duel` Arena-System
- LMS Event
- Turnier-Brackets
- Hall of Fame Season-Freeze
- automatische Mob-Entity-Stacks (AMS)
- Spawner Creature-Type-Preservation beim Abbau
- Skyblock-Missions-Baum / Insel-Level
- Insel-Shop-Miete
- Discord-Webhooks vollständig
- Crate-/Voucher-HMAC bzw. issued-serial registry
- finaler BattlePass-/Economy-Balancepass
- TPS-/Entity-Lasttest mit echten Spielern

## 8. Release Gate

Vor Public Release müssen mindestens folgende Gates grün sein:
- Maven Build
- kompletter Serverboot ohne Exception
- `/skcheck`
- 2-Spieler Trade-Test
- 2-Spieler PvP/Peace-Test
- Claim-Grief-Test
- PlayerShop Dupe-Test
- SpawnerStack Mengen-Test
- Crate/Voucher Replay-Test
- Restart-Persistenztest
- Backup/Restore-Test
- 30–60 Minuten Soak-Test mit MapLoot, Displays, Scoreboard und Shops
