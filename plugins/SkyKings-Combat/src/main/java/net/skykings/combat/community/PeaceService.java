package net.skykings.combat.community;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Gegenseitige Peace-Paare; niemals einseitiger PvP-Schutz. */
public final class PeaceService implements Listener {
    private final JavaPlugin plugin;
    private final File file;
    private final Set<String> pairs = new HashSet<String>();

    public PeaceService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "peace.yml");
        load();
    }

    public boolean add(UUID a, UUID b) {
        if (a == null || b == null || a.equals(b)) return false;
        boolean changed = pairs.add(key(a, b));
        if (changed) save();
        return changed;
    }

    public boolean remove(UUID a, UUID b) {
        boolean changed = pairs.remove(key(a, b));
        if (changed) save();
        return changed;
    }

    public boolean isPeace(UUID a, UUID b) { return pairs.contains(key(a, b)); }
    public int countFor(UUID uuid) { return partners(uuid).size(); }

    public List<UUID> partners(UUID uuid) {
        List<UUID> out = new ArrayList<UUID>();
        String id = uuid.toString();
        for (String pair : pairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;
            try {
                if (parts[0].equals(id)) out.add(UUID.fromString(parts[1]));
                else if (parts[1].equals(id)) out.add(UUID.fromString(parts[0]));
            } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    /** LOWEST ist absichtlich: Peace muss vor CombatTag/Newbie-Logik canceln. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || !isPeace(attacker.getUniqueId(), victim.getUniqueId())) return;
        event.setCancelled(true);
        attacker.sendMessage(ChatColor.YELLOW + "Du hast mit " + victim.getName() + " Frieden. " + ChatColor.GRAY + "/friede remove " + victim.getName());
    }

    private Player resolvePlayer(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof org.bukkit.entity.Projectile) {
            Object shooter = ((org.bukkit.entity.Projectile) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    private String key(UUID a, UUID b) {
        String x = a.toString(), y = b.toString();
        return x.compareTo(y) <= 0 ? x + ":" + y : y + ":" + x;
    }

    private void load() {
        if (!file.exists()) return;
        pairs.addAll(YamlConfiguration.loadConfiguration(file).getStringList("pairs"));
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pairs", new ArrayList<String>(pairs));
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("peace.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
