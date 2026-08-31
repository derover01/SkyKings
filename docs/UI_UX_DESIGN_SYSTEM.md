# SkyPvP UI/UX & Design System – verbindliche Entwicklungsrichtlinie

Du arbeitest an einem Minecraft OP-SkyPvP-Server auf Version 1.8.9.

Der Server soll das klassische SkyPvP-Spielgefühl früherer 1.8-Server behalten, aber visuell, technisch und hinsichtlich UX wie ein moderner Server aus der heutigen Zeit wirken.

Das Ziel ist NICHT, möglichst viele Farben, Effekte oder dekorative Elemente einzubauen.

Das Ziel ist:

**clean, modern, hochwertig, konsistent, minimalistisch, ästhetisch und professionell.**

Der gesamte Server soll wie EIN Produkt wirken und nicht wie eine Sammlung verschiedener Plugins.

---

# 1. Grundregel

Jedes bestehende und zukünftige Plugin muss sich an dasselbe UI/UX-System halten.

Das betrifft:

- Inventory GUIs
- Item-Namen
- Item-Lore
- Chat-Nachrichten
- Fehlermeldungen
- Success-Messages
- Commands
- Scoreboards
- Actionbar
- Titles
- Subtitles
- Tablist
- Hologramme
- NPC-Beschriftungen
- Shops
- Quests
- Profile
- Statistiken
- Leaderboards
- Crates
- Bounties
- Events
- Clans
- Duels
- Kit-Auswahl
- Einstellungen
- Bestätigungsmenüs
- Pagination
- Sounds
- Animationen
- Cooldowns
- Progress Bars
- Statusanzeigen
- sämtliche andere Spielerinteraktionen

Kein Plugin darf sein eigenes visuelles System erfinden.

---

# 2. Design-Philosophie

Der Server soll aussehen wie:

**Minecraft 1.8.9 Combat + modernes Game UI/UX Design.**

Orientiere dich bei UX eher an modernen Games und Apps als an klassischen Minecraft-Servern.

Vermeide typisches altes Minecraft-Server-Design wie:

- komplett mit Glass Panes gefüllte GUIs
- fünf verschiedene Farben gleichzeitig
- Rainbow-Texte
- übermäßig fett geschriebene Texte
- `§k` Magic Text
- unnötig lange Lore
- überall „CLICK HERE“
- überall Großbuchstaben
- unterschiedliche Designs pro Plugin
- unnötige Trennlinien
- 15 Informationen in einem Item
- Spam im Chat
- überladene Scoreboards
- zufällige Unicode-Symbole ohne Bedeutung
- Emojis ohne einheitliches System

Das Design soll bewusst reduziert sein.

Weniger Elemente, aber dafür jedes Element durchdacht.

---

# 3. Visuelle Hierarchie

Informationen werden immer nach Wichtigkeit sortiert.

## Priorität 1

Was ist das?

## Priorität 2

Was ist der aktuelle Status?

## Priorität 3

Welche wichtige Kennzahl gibt es?

## Priorität 4

Was kann der Spieler tun?

Beispiel:

`§bBounty Board`

`§7Aktive Kopfgelder`

`§f18 Spieler`

`§eKlicken zum Öffnen`

Nicht 12 weitere Zeilen hinzufügen, wenn sie nicht unmittelbar relevant sind.

---

# 4. Farb-System

Nutze wenige feste Farben.

## Primary / Accent

Aqua / helles Cyan

Beispiel:
`§b`

Verwendung:

- aktive Elemente
- wichtige Navigation
- Branding
- ausgewählte Buttons
- aktive Statusinformationen

## Primary Text

Weiß

`§f`

## Secondary Text

Grau

`§7`

## Disabled / Hintergrund

Dunkelgrau

`§8`

## Success

Grün

`§a`

## Warning

Gelb / Gold

`§e` oder gezielt `§6`

## Danger

Rot

`§c`

## Legendary

Gold

`§6`

## Mythic / extrem selten

Hellviolett

`§d`

Keine Farbe darf grundlos verwendet werden.

Nicht jedes Wort einzeln einfärben.

---

# 5. Naming-System

Feature-Namen sollen kurz, klar und hochwertig sein.

Beispiele:

- Profile
- Combat
- Progression
- Collection
- Bounties
- Clans
- Events
- Market
- Black Market
- Forge
- Duels
- Settings
- Leaderboards

Vermeide unnötige Namen wie:

- Super Awesome Player Settings
- Ultimate OP Shop
- Mega Special Daily Rewards

