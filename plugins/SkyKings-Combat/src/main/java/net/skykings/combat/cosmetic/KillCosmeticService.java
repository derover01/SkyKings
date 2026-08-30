package net.skykings.combat.cosmetic;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Persistente Auswahl rein kosmetischer Kill-Effects. */
public final class KillCosmeticService {

    public enum KillEffect {
        NONE("Kein Effekt", null),
        LIGHTNING("Blitz", "skykings.killeffect.lightning"),
        FLAME("Flammen", "skykings.killeffect.flame"),
        HEART("Herzen", "skykings.killeffect.heart"),
        ENDER("Ender", "skykings.killeffect.ender");

        private final String displayName;
        private final String permission;

        KillEffect(String displayName, String permission) {
            this.displayName = displayName;
            this.permission = permission;
        }

        public String getDisplayName() { return displayName; }
        public String getPermission() { return permission; }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, KillEffect> selected = new ConcurrentHashMap<UUID, KillEffect>();
    private final ExecutorService writer;

    public KillCosmeticService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kill-effects.yml");
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SkyKings-KillEffects");
            thread.setDaemon(true);
            return thread;
        });
        load();
    }

    public KillEffect getSelected(UUID uuid) {
        KillEffect effect = selected.get(uuid);
        return effect == null ? KillEffect.NONE : effect;
    }

    public boolean canUse(Player player, KillEffect effect) {
        return effect == KillEffect.NONE || player.hasPermission(effect.getPermission());
    }

    public boolean select(Player player, KillEffect effect) {
        if (!canUse(player, effect)) return false;
        if (effect == KillEffect.NONE) selected.remove(player.getUniqueId());
        else selected.put(player.getUniqueId(), effect);
        saveAsync();
        return true;
    }

    public void play(Player killer, Location location) {
        KillEffect effect = getSelected(killer.getUniqueId());
        if (effect == KillEffect.NONE || !canUse(killer, effect) || location == null || location.getWorld() == null) return;
        switch (effect) {
            case LIGHTNING:
                location.getWorld().strikeLightningEffect(location);
                break;
            case FLAME:
                for (int i = 0; i < 4; i++) location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 0);
                break;
            case HEART:
                for (int i = 0; i < 4; i++) location.getWorld().playEffect(location, Effect.HEART, 0);
                break;
            case ENDER:
                location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
                break;
            default:
                break;
        }
    }

    public void shutdown() {
        Map<UUID, KillEffect> snapshot = new HashMap<UUID, KillEffect>(selected);
        writer.submit(() -> save(snapshot));
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Kill-Effect-Writer wurde nicht rechtzeitig beendet.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) return;
        for (String rawUuid : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                String raw = root.getString(rawUuid, "NONE");
                KillEffect effect = KillEffect.valueOf(raw.toUpperCase(Locale.ROOT));
                if (effect != KillEffect.NONE) selected.put(uuid, effect);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void saveAsync() {
        final Map<UUID, KillEffect> snapshot = new HashMap<UUID, KillEffect>(selected);
        writer.submit(() -> save(snapshot));
    }

    private void save(Map<UUID, KillEffect> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, KillEffect> entry : snapshot.entrySet()) {
            yaml.set("players." + entry.getKey().toString(), entry.getValue().name());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Kill-Effect-Auswahl konnte nicht gespeichert werden.", ex);
        }
    }
}
