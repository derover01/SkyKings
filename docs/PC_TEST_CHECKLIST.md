# SkyKings – PC Test Checklist

Diese Checkliste ist der feste Einstieg, sobald Martin wieder am PC ist.

Trigger im Chat: **„ich bin am PC“**

## 1. Repository aktualisieren und bauen

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
.\scripts\deploy-server.ps1
```

Erwartung: `BUILD SUCCESS`.

Bei Build-Fehlern: kompletten Maven-Fehlerblock an ChatGPT senden, besonders ab dem ersten `[ERROR]` inklusive Datei/Zeilennummer.

## 2. Server starten

Nur wenn der Build erfolgreich war:

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Beim Start auf `SEVERE`, `Exception`, `Could not load`, `NoSuchMethodError`, `NoClassDefFoundError` und deaktivierte SkyKings-Module achten.

## 3. Runtime-Schnellcheck

Ingame zuerst:

```text
/skcheck
```

Erwartet werden insbesondere:
- SkyKings-Core: OK
- SkyKings-Combat: OK
- SkyKings-Crates: OK
- SkyKings-Admin: OK
- LuckPerms: OK
- Vault: OK
- Core API: OK
- Island Access API: OK
- Plot Access API: OK
- SkyPvP Welt: geladen, sobald Produktionsmap geladen wurde
- SkyIslands: geladen
- SkyPlots: geladen

## 4. Kritische Smoke Tests

### Core / Komfort
- `/ec` öffnen
- normale Enderchest rechtsklicken -> Custom `/ec`
- Seitenwechsel und Kauf testen
- `/speed 1`, `/speed 5`, `/speed 10`, `/speed reset`
- `/anvil`
- `/workbench`
- `/enchantmenttable`

### Map-Schutz
- ohne `/buildmode` auf SkyPvP abbauen/platzieren versuchen -> muss blockiert werden
- `/buildmode` als Staff aktivieren -> bauen muss möglich sein
- `/buildmode` wieder deaktivieren

### Trade
- `/trade <Spieler>`
- Items einsetzen
- Coins anbieten
- bestätigen
- während Countdown Angebot ändern -> Abschluss muss abbrechen/resetten
- Disconnect/Close -> Items müssen sicher zurückkommen

### Shops
- System-/PvP-/Blacksmith-/Enchant-/Recycler-/Merchant-/Jackpot-NPC öffnen
- Kauf mit Coins testen
- Kauf mit physischen Nethersternen testen
- Blacksmith/Repair prüfen

### Islands
- `/is create`
- `/is home`
- fremder Spieler darf nicht bauen
- `/is trust <Spieler>` -> danach darf der Spieler bauen
- `/is untrust <Spieler>` -> danach wieder gesperrt

### Plots
- `/plot create`
- `/plot home`
- Schutz/Trust wie bei Islands testen

### PlayerShops
- auf eigener Island oder eigenem Plot `/playershop create`
- `/playershop set <Menge> <Coins>`
- passendes Item halten und `/playershop stock <Menge>`
- Kauf mit zweitem Spieler
- `/playershop claim`
- `/playershop withdraw <Menge>`
- Shop außerhalb eigener Claim-Fläche darf nicht funktionieren

### Spawner
- Spawner auf Island/Plot setzen
- mit weiterem Spawner rechtsklicken -> Stack erhöhen
- `/spawnerstack`
- Stack abbauen -> korrekte Anzahl Spawner zurück

### Retention
- `/daily`
- `/season`
- `/pvplevel`
- `/achievements`
- `/quests`
- `/battlepass`

### Peace / Friede
`/peace` und `/friede` sind dasselbe System.

Test:
- `/friede <Spieler>`
- Gegenüber nimmt an
- gegenseitiger Schaden muss blockiert sein
- dabei darf kein CombatTag entstehen
- `/friede remove <Spieler>` -> PvP danach wieder möglich

### Map Gameplay
- `/mapsetup`
- `/kingaltar info`
- `/hotzone list`
- `/endzone info`
- `/secret list`
- `/route list`
- `/landmark list`
- `/mapdisplay list`
- `/trashbin list`

## 5. Danach an ChatGPT schicken

1. Ergebnis von `deploy-server.ps1`
2. Start-Log ab dem Laden der SkyKings-Plugins bis `Done`
3. Ausgabe von `/skcheck`
4. Auffälligkeiten aus den Smoke Tests

Danach werden zuerst Build-/Runtime-Probleme korrigiert und anschließend Duel/LMS/Tournament, AMS/Mob-Stacking, Hall of Fame, Discord-Logging und der finale Release-Hardening-Pass weitergebaut.