Kurze Begriffe wirken hochwertiger.

---

# 6. GUI-Struktur

Alle größeren GUIs sollen denselben Aufbau verwenden.

Wenn technisch sinnvoll:

## Header / obere Ebene

zeigt:

- Name des Bereichs
- optional Spielerstatus
- optional wichtigste Kennzahl

## Content

mittlere Slots:

- eigentliche Funktionen

## Navigation

untere Reihe:

- Zurück
- Home
- nächste/vorherige Seite
- Einstellungen
- relevante Navigation

Navigation soll möglichst immer an denselben Positionen liegen.

Ein Spieler soll nach dem ersten Menü intuitiv verstehen, wie alle anderen Menüs funktionieren.

---

# 7. GUI-Raster

Entwickle ein einheitliches Slot-System.

Beispielsweise:

Slot 45:
Zurück

Slot 49:
Home / Hauptmenü

Slot 53:
Weiter

Wenn eine GUI keine Pagination benötigt, können Slots angepasst werden, aber die grundlegende Navigation soll konsistent bleiben.

Keine Buttons zufällig von GUI zu GUI verschieben.

---

# 8. Empty Space

Nicht jeder freie Inventory-Slot muss gefüllt werden.

Leerraum ist erlaubt und ausdrücklich erwünscht.

GUIs sollen nicht automatisch mit 30 Glass Panes gefüllt werden.

Dekoration darf verwendet werden, wenn sie die Struktur verbessert, aber nicht als Standardlösung für jeden freien Slot.

Das Menü soll atmen können.

---

# 9. Item-Lore

Lore muss kurz und übersichtlich sein.

Ideal:

2–6 Informationszeilen.

Beispiel:

`§bCombat Profile`
`§7Deine persönlichen PvP-Statistiken.`
`§f4.281 Kills §8• §f2.45 K/D`
`§eKlicken zum Öffnen`

Keine Textwände.

Falls viele Informationen notwendig sind, verteile sie auf Untermenüs.

---

# 10. Zahlenformatierung

Große Zahlen müssen lesbar formatiert werden.

Beispiele:

`4.281`
`18.420 Coins`
`1,8 Mio.`

Nutze ein einheitliches System im gesamten Server.

Keine Mischung aus:

`18420`
`18,420`
`18.420`
`18.4k`

Innerhalb derselben UI.

---

# 11. Progress Bars

Progression soll visuell dargestellt werden.

Beispiel:

`███████░░░ 73%`

oder eine für 1.8.9 geeignete Variante.

Nutze Progress Bars für:

- Level
- Prestige
- Quests
- Cooldowns
- Forge
- Events
- Challenges
- Achievements

Aber nicht überall.

---

# 12. Status-System

Wiederkehrende Statusanzeigen müssen einheitlich sein.

Beispiele:

ACTIVE
READY
LOCKED
COMPLETED
COOLDOWN
CONTESTED
CLAIMED

Farblogik:

Grün = positiv / bereit

Gelb = wartet / Warnung

Rot = Fehler / Gefahr / nicht möglich

Grau = deaktiviert / gesperrt

Aqua = aktiv / ausgewählt

---

# 13. Selected State

Ausgewählte Optionen müssen visuell eindeutig sein.

Zum Beispiel:

`§b● Selected`

Nicht ausgewählt:

`§7○ Select`

Dies beispielsweise für:

- Einstellungen
- Kits
- Sortierung
- Filter
- Kategorien
- Profileinstellungen

---

# 14. Sounds

Sounds sind Teil des UI.

Definiere zentrale Sound-Kategorien.

## UI_CLICK

normaler Menü-Klick

## UI_BACK

zurück

## UI_SUCCESS

Kauf erfolgreich / Claim erfolgreich

## UI_ERROR

nicht genug Coins / nicht möglich

## UI_NOTIFY

wichtige Information

## UI_LEVEL_UP

Level erreicht

## UI_REWARD

Reward erhalten

## UI_WARNING

Bounty / Target / Gefahr

Keine zufälligen Sounds pro Plugin.

Sounds sollen subtil eingesetzt werden.

---

# 15. Titles & Subtitles

Titles nur für wichtige Ereignisse.

Beispiele:

LEVEL UP

`37 → 38`

oder:

KILLSTREAK

`25 Kills`

oder:

BOUNTY CLAIMED

`+18.500 Coins`

Keine Titles für triviale Aktionen wie das Öffnen eines Menüs.

---

# 16. Actionbar

Actionbar für temporäre, kontextbezogene Informationen.

Beispiele:

