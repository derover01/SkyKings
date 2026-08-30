package net.skykings.core.integration.vault;

import net.milkbowl.vault.economy.Economy;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.integration.EconomyBridge;
import net.skykings.core.integration.NoOpEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.logging.Logger;

/**
 * Registriert {@link SkyKingsVaultEconomyProvider} als Vault/VaultUnlocked-Economy-Provider.
 *
 * <p>Referenziert Vault-API-Typen nur in dieser Klasse (plus {@link SkyKingsVaultEconomyProvider}),
 * damit ein fehlendes Vault/VaultUnlocked nie zu einer harten {@code ClassNotFoundException}/
 * {@code NoClassDefFoundError} beim Laden von {@code SkyKingsCore} fuehrt - siehe
 * {@link #createAndRegister(Plugin, EconomyService, Logger)}, das in {@code SkyKingsCore}
 * innerhalb eines try/catch(Throwable) aufgerufen wird.
 */
public final class VaultEconomyBridge implements EconomyBridge {

    /** Registriert SkyKings als Vault-Economy-Provider, falls Vault/VaultUnlocked verfuegbar ist. */
    public static EconomyBridge createAndRegister(Plugin plugin, EconomyService economyService, Logger logger) {
        SkyKingsVaultEconomyProvider provider = new SkyKingsVaultEconomyProvider(economyService, logger);
        Bukkit.getServicesManager().register(Economy.class, provider, plugin, ServicePriority.Highest);
        return new VaultEconomyBridge();
    }

    private VaultEconomyBridge() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isRegistered() {
        return true;
    }
}
