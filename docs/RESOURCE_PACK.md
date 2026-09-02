# SkyKings Resource Pack — 1.8.9

Stand: 2026-09-02

Der Pack ist ein fester Pre-Launch-Baustein, aber keine Gameplay-Abhaengigkeit. Technische Details und Build-Anleitung stehen in `resource-pack/README.md`.

## Status

- Foundation: IMPLEMENTIERT
- Build-ZIP: IMPLEMENTIERT
- CI-Artifact: IMPLEMENTIERT
- Core-Icons: OFFEN
- Economy-/Progression-Icons: OFFEN
- Branding-PNGs: OFFEN
- Client-Runtime-Test: OFFEN
- stabile HTTPS-Auslieferung: OFFEN

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
