# SkyKings – PC Test Checklist

Diese Datei ist die kompakte Runtime-Checkliste. Die **verbindliche aktuelle Reihenfolge** steht in `docs/NEXT_PC_CHECKLIST.md`.

## 1. Aktualisieren / Preflight / Deploy

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
$env:Path += ";C:\Users\marti\OneDrive\Desktop\SkyKings\tools\apache-maven-3.9.16\bin"
.\scripts\release-preflight.ps1
.\scripts\deploy-server.ps1
```

Erwartung: Tests + Build komplett gruen.

## 2. Plotwelt fuer den neuen Raster-Test resetten

Server muss aus sein:

```powershell
.\scripts\reset-skyplots.ps1
```

Danach:
- 65x65 Gras = ein Plot
- 7 Block Stone-Brick = neutrale Strasse
- Holzstufe frei / Steinstufe claimed
- `/p rand`
- `/p merge`
- Roads ausserhalb eines Merge bleiben unantastbar

## 3. Server starten

```powershell
cd ".\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Auf `SEVERE`, Exceptions und deaktivierte Module achten.

## 4. Runtime-Schnellcheck

```text
/skcheck
```

Core, Combat, Crates, Admin, APIs und aktive Commands muessen OK sein.

## 5. GUI / UI

```text
/commands
/kit
/battlepass
/quests
/craterewards
/prefix
/top
/gutscheine
```

Pruefen:
- keine Slot-Crashes
- Custom-Panel-Stil konsistent
- Lore sauber umgebrochen
- Back/Home Navigation
- READY / LOCKED / COOLDOWN / COMPLETED eindeutig
- Clan-Tag zerstoert Rang/Prefix nicht

## 6. Combat / Economy / Security

- CombatTag / Enderperlen-Cooldown
- legitime Kills vs. Anti-Farm
- Bounty nur bei legitimen Kill
- Voucher/Crate Rapid-Click + Restart-Replay
- PlayerShop Kauf vs. Stock-Withdraw
- Revenue Doppelclaim
- Trade-Angebot waehrend Countdown aendern
- Item droppen und sofort Commands/GUIs oeffnen

## 7. Islands / Plots / Spawner

- Island Create / Level / Welcome Visit
- Plot Claim / Flags / Trust / Deny / Rand / Merge
- Spawner bis Limit stacken
- Spawner-Mobs gleicher Art werden automatisch gestackt
- ein Kill reduziert Mob-Stack genau um 1
- Drops/XP nur fuer einen Kill
- Restart / Chunk Reload / Performance

## 8. Finale Event-Welt

```text
/skymap event SkyEvents
```

Die finale Eventmap enthaelt:
- Hub
- Duel
- LMS
- Clan Wars

**Tournament und Juggernaut sind bewusst nicht Bestandteil des Servers.**

Danach gemaess `SERVER_SETUP_TODO.md` Arena-Punkte setzen und testen:
- Duel normal + Wager
- LMS
- Clan Wars 2v2 / 3v3 / 5v5
- Quit/Forfeit
- Team-Damage
- Event-Kills nicht in Open-World-Stats

## 9. Map / Buildmode

- SkyPvP ohne Buildmode geschuetzt
- Staff mit Buildmode kann bauen
- Eventmap ohne Plot-Meldungen
- Farmland bleibt geschuetzt
- KOTH / HotZones / Secrets / Routes / Displays pruefen

## 10. Backup / Restart / Restore

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
.\scripts\backup-server.ps1
```

Daten veraendern -> sauber stoppen -> Restart -> Persistenz pruefen -> spaeter Guarded Restore testen.

## 11. Finales Gate

Erst danach:
- Economy-Balance
- Kit-Cooldowns
- Battle-Pass-XP/Rewards
- Crate-Chancen
- Mob-/Spawner-Limits
- TPS unter realer Spielerzahl
- Soft Launch

Der Resource-Pack-Layer kommt danach als UI-Polish.
