package net.skykings.core.clan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistente Oldschool-Clans mit Owner, Mitgliedern, Invites und Friendly-Fire-Schutz. */
public final class ClanService implements Listener {
    public static final int MAX_MEMBERS = 12;

    public static final class Clan {
        private final UUID id;
        private String name;
        private String tag;
        private UUID owner;
        private final Set<UUID> members = new HashSet<UUID>();

        Clan(UUID id, String name, String tag, UUID owner) {
            this.id = id; this.name = name; this.tag = tag; this.owner = owner;
            this.members.add(owner);
        }
        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getTag() { return tag; }
        public UUID getOwner() { return owner; }
        public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
        public boolean isOwner(UUID uuid) { return owner.equals(uuid); }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Clan> clans = new LinkedHashMap<UUID, Clan>();
    private final Map<UUID, UUID> memberToClan = new HashMap<UUID, UUID>();
    private final Map<UUID, UUID> invites = new HashMap<UUID, UUID>();

    public ClanService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    public synchronized Clan create(Player owner, String nameRaw, String tagRaw) {
        if (owner == null || getClan(owner.getUniqueId()) != null) return null;
        String name = cleanName(nameRaw);
        String tag = cleanTag(tagRaw);
        if (name == null || tag == null || nameTaken(name) || tagTaken(tag)) return null;
        Clan clan = new Clan(UUID.randomUUID(), name, tag, owner.getUniqueId());
        clans.put(clan.id, clan);
        memberToClan.put(owner.getUniqueId(), clan.id);
        save();
        return clan;
    }

    public synchronized Clan getClan(UUID member) {
        UUID clanId = memberToClan.get(member);
        return clanId == null ? null : clans.get(clanId);
    }

    public synchronized boolean invite(UUID owner, UUID target) {
        Clan clan = getClan(owner);
        if (clan == null || !clan.isOwner(owner) || getClan(target) != null || clan.members.size() >= MAX_MEMBERS) return false;
        invites.put(target, clan.id);
        return true;
    }

    public synchronized Clan pendingInvite(UUID target) {
        UUID id = invites.get(target);
        return id == null ? null : clans.get(id);
    }

    public synchronized Clan accept(UUID target) {
        UUID id = invites.remove(target);
        Clan clan = id == null ? null : clans.get(id);
        if (clan == null || getClan(target) != null || clan.members.size() >= MAX_MEMBERS) return null;
        clan.members.add(target);
        memberToClan.put(target, clan.id);
        save();
        return clan;
    }

    public synchronized void deny(UUID target) { invites.remove(target); }

    public synchronized boolean kick(UUID owner, UUID target) {
        Clan clan = getClan(owner);
        if (clan == null || !clan.isOwner(owner) || owner.equals(target) || !clan.members.remove(target)) return false;
        memberToClan.remove(target);
        save();
        return true;
    }

    public synchronized boolean leave(UUID member) {
        Clan clan = getClan(member);
        if (clan == null || clan.isOwner(member)) return false;
        clan.members.remove(member);
        memberToClan.remove(member);
        save();
        return true;
    }

    public synchronized boolean disband(UUID owner) {
        Clan clan = getClan(owner);
        if (clan == null || !clan.isOwner(owner)) return false;
        clans.remove(clan.id);
        for (UUID member : new ArrayList<UUID>(clan.members)) memberToClan.remove(member);
        invites.values().removeIf(id -> clan.id.equals(id));
        save();
        return true;
    }

    public synchronized boolean sameClan(UUID a, UUID b) {
        Clan ca = getClan(a), cb = getClan(b);
        return ca != null && cb != null && ca.id.equals(cb.id);
    }

    public synchronized List<Clan> all() { return new ArrayList<Clan>(clans.values()); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = resolve(event.getDamager());
        if (attacker == null || attacker.equals(victim) || !sameClan(attacker.getUniqueId(), victim.getUniqueId())) return;
        event.setCancelled(true);
        attacker.sendMessage(ChatColor.RED + "Friendly Fire ist im Clan deaktiviert. " + ChatColor.GRAY + "Du kannst " + victim.getName() + " nicht angreifen.");
        attacker.playSound(attacker.getLocation(), Sound.CLICK, 0.45F, 0.6F);
    }

    private Player resolve(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            Object shooter = ((Projectile) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    private String cleanName(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.matches("[A-Za-z0-9_-]{3,16}") ? s : null;
    }
    private String cleanTag(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return s.matches("[A-Z0-9]{2,5}") ? s : null;
    }
    private boolean nameTaken(String name) {
        for (Clan clan : clans.values()) if (clan.name.equalsIgnoreCase(name)) return true;
        return false;
    }
    private boolean tagTaken(String tag) {
        for (Clan clan : clans.values()) if (clan.tag.equalsIgnoreCase(tag)) return true;
        return false;
    }

    private void load() {
        clans.clear(); memberToClan.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("clans");
        if (root == null) return;
        for (String raw : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(raw);
                String base = "clans." + raw + ".";
                String name = yaml.getString(base + "name", "Clan");
                String tag = yaml.getString(base + "tag", "SKY");
                UUID owner = UUID.fromString(yaml.getString(base + "owner"));
                Clan clan = new Clan(id, name, tag, owner);
                clan.members.clear();
                for (String memberRaw : yaml.getStringList(base + "members")) {
                    try { clan.members.add(UUID.fromString(memberRaw)); } catch (IllegalArgumentException ignored) { }
                }
                clan.members.add(owner);
                clans.put(id, clan);
                for (UUID member : clan.members) memberToClan.put(member, id);
            } catch (Exception ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Clan clan : clans.values()) {
            String base = "clans." + clan.id + ".";
            yaml.set(base + "name", clan.name);
            yaml.set(base + "tag", clan.tag);
            yaml.set(base + "owner", clan.owner.toString());
            List<String> members = new ArrayList<String>();
            for (UUID member : clan.members) members.add(member.toString());
            yaml.set(base + "members", members);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("clans.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
