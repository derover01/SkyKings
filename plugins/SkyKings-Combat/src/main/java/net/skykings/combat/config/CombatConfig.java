package net.skykings.combat.config;

import net.skykings.combat.killstreak.KillstreakTier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Liest die gesamte Combat-Konfiguration aus config.yml (siehe Auftrag Phase 2, Abschnitt 2).
 * Alle Balancewerte sind hier zentral versammelt - kein Hardcoding in den Services.
 */
public final class CombatConfig {

    private final long combatTagMillis;
    private final long enderpearlCooldownMillis;
    private final long lootProtectionMillis;
    private final long newbieProtectionMillis;
    private final long baseNetherstarsPerKill;
    private final boolean starterKitEnabled;
    private final int starterKitGoldenApples;
    private final List<KillstreakTier> killstreakTiers;
    private final int antiFarmFullRewardMaxKills;
    private final int antiFarmHalfRewardMaxKills;
    private final double antiFarmHalfRewardMultiplier;
    private final long feedbackMessageCooldownMillis;

    public CombatConfig(JavaPlugin plugin) {
        this(loadConfig(plugin));
    }

    /** Sichtbar fuer Tests: erlaubt das Parsen einer {@link FileConfiguration} ohne echtes Plugin/Server (z. B. {@code YamlConfiguration}). */
    CombatConfig(FileConfiguration config) {
        this.combatTagMillis = TimeUnit.SECONDS.toMillis(config.getLong("combat-tag-seconds", 15));
        this.enderpearlCooldownMillis = TimeUnit.SECONDS.toMillis(config.getLong("enderpearl-cooldown-seconds", 3));
        this.lootProtectionMillis = TimeUnit.SECONDS.toMillis(config.getLong("loot-protection-seconds", 5));
        this.newbieProtectionMillis = TimeUnit.MINUTES.toMillis(config.getLong("newbie-protection-minutes", 20));
        this.baseNetherstarsPerKill = config.getLong("base-netherstars-per-kill", 1);
        this.starterKitEnabled = config.getBoolean("starter-kit.enabled", true);
        this.starterKitGoldenApples = config.getInt("starter-kit.golden-apples", 8);
        this.killstreakTiers = Collections.unmodifiableList(parseTiers(config.getMapList("killstreak.tiers")));
        this.antiFarmFullRewardMaxKills = config.getInt("anti-farm.full-reward-max-kills", 5);
        this.antiFarmHalfRewardMaxKills = config.getInt("anti-farm.half-reward-max-kills", 6);
        this.antiFarmHalfRewardMultiplier = config.getDouble("anti-farm.half-reward-multiplier", 0.5);
        this.feedbackMessageCooldownMillis = TimeUnit.SECONDS.toMillis(config.getLong("feedback.message-cooldown-seconds", 2));
    }

    private static FileConfiguration loadConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        return plugin.getConfig();
    }

    private static List<KillstreakTier> parseTiers(List<Map<?, ?>> raw) {
        List<KillstreakTier> tiers = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            int threshold = toInt(entry.get("threshold"));
            long perKill = toLong(entry.get("per-kill"));
            long milestoneBonus = toLong(entry.get("milestone-bonus"));
            tiers.add(new KillstreakTier(threshold, perKill, milestoneBonus));
        }
        return tiers;
    }

    private static int toInt(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : Integer.parseInt(String.valueOf(raw));
    }

    private static long toLong(Object raw) {
        return raw instanceof Number ? ((Number) raw).longValue() : Long.parseLong(String.valueOf(raw));
    }

    public long getCombatTagMillis() {
        return combatTagMillis;
    }

    public long getEnderpearlCooldownMillis() {
        return enderpearlCooldownMillis;
    }

    public long getLootProtectionMillis() {
        return lootProtectionMillis;
    }

    public long getNewbieProtectionMillis() {
        return newbieProtectionMillis;
    }

    public long getBaseNetherstarsPerKill() {
        return baseNetherstarsPerKill;
    }

    public boolean isStarterKitEnabled() {
        return starterKitEnabled;
    }

    public int getStarterKitGoldenApples() {
        return starterKitGoldenApples;
    }

    public List<KillstreakTier> getKillstreakTiers() {
        return killstreakTiers;
    }

    public int getAntiFarmFullRewardMaxKills() {
        return antiFarmFullRewardMaxKills;
    }

    public int getAntiFarmHalfRewardMaxKills() {
        return antiFarmHalfRewardMaxKills;
    }

    public double getAntiFarmHalfRewardMultiplier() {
        return antiFarmHalfRewardMultiplier;
    }

    public long getFeedbackMessageCooldownMillis() {
        return feedbackMessageCooldownMillis;
    }
}
