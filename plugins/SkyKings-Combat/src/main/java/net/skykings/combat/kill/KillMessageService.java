package net.skykings.combat.kill;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Persistente, auswählbare SkyKings-Death-Message-Cosmetics für normale PvP-Kills. */
public final class KillMessageService {

    public enum MessageStyle {
        CLASSIC("Classic", null, ChatColor.GRAY),
        ROYAL("Royal", "skykings.deathmessage.royal", ChatColor.GOLD),
        VOID("Void", "skykings.deathmessage.void", ChatColor.DARK_PURPLE),
        HUNTER("Hunter", "skykings.deathmessage.hunter", ChatColor.DARK_AQUA),
        LEGEND("Legend", "skykings.deathmessage.legend", ChatColor.LIGHT_PURPLE);

        private final String displayName;
        private final String permission;
        private final ChatColor color;

        MessageStyle(String displayName, String permission, ChatColor color) {
            this.displayName = displayName;
            this.permission = permission;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getPermission() { return permission; }
        public ChatColor getColor() { return color; }
    }

    private static volatile KillMessageService liveInstance;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, MessageStyle> selected = new HashMap<UUID, MessageStyle>();
    private final Map<MessageStyle, String[]> templates = new EnumMap<MessageStyle, String[]>(MessageStyle.class);

    public KillMessageService() {
        this.plugin = JavaPlugin.getProvidingPlugin(KillMessageService.class);
        this.file = new File(plugin.getDataFolder(), "death-messages.yml");
        registerTemplates();
        load();
        liveInstance = this;
    }

    public static KillMessageService liveInstance() {
        return liveInstance;
    }

    public MessageStyle getSelected(UUID uuid) {
        MessageStyle style = selected.get(uuid);
        return style == null ? MessageStyle.CLASSIC : style;
    }

    public boolean canUse(Player player, MessageStyle style) {
        return style == MessageStyle.CLASSIC || style.getPermission() == null || player.hasPermission(style.getPermission());
    }

    public synchronized boolean select(Player player, MessageStyle style) {
        if (player == null || style == null || !canUse(player, style)) return false;
        selected.put(player.getUniqueId(), style);
        save();
        return true;
    }

    public String create(Player killer, Player victim) {
        MessageStyle style = getSelected(killer.getUniqueId());
        if (!canUse(killer, style)) style = MessageStyle.CLASSIC;
        String[] choices = templates.get(style);
        if (choices == null || choices.length == 0) choices = templates.get(MessageStyle.CLASSIC);
        String template = choices[ThreadLocalRandom.current().nextInt(choices.length)];
        return ChatColor.DARK_GRAY + "[" + style.getColor() + "⚔" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GRAY + template
                .replace("%killer%", style.getColor() + killer.getName() + ChatColor.GRAY)
                .replace("%victim%", ChatColor.WHITE + victim.getName() + ChatColor.GRAY);
    }

    private void registerTemplates() {
        templates.put(MessageStyle.CLASSIC, new String[] {
                "%killer% hat %victim% aus dem Himmel geschickt.",
                "%victim% konnte %killer% nicht entkommen.",
                "%killer% hat %victim% im SkyPvP zerlegt.",
                "%victim% wurde von %killer% ausgeschaltet."
        });
        templates.put(MessageStyle.ROYAL, new String[] {
                "%killer% hat %victim% vom Thron gestoßen.",
                "%victim% kniet vor %killer%.",
                "%killer% beansprucht die Krone gegen %victim%."
        });
        templates.put(MessageStyle.VOID, new String[] {
                "%victim% verschwand durch %killer% im Void.",
                "%killer% schickte %victim% in die Leere.",
                "Der Himmel nahm %victim% nach dem Treffer von %killer%."
        });
        templates.put(MessageStyle.HUNTER, new String[] {
                "%killer% hat die Jagd auf %victim% beendet.",
                "%victim% wurde zur Beute von %killer%.",
                "%killer% ließ %victim% keine Fluchtroute."
        });
        templates.put(MessageStyle.LEGEND, new String[] {
                "%killer% schrieb %victim% in die SkyKings-Legende ein.",
                "%victim% fiel vor der Legende %killer%.",
                "%killer% machte aus %victim% Geschichte."
        });
    }

    private synchronized void load() {
        selected.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("players") == null) return;
        for (String rawUuid : yaml.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                MessageStyle style = MessageStyle.valueOf(yaml.getString("players." + rawUuid, "CLASSIC").toUpperCase());
                selected.put(uuid, style);
            } catch (Exception ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, MessageStyle> entry : selected.entrySet()) {
            yaml.set("players." + entry.getKey().toString(), entry.getValue().name());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("death-messages.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
