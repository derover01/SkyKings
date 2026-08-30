package net.skykings.combat.config;

import net.skykings.combat.killstreak.KillstreakTier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Parst die tatsaechlich ausgelieferte {@code config.yml} ueber eine echte, server-unabhaengige
 * {@link YamlConfiguration} (kein Mock-Server noetig) und prueft die dokumentierten Defaults.
 */
public class CombatConfigTest {

    private CombatConfig loadShippedConfig() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            assertTrue("config.yml Resource nicht gefunden", in != null);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(in);
            return new CombatConfig(yaml);
        }
    }

    @Test
    public void shippedConfigMatchesDocumentedDefaults() throws Exception {
        CombatConfig config = loadShippedConfig();

        assertEquals(TimeUnit.SECONDS.toMillis(15), config.getCombatTagMillis());
        assertEquals(TimeUnit.SECONDS.toMillis(3), config.getEnderpearlCooldownMillis());
        assertEquals(TimeUnit.SECONDS.toMillis(5), config.getLootProtectionMillis());
        assertEquals(TimeUnit.MINUTES.toMillis(20), config.getNewbieProtectionMillis());
        assertEquals(1L, config.getBaseNetherstarsPerKill());
        assertTrue(config.isStarterKitEnabled());
        assertEquals(8, config.getStarterKitGoldenApples());
        assertEquals(5, config.getAntiFarmFullRewardMaxKills());
        assertEquals(6, config.getAntiFarmHalfRewardMaxKills());
        assertEquals(0.5, config.getAntiFarmHalfRewardMultiplier(), 0.0001);
        assertEquals(TimeUnit.SECONDS.toMillis(2), config.getFeedbackMessageCooldownMillis());
    }

    @Test
    public void shippedConfigDefinesAllSixKillstreakTiers() throws Exception {
        CombatConfig config = loadShippedConfig();

        List<KillstreakTier> tiers = config.getKillstreakTiers();
        assertEquals(6, tiers.size());
        assertEquals(5, tiers.get(0).getThreshold());
        assertEquals(2L, tiers.get(0).getPerKill());
        assertEquals(3L, tiers.get(0).getMilestoneBonus());
        assertEquals(100, tiers.get(5).getThreshold());
        assertEquals(5L, tiers.get(5).getPerKill());
        assertEquals(125L, tiers.get(5).getMilestoneBonus());
    }

    @Test
    public void missingValuesFallBackToSafeDefaults() {
        YamlConfiguration empty = new YamlConfiguration();
        CombatConfig config = new CombatConfig(empty);

        assertEquals(TimeUnit.SECONDS.toMillis(15), config.getCombatTagMillis());
        assertTrue(config.isStarterKitEnabled());
        assertTrue("Ohne konfigurierte Tiers darf die Liste nicht null sein", config.getKillstreakTiers().isEmpty());
    }
}
