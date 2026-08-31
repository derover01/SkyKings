# SkyKings Discord Setup

SkyKings nutzt fuer ausgehende Discord-Nachrichten einen eigenen Discord-Bot. Der Bot-Token wird **niemals** im GitHub-Repository gespeichert.

## 1. Discord Bot erstellen

Im Discord Developer Portal eine Application erstellen, unter **Bot** einen Bot anlegen und diesen in den SkyKings-Discord einladen.

Empfohlene minimale Rechte fuer die erste Bridge:
- View Channels
- Send Messages
- Embed Links (spaeter)
- Read Message History

Kein Administrator-Recht notwendig.

## 2. Channel-IDs

Developer Mode in Discord aktivieren und die IDs der vorgesehenen Channels kopieren.

Beim ersten Serverstart erzeugt `SkyKings-Admin` automatisch:

`plugins/SkyKings-Admin/discord.yml`

Beispiel:

```yaml
enabled: true
channels:
  staff: '123456789012345678'
  audit: '123456789012345678'
  events: '123456789012345678'
  status: '123456789012345678'
```

## 3. Bot-Token sicher setzen

Der Token kommt ausschliesslich aus der Umgebungsvariable:

`SKYKINGS_DISCORD_BOT_TOKEN`

Er gehoert NICHT in `discord.yml`, `server.properties`, GitHub oder Screenshots.

Unter Windows PowerShell fuer die aktuelle Sitzung:

```powershell
$env:SKYKINGS_DISCORD_BOT_TOKEN="DEIN_BOT_TOKEN"
```

Wenn der Minecraft-Server aus demselben PowerShell-Fenster gestartet wird, kann SkyKings den Token lesen.

## 4. Verbindung testen

Ingame mit Staff-Recht:

```text
/discordtest staff
/discordtest audit
/discordtest events
/discordtest status
```

Permission:

`skykings.admin.discord`

## Geplante SkyKings-Events fuer Discord

- Server Start/Stop
- Staff-/Audit-Logs
- auffaellige Economy-Transaktionen
- Voucher-/Crate-Einloesungen
- Duel-/LMS-/Tournament-Ergebnisse
- King Altar / KOTH
- Supply Drops
- grosse Killstreaks/Bounties
- Moderationsaktionen

Inbound Discord-Commands und Slash-Commands werden spaeter als getrennte Schicht gebaut, damit der Minecraft-Server nicht unnoetig Gateway-/Bot-Komplexitaet traegt.
