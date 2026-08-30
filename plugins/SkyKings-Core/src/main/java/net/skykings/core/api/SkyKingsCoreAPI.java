package net.skykings.core.api;

import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.kit.KitGrantService;
import net.skykings.core.kit.KitRegistry;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.netherstar.NetherstarService;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.rank.RankService;

/**
 * Zugriffspunkt fuer andere SkyKings-Module (Combat, Crates, Admin).
 *
 * <p>Wird ueber Bukkits {@code ServicesManager} bereitgestellt (registriert von
 * {@code SkyKingsCore#onEnable}), analog zum etablierten Vault-Muster. Andere Module
 * benoetigen dafuer nur einen {@code depend: [SkyKings-Core]}-Eintrag in ihrer plugin.yml
 * und rufen {@code Bukkit.getServicesManager().getRegistration(SkyKingsCoreAPI.class)} auf -
 * es gibt keine statische Singleton-Zugriffsklasse.
 */
public interface SkyKingsCoreAPI {

    PlayerProfileService getPlayerProfileService();

    RankService getRankService();

    EconomyService getEconomyService();

    NetherstarService getNetherstarService();

    CooldownService getCooldownService();

    LoggingService getLoggingService();

    KitRegistry getKitRegistry();

    KitGrantService getKitGrantService();

    GuiManager getGuiManager();
}
