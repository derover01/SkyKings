# SkyKings

SkyKings ist ein Minecraft-OP-SkyPvP-Projekt auf Legacy-Combat-Basis (1.8.9-Client / 1.8.8-Serverstack).

## Ziel

Ein moderner Neuaufbau des klassischen OP-SkyPvP-Gefühls mit langfristiger Progression, eigener Economy, Crates, Rängen, Islands, Plots, Villager-PlayerShops, Events, Seasons und einem starken Community-Fokus.

## Kernprinzipien

- SkyPvP bleibt der Hauptmodus.
- Kein Fallschaden in der PvP-Welt.
- OP-Goldäpfel, Enderperlen, Stärke II und Schnelligkeit II sind zentrale Combat-Ressourcen.
- Kämpfe sollen durch Timing, Armor-Durability, Consumable-Management und Positioning entschieden werden.
- Free-Spieler können relevante Progression vollständig erspielen.
- Paid-Ränge bieten Komfort, Prestige, Kits und Crate-Rewards, dürfen aber die PvP-Balance nicht vollständig entwerten.
- Alle Economy-Systeme werden auf einen gemeinsamen Wertmaßstab kalibriert.
- Kritische Aktionen werden intern und optional nach Discord geloggt.

## Ranghierarchie

### Free
1. Spieler
2. Iron
3. Gold
4. Epic
5. Diamond

### Paid
1. Knight
2. Phoenix
3. Eternal
4. Exile
5. Endling
6. King

### Team
Builder, Azubi, Test Supporter, Supporter, SRSupporter, Moderator, SRModerator, Head of Mods, Admin, Headadmin, Superadmin, Manager, STV. Owner, Owner

## Geplante Projektstruktur

```text
SkyKings/
├── docs/
├── plugins/
│   ├── SkyKings-Core/
│   ├── SkyKings-Combat/
│   ├── SkyKings-Crates/
│   └── SkyKings-Admin/
├── server/
│   ├── configs/
│   ├── maps/
│   └── plugins/
├── tools/
└── README.md
```

## Entwicklungsprinzip

Features werden modular gebaut. Zuerst entsteht ein stabiles Fundament aus Core, Rängen, Economy, Kits und Combat. Danach folgen Crates, Gutscheine, Events, Islands, Plots, Shops, Spawner, Seasons, Battle Pass und Discord-Integration.

Siehe `docs/ARCHITECTURE.md`, `docs/GAMEPLAY.md` und `docs/ROADMAP.md`.