Combat:

`⚔ Combat • 7.4s`

Bounty:

`☠ Target: Martin • 8.500 Coins`

KOTH:

`♛ KOTH • Contested • 01:28`

Cooldown:

`Ender Pearl • 2.4s`

Nicht dauerhaft 6 verschiedene Informationen gleichzeitig anzeigen.

Informationen sollen kontextbezogen wechseln.

---

# 17. Scoreboard

Scoreboard muss minimalistisch bleiben.

Maximal ungefähr 8–10 relevante Informationszeilen.

Standard-Beispiel:

SKYPVP

Martin

Level 37
Coins 18.420

Kills 4.281
Streak 7

play.server.de

Während Events darf ein spezielles Event-Scoreboard verwendet werden.

Beispielsweise KOTH:

KOTH

Status CONTESTED
Owner -
Time 02:48

Kills 4

play.server.de

Scoreboards sollen situationsabhängig sein.

---

# 18. Chat-System

Chat-Messages kurz halten.

Nicht:

`You have successfully purchased the selected item from the shop for the amount of 5000 coins!`

Sondern:

`✔ Kauf erfolgreich`
`5.000 Coins wurden abgezogen.`

Fehler:

`✕ Nicht genug Coins`
`Dir fehlen 1.250 Coins.`

Keine unnötigen Präfixe vor jeder Nachricht.

---

# 19. Feedback-System

Jede wichtige Aktion benötigt Feedback.

Beispielsweise bei einem Kauf:

1. Klick
2. Success Sound
3. Item aktualisiert sich
4. kurze Nachricht
5. Coin-Anzeige aktualisiert sich sofort

Der Spieler darf sich niemals fragen:

„Hat mein Klick funktioniert?“

---

# 20. Confirmation-Menüs

Riskante Aktionen müssen bestätigt werden.

Beispiele:

- große Käufe
- Item zerstören
- Prestige
- Clan verlassen
- Reset
- teures Forge Upgrade
- Wager Duel

Einheitliches Schema:

GRÜN:
Bestätigen

ROT:
Abbrechen

In der Mitte:
Objekt / Aktion

---

# 21. Loading States

Wenn ein Feature Daten lädt, soll eine Loading-Anzeige existieren.

Zum Beispiel:

`Loading...`

mit kleiner Animation.

Nicht einfach eine leere GUI anzeigen.

---

# 22. Empty States

Wenn keine Daten vorhanden sind, soll das Menü trotzdem professionell aussehen.

Beispiel:

`Keine aktiven Bounties`

`Momentan wurde auf keinen Spieler ein Kopfgeld ausgesetzt.`

Nicht einfach ein komplett leeres Inventory.

---

# 23. Pagination

Pagination immer gleich.

Links:
Previous

Mitte:
`Page 2 / 5`

Rechts:
Next

Buttons nur anzeigen, wenn die jeweilige Seite existiert.

---

# 24. Profile Design

Spielerprofile sollen wie moderne Game Profiles aufgebaut sein.

Bereiche:

- Overview
- Combat
- Progression
- Collection
- Achievements
- Rivals
- Events
- Clan
- History

Nicht alle Informationen auf eine Seite pressen.

---

# 25. Item Design

Custom Items müssen ebenfalls das Design-System verwenden.

Beispiel:

`§fNightfall`
`§8Legendary Sword`

`§7Kills §f1.284`
`§7Owner`
`§fMartin`

`§6LEGENDARY`

Keine extrem langen Item-Namen.

Keine unnötigen Farbcodes.

Keine permanenten Magic-Buchstaben.

---

# 26. Economy UI

Bei Käufen immer anzeigen:

- Preis
- aktuelles Guthaben
- gegebenenfalls fehlender Betrag

Beispiel:

`Preis`
`§f25.000 Coins`

`Dein Guthaben`
`§f18.420 Coins`

`§cDir fehlen 6.580 Coins.`

---

# 27. Hover / Dynamic Lore

Wenn möglich, sollen Buttons ihren aktuellen Zustand anzeigen.

Beispiel Quest:

DAILY HUNTER

`Kills`
`17 / 25`

`██████░░░░ 68%`

`Reward`
`2.500 Coins`

Nicht einfach:

`Click to view quest`

---

# 28. Animationen

Animationen sparsam, aber hochwertig einsetzen.

Geeignet für:

- Crates
- Level Up
- Forge
- Prestige
- große Rewards
- Event Start
- Black Market Refresh
- GUI Loading

Keine permanent blinkenden Menüs.

