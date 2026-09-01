# SkyKings – PC Test Checklist

Diese Datei ist die kompakte Runtime-Checkliste. Die **verbindliche aktuelle Reihenfolge** steht in `docs/NEXT_PC_CHECKLIST.md`.

## 1. Aktualisieren / Preflight / Deploy

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
git pull
$env:Path += ";C:\Users\marti\OneDrive\Desktop\SkyKings\tools\apache-maven-3.9.16\bin"
.\scripts\prepare-local-test.ps1 -ResetSkyPlots
```

Den Plot-Reset nur beim ersten Test des neuen Rasters verwenden. Spaeter reicht:

```powershell
.\scripts\prepare-local-test.ps1
```

Erwartung: Static Audit, Tests, Build und Deploy komplett gruen.

## 2. Server starten

```powershell
cd ".\server"
java -Xms1G -Xmx2G -jar spigot-1.8.8.jar nogui
```

Auf `SEVERE`, Exceptions und deaktivierte Module achten.

## 3. Runtime-Schnellcheck

```text
/skcheck
/skymap list
```

Core, Combat, Crates, Admin, APIs und aktive Commands muessen OK sein.

Offizielle Welten:
- `SkyPvP`
- `SkyPlots`
- `SkyIslands`
- `SkyCommunityEvent`

`SkyEvents` ist retired und darf nicht fuer den finalen Server erzeugt werden.

Bei `/skcheck` ausserdem beachten:
- Event Return Recovery muss installiert sein
- offene Event-Returns werden bewusst als Recovery-Zustand angezeigt
- Jackpot `REVIEW` muss vor weiteren Auszahlungen geklaert werden

## 4. Plots / Islands / Spawner

Plots:
- 65x65 Gras = ein Plot
- 7 Block Stone-Brick = neutrale Strasse
- Holzstufe frei / Steinstufe claimed
- `/p rand`
- `/p merge`
- Roads ausserhalb eines Merge bleiben unantastbar
- Restart-Persistenz

Islands / Spawner:
- Island Create / Level / Welcome Visit
- Spawner bis Limit stacken
- Spawner-Mobs gleicher Art werden automatisch gestackt
- ein Kill reduziert Mob-Stack genau um 1
- Drops/XP nur fuer einen Kill
- Restart / Chunk Reload / Performance

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
- Jackpot mit mindestens zwei Teilnehmern + Restart
- Map Mastery Zeit/Visits/Activities + Restart

## 7. Event-Arenen / Multiplayer

Keine separate Event-Produktionswelt erzeugen. Arena-Punkte gemaess `SERVER_SETUP_TODO.md` in einer geeigneten vorhandenen Welt setzen.

Testen:
- Duel normal + Wager + Setup-Kit
- identisches Kit fuer beide Duel-Spieler
- Originalinventar/XP/Effekte/Vitalwerte nach Duel restauriert
- kein Heal-/Food-Reset durch Duel
- LMS
- Clan Wars 2v2 / 3v3 / 5v5
- Quit/Forfeit
- Death-Screen-Quit
- Serverrestart waehrend Event/Death-Screen
- Event Return Recovery bringt Spieler zur urspruenglichen Position zurueck
- Team-Damage korrekt
- Event-Kills nicht in Open-World-Stats
- Wager/Rewards exakt einmal

**Tournament und Juggernaut sind bewusst nicht Bestandteil des Servers.**

## 8. Map / Buildmode

- SkyPvP ohne Buildmode geschuetzt
- Staff mit Buildmode kann bauen
- keine Plot-Meldungen ausserhalb von SkyPlots
- Farmland bleibt geschuetzt
- KOTH / HotZones / Secrets / Routes / Displays pruefen
- Shop-NPCs inklusive `/shopnpc bind jackpot` testen

## 9. Backup / Restart / Restore

```powershell
cd "C:\Users\marti\OneDrive\Desktop\SkyKings"
.\scripts\backup-server.ps1
```

Daten veraendern -> sauber stoppen -> Restart -> Persistenz pruefen -> Guarded Restore testen.

Stichproben:
- Coins
- Clan
- Plot/Rand/Merge
- Battle Pass / Quests
- PlayerShop
- Jackpot
- Map Mastery
- Event-Arena-Punkte
- Warps

## 10. Finales Gate

Erst danach:
- Economy-Balance
- Kit-Cooldowns
- Battle-Pass-XP/Rewards
- Crate-Chancen
- Mob-/Spawner-Limits
- TPS unter realer Spielerzahl
- kleiner geschlossener Soft Launch

Der Resource-Pack-Layer kommt danach als UI-Polish.
