# SkyKings UI/UX Audit

Verbindliche Basis: `docs/UI_UX_DESIGN_SYSTEM.md`.

## SkyKings-Core

| Feature | Status | Hauptprobleme | Migration |
|---|---|---|---|
| `/is` | GUI vorhanden | teils eigene Farben/Lore, zu viel Dekoglas | auf `UiTheme`, `UiItems`, feste Navigation, Empty/Status States |
| `/plot` | GUI vorhanden | eigener Stil, PlotSquared-Shortcuts teils nur Text | gleiche Navigation/Theme, Settings/Flags als Untermenue |
| `/clan` | GUI vorhanden | Confirmations und zentrale Formatierung noch nicht ueberall | ConfirmationMenu, Theme, Format, PlayerCards |
| PlayerShop | Owner-GUI vorhanden | Kauf-UX noch direkt, Economy-Darstellung inkonsistent | ShopItem/CurrencyDisplay/Confirmation |
| Enderchest | eigenes GUI | Sonderdesign/Sounds | zentrale Sounds, Navigation/Format angleichen |
| Shops | mehrere GUIs | unterschiedliche Lore/Preisformatierung | zentrale Economy Cards + Format |
| Commands/Kits/Ranks | GUI vorhanden | Legacy-Stil gemischt | schrittweise Theme-Migration |

## SkyKings-Combat

| Feature | Status | Hauptprobleme | Migration |
|---|---|---|---|
| PvP/Killstreak | funktional | Chatfeedback alt, `Netherstern`-Texte | kurze Messages, SkyKings Stern, Event-Feedback |
| Stats | funktional | kein modernes Profile-Hub | Profile/Combat/Collection/Rivals/History |
| Peace | GUI vorhanden | eigener Stil | zentrale Theme-/PlayerCard-Komponenten |
| Quests | GUI vorhanden | eigener Card-Code | QuestCard + zentrale Progress Bars |
| Battle Pass | GUI vorhanden | eigener Layout-Code | RewardCard/Status/Navigation |
| KOTH/Hot/End Zone | funktional | Status teils Chat/Hologramm uneinheitlich | Actionbar + StatusDisplay |
| Duels | Basis vorhanden | Wagers/Confirmation fehlen | Duel-Menue + Confirmation + Wager-Escrow |
| LMS | Basis vorhanden | Event-UI/Scoreboard fehlt | Event-Scoreboard/Actionbar |
| Secrets | Basis vorhanden | keine Loot-Room-Rotation | Secret Rooms + CooldownDisplay |
| Merchant | Basis vorhanden | statische Angebote | rotierender Black Market |

## SkyKings-Crates

| Feature | Status | Hauptprobleme | Migration |
|---|---|---|---|
| Crates | Roulette vorhanden | Preview/Choice noch Legacy-Layout | Theme, RewardCards, subtile Animation |
| Gutscheine | funktional | Item-/Message-Stile uneinheitlich | zentrale Item/Message-Regeln |

## SkyKings-Admin

| Feature | Status | Hauptprobleme | Migration |
|---|---|---|---|
| Admin Commands | funktional | primär Textcommands | Staff-Menues nur wo wirklich sinnvoll |
| Discord | Bridge vorhanden | kein Spieler-UI-Thema | nur Status/Feedback zentralisieren |

## Verbindliche Reihenfolge

1. Shared UI Framework im Core etablieren.
2. Neue Features ausschliesslich damit entwickeln.
3. Sichtbare High-Traffic-Menues migrieren: Profile, Collection, Quests, Battle Pass, Clans, Duels, Market.
4. Danach Legacy-Menues Plugin fuer Plugin refactoren.
5. Vor Fertigstellung: Theme, Navigation, Empty/Error/Confirmation, Sounds, 1.8.9, Performance, CI.