Animationen sollen eine Funktion haben.

---

# 29. Microinteractions

Das UI soll auf Spieleraktionen reagieren.

Beispiele:

- Button wird nach Klick aktualisiert
- Reward wechselt zu CLAIMED
- Quest Progress aktualisiert sich sofort
- Kauf-Button wird bei zu wenig Coins rot
- aktiver Filter erhält selected state
- Cooldown wird dynamisch angezeigt
- Eventstatus ändert sich live

---

# 30. Command UX

Commands müssen ebenfalls konsistent sein.

Bei falscher Verwendung:

Nicht:

`Usage: /bounty <player> <amount>`

Sondern:

`Bounty erstellen`
`/bounty <Spieler> <Coins>`

und bei Fehler:

`Spieler nicht gefunden.`

Commands sollen möglichst über GUI erreichbar sein, sofern eine GUI sinnvoll ist.

Commands bleiben als Power-User-Alternative verfügbar.

---

# 31. Bestehende Plugins auditieren

Gehe ALLE bereits vorhandenen Plugins durch.

Erstelle zunächst eine Übersicht:

Plugin
Feature
vorhandene GUI
vorhandene Messages
Scoreboard/Actionbar
Sounds
Design-Probleme
notwendige Änderungen

Danach vereinheitliche jedes Plugin.

Prüfe insbesondere:

- unterschiedliche Farben
- unterschiedliche Navigationsslots
- unterschiedliche Lore-Stile
- unterschiedliche Message-Formate
- doppelte GUI-Komponenten
- unterschiedliche Sounds
- überladene GUIs
- fehlende Feedback-States
- fehlende Empty States
- fehlende Confirmation
- uneinheitliche Begriffe
- inkonsistente Zahlenformate
- veraltete Texte
- unnötige Chat-Messages

Bestehende Funktionalität darf dabei nicht ohne Grund verändert oder entfernt werden.

---

# 32. Zentrales UI Framework

Erstelle wenn technisch sinnvoll ein gemeinsames internes UI-System, das alle Plugins verwenden.

Nicht jedes Plugin soll eigene Helper-Klassen für:

- ItemBuilder
- GUI
- Messages
- Sounds
- Pagination
- Color Formatting
- Progress Bars
- Confirmation
- Player Head
- Navigation

bauen.

Stattdessen soll es zentrale Komponenten geben.

Beispielsweise:

UiManager

MenuBuilder

MenuItemBuilder

NavigationComponent

PaginationComponent

ConfirmationMenu

MessageService

SoundService

FormatService

ProgressBarService

PlayerHeadService

Theme

Das Ziel ist, dass Designänderungen später zentral durchgeführt werden können.

---

# 33. Zentraler Theme Config

Farben, Standardtexte und wichtige Symbole nicht überall hardcoden.

Erstelle eine zentrale Theme-Konfiguration bzw. Konstantenstruktur.

Zum Beispiel:

PRIMARY
TEXT
MUTED
SUCCESS
WARNING
DANGER
LEGENDARY

BACK_BUTTON
HOME_BUTTON
NEXT_BUTTON
PREVIOUS_BUTTON

SOUND_CLICK
SOUND_SUCCESS
SOUND_ERROR

Damit bleibt der gesamte Server langfristig konsistent.

---

# 34. Wiederverwendbare Components

Erstelle standardisierte UI-Komponenten.

Beispiele:

PlayerCard

StatCard

RewardCard

QuestCard

ShopItem

NavigationBar

ProgressDisplay

CurrencyDisplay

CooldownDisplay

StatusDisplay

ConfirmationDialog

LeaderboardEntry

PaginationBar

FeatureLockedCard

Diese Komponenten sollen in mehreren Plugins verwendet werden.

---

# 35. Neue Plugins

Bei JEDEM zukünftigen Plugin muss zuerst überlegt werden:

1. Wo befindet sich das Feature im bestehenden UX-System?
2. Braucht es überhaupt eine eigene GUI?
3. Kann eine bestehende GUI erweitert werden?
4. Welche vorhandenen UI Components können wiederverwendet werden?
5. Welche Informationen benötigt der Spieler wirklich?
6. Welche Feedback-Zustände existieren?
7. Welche Sounds werden verwendet?
8. Was passiert bei einem Fehler?
9. Was passiert, wenn keine Daten vorhanden sind?
10. Wie funktioniert Navigation zurück und nach Hause?
11. Wie funktioniert das Feature auf einem kleinen Bildschirm?
12. Ist die GUI auch bei vielen Daten übersichtlich?

