# SkyKings Resource Pack — 1.8.9

Stand: 2026-09-02

Der Pack ist ein fester Pre-Launch-Baustein, aber keine Gameplay-Abhaengigkeit. Technische Details und Build-Anleitung stehen in `resource-pack/README.md`.

## Status

- Foundation: IMPLEMENTIERT
- Build-ZIP: IMPLEMENTIERT
- CI-Artifact: IMPLEMENTIERT
- Server-Delivery (`/pack` + Join): IMPLEMENTIERT
- HTTPS-/1.8-URL-Validierung: IMPLEMENTIERT
- Core-Icons: OFFEN
- Economy-/Progression-Icons: OFFEN
- Branding-PNGs: OFFEN
- Client-Runtime-Test: OFFEN
- stabile HTTPS-Auslieferung: OFFEN

## Server-Auslieferung

SkyKings-Core kann den Pack ueber die klassische Minecraft-1.8-Resource-Pack-Anforderung an den Client senden. Das Feature ist bewusst standardmaessig deaktiviert, bis die finale ZIP unter einer stabilen direkten HTTPS-URL liegt.

`plugins/SkyKings-Core/config.yml`:

```yaml
resource-pack:
  enabled: false
  send-on-join: true
  join-delay-ticks: 40
  url: ""
```

Release-Ablauf:

1. finale ZIP bauen,
2. ZIP unter einer direkten stabilen HTTPS-URL hosten,
3. URL in `resource-pack.url` eintragen,
4. `enabled: true` setzen,
5. Server komplett neu starten,
6. mit frischem 1.8.9-Client Join-Auslieferung testen,
7. `/pack` als manuellen Retry testen.

Die URL-Pruefung ist fail-closed: leer, HTTP statt HTTPS, Nicht-ASCII, Fragmente oder offensichtlich ungeeignete URLs werden nicht an den Client gesendet. Ein kaputter/nicht konfigurierter Pack deaktiviert niemals das Gameplay.

## 1.8.9-Regeln

- `pack_format: 1`
- kein modernes CustomModelData
- keine modernen JSON-Font-/Glyph-Provider
- Icon-Texturen nur auf bewusst reservierten Vanilla-Material-/Data-Kombinationen
- Schwerter, Bow, Rod, Armor, Ender Pearls, Golden Apples und wichtige PvP-Bloecke im ersten Pack-Pass nicht global ueberschreiben
- jedes Icon braucht weiterhin einen eindeutigen Itemnamen/Lore-Fallback

## Asset-Manifest

Eine Material-/Data-Kombination gilt erst dann als reserviert, wenn sie gegen das gesamte Gameplay geprueft wurde.

| Bereich | Icon | Material/Data | PNG | Gameplay geprueft | Status |
|---|---|---|---|---|---|
| Navigation | Home | TBD | TBD | nein | offen |
| Navigation | Back | TBD | TBD | nein | offen |
| Navigation | Next | TBD | TBD | nein | offen |
| State | Locked | TBD | TBD | nein | offen |
| State | Ready | TBD | TBD | nein | offen |
| State | Completed | TBD | TBD | nein | offen |
| Premium | Premium | TBD | TBD | nein | offen |
| Economy | Coins | TBD | TBD | nein | offen |
| Economy | Netherstar | TBD | TBD | nein | offen |
| Economy | Shop | TBD | TBD | nein | offen |
| Economy | Trade | TBD | TBD | nein | offen |
| Economy | Jackpot | TBD | TBD | nein | offen |
| Progression | Battle Pass | TBD | TBD | nein | offen |
| Progression | Quest | TBD | TBD | nein | offen |
| Progression | Kit | TBD | TBD | nein | offen |
| Social | Clan | TBD | TBD | nein | offen |
| Event | Duel | TBD | TBD | nein | offen |

## Runtime-Gate

Vor groesserem Launch:

- [ ] Pack-ZIP laedt auf frischem 1.8.9-Client
- [ ] `/pack` fordert die konfigurierte ZIP erneut an
- [ ] Join mit `send-on-join: true` fordert die ZIP nach dem konfigurierten Delay an
- [ ] deaktivierter/leer konfigurierter Pack erzeugt keinen Join-Fehler
- [ ] keine Missing-Texture-Flaechen
- [ ] GUI Scale Small/Normal getestet
- [ ] Battle Pass, Quests, Kits, Crates und Commands Hub mit Pack lesbar
- [ ] dieselben Systeme ohne Pack voll bedienbar
- [ ] PlayerShop/Trade funktionieren mit und ohne Pack
- [ ] normales PvP-Pack bleibt fuer Waffen/Ruestung nutzbar
- [ ] finale ZIP unter stabiler HTTPS-URL erreichbar
- [ ] Server-Pack-Auslieferung nach Relog/Restart getestet

## Build

```powershell
.\scripts\build-resource-pack.ps1
```

Output:

```text
build/resource-pack/SkyKings-ResourcePack-1.8.9.zip
```