Erst danach implementieren.

---

# 36. Keine Feature-Inseln

Neue Features dürfen nicht isoliert wirken.

Beispiel:

Bounty soll mit Profile, Stats, Killfeed, Leaderboard und Events verbunden sein.

Quests sollen mit Level, Coins und Achievements verbunden sein.

Clans sollen mit Profiles, Leaderboards und Events verbunden sein.

Der Server soll wie ein zusammenhängendes System wirken.

---

# 37. Performance

Da der Server Minecraft 1.8.9 verwendet, muss Performance jederzeit berücksichtigt werden.

Vermeide:

- unnötige Inventory Updates pro Tick
- übermäßige Packets
- permanente Animationen
- unnötige Datenbankqueries
- synchrones Laden großer Datenmengen
- unnötig komplexe Scoreboard Updates
- Memory Leaks
- mehrfach registrierte Listener

UI darf hochwertig sein, aber niemals Performance opfern.

---

# 38. Technische Kompatibilität

Alle Lösungen müssen Minecraft 1.8.9 kompatibel sein.

Verwende keine APIs oder Features späterer Minecraft-Versionen, ohne vorher eine kompatible Alternative zu entwickeln.

Falls eine moderne UI-Idee technisch nicht direkt auf 1.8.9 umsetzbar ist:

Nicht einfach entfernen.

Suche eine kreative 1.8.9-kompatible Umsetzung mit:

- Inventory GUI
- Player Heads
- Item Metadata
- Lore
- Enchantment Glow
- Scoreboard
- Titles
- Actionbar
- Hologrammen
- Sounds
- Partikeln
- Resource Pack
- Legacy Font Möglichkeiten

---

# 39. Qualitätsprüfung vor Fertigstellung

Bevor ein Plugin oder eine Überarbeitung als fertig gilt, kontrolliere:

### Design

- passt es zum Theme?
- gleiche Farben?
- gleiche Sprache?
- gleiche Navigation?
- clean?
- unnötige Elemente entfernt?

### UX

- versteht ein neuer Spieler die Funktion?
- erhält jeder Klick Feedback?
- gibt es Error States?
- gibt es Empty States?
- gibt es Confirmation wo notwendig?
- kommt man zurück?

### Konsistenz

- verwendet es bestehende Components?
- verwendet es zentrale Formatter?
- verwendet es zentrale Sounds?
- verwendet es zentrale Messages?

### Technik

- 1.8.9 kompatibel?
- performant?
- keine unnötigen Tasks?
- keine duplizierten Systeme?

---

# 40. Entscheidungsregel

Wenn du zwischen zwei Designs entscheiden musst:

Wähle grundsätzlich die Variante, die:

1. weniger visuelles Chaos erzeugt
2. schneller verstanden wird
3. hochwertiger wirkt
4. weniger Farben benötigt
5. konsistenter mit bestehenden Menüs ist
6. weniger Informationen gleichzeitig zeigt
7. trotzdem alle relevanten Informationen bereitstellt

---

# 41. Zielzustand

Wenn ein Spieler zwischen:

Profile
Bounties
Shop
Quests
Clans
Duels
Forge
Black Market
Events

wechselt, soll er sofort erkennen:

„Das gehört alles zu demselben Server.“

Der Server darf sich niemals wie zehn verschiedene Plugins anfühlen.

Die komplette Spielerfahrung soll wie ein einziges zusammenhängendes modernes Game Interface aufgebaut sein.

---

# Deine Aufgabe ab jetzt

Behandle diese Richtlinie als verbindliches Design-System für das gesamte Projekt.

Für bereits existierende Plugins:

1. Code und sämtliche Spielerinterfaces analysieren.
2. UI/UX-Probleme dokumentieren.
3. Inkonsistenzen identifizieren.
4. vorhandene Funktionalität erhalten.
5. GUIs und Messages auf das neue System migrieren.
6. wiederverwendbare Components extrahieren.
7. zentrale UI-Utilities erstellen.
8. danach Plugin für Plugin refactoren.

Für zukünftige Plugins:

Plane das Feature direkt innerhalb dieses Systems.

Bevor du eine neue GUI implementierst, definiere zuerst:

- Zweck
- Informationshierarchie
- Navigation
- States
- Slot Layout
- Farben
- Lore
- Sounds
- Error States
- Empty States
- Feedback
- verwendete Shared Components

Erstelle anschließend die Implementation.

Das Endprodukt soll nicht nur funktionieren.

Es soll sich **bewusst designed** anfühlen.
