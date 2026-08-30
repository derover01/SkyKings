package net.skykings.core.integration.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.skykings.core.economy.EconomyOverflowException;
import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registriert SkyKings-Coins als klassischen Vault/VaultUnlocked-Economy-Provider.
 *
 * <p>{@link EconomyService} bleibt die alleinige Source of Truth - diese Klasse ist nur ein
 * duenner Adapter fuer Fremdplugins, die die klassische {@code net.milkbowl.vault.economy.
 * Economy}-API erwarten. Es wird KEINE zweite Waehrung eingefuehrt: jede Methode delegiert auf
 * {@link EconomyService}.
 *
 * <p>Implementiert {@code Economy} direkt statt {@code AbstractEconomy} zu erweitern, damit
 * jede Methode explizit und nachvollziehbar ist (keine impliziten Annahmen darueber, was eine
 * Basisklasse bereits "fuer uns erledigt").
 *
 * <p><b>Offline-Spieler (Phase 1B Grenze):</b> {@link EconomyService} arbeitet nur mit online
 * geladenen Profilen. Fuer Spieler, die aktuell nicht online sind, gibt es keine sichere
 * Datenquelle - anstatt erfundene Werte zurueckzugeben, melden alle Methoden fuer offline
 * Spieler explizit "keine Aktion moeglich" (false / 0.0 / {@code EconomyResponse.FAILURE} mit
 * klarer Fehlermeldung). Dies ist ein bewusster, dokumentierter Interims-Zustand (siehe
 * Abschlussbericht, offene Punkte) - kein Datenverlust, nur (noch) keine Offline-Unterstuetzung.
 *
 * <p>Pro-Welt-Economy wird nicht unterstuetzt; alle {@code world}-Parameter werden ignoriert.
 * Bank-Accounts werden nicht unterstuetzt ({@link #hasBankSupport()} ist {@code false}).
 */
public final class SkyKingsVaultEconomyProvider implements Economy {

    private final EconomyService economyService;
    private final Logger logger;
    private final Predicate<UUID> onlineCheck;

    public SkyKingsVaultEconomyProvider(EconomyService economyService, Logger logger) {
        this(economyService, logger, uuid -> Bukkit.getPlayer(uuid) != null);
    }

    /** Sichtbar fuer Tests: erlaubt, den Bukkit-Online-Check durch ein Test-Double zu ersetzen. */
    SkyKingsVaultEconomyProvider(EconomyService economyService, Logger logger, Predicate<UUID> onlineCheck) {
        this.economyService = economyService;
        this.logger = logger;
        this.onlineCheck = onlineCheck;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "SkyKings";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        return Math.round(amount) + " Coins";
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return isOnline(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!isOnline(player)) {
            return 0.0d;
        }
        try {
            return economyService.getBalance(player.getUniqueId());
        } catch (IllegalStateException e) {
            return 0.0d;
        }
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!isOnline(player) || amount < 0) {
            return false;
        }
        try {
            return economyService.has(player.getUniqueId(), Math.round(amount));
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (!isOnline(player)) {
            return offlineFailure();
        }
        if (amount < 0) {
            return negativeAmountFailure(player);
        }
        long whole = Math.round(amount);
        UUID uuid = player.getUniqueId();
        try {
            if (whole == 0) {
                return new EconomyResponse(0, economyService.getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, null);
            }
            boolean success = economyService.withdraw(uuid, whole, "VAULT", "Vault-Economy-API");
            double newBalance = economyService.getBalance(uuid);
            if (!success) {
                return new EconomyResponse(0, newBalance, EconomyResponse.ResponseType.FAILURE, "Nicht genug Coins.");
            }
            return new EconomyResponse(whole, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        } catch (IllegalStateException e) {
            return offlineFailure();
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (!isOnline(player)) {
            return offlineFailure();
        }
        if (amount < 0) {
            return negativeAmountFailure(player);
        }
        long whole = Math.round(amount);
        UUID uuid = player.getUniqueId();
        try {
            if (whole == 0) {
                return new EconomyResponse(0, economyService.getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, null);
            }
            economyService.deposit(uuid, whole, "VAULT", "Vault-Economy-API");
            return new EconomyResponse(whole, economyService.getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, null);
        } catch (EconomyOverflowException e) {
            logger.log(Level.WARNING, "Vault-Einzahlung abgelehnt (Kontostand-Obergrenze erreicht) fuer " + uuid, e);
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE,
                    "Kontostand-Obergrenze erreicht.");
        } catch (IllegalStateException e) {
            return offlineFailure();
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        // Ein Profil wird bereits automatisch beim Login angelegt (siehe PlayerLifecycleListener).
        // Eine eigenstaendige Offline-Account-Erstellung ist in Phase 1B nicht vorgesehen.
        return isOnline(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    // --- Bank-Accounts werden bewusst nicht unterstuetzt (siehe hasBankSupport()) ---

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    private boolean isOnline(OfflinePlayer player) {
        return player != null && onlineCheck.test(player.getUniqueId());
    }

    private EconomyResponse offlineFailure() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE,
                "Spieler ist nicht online - SkyKings-Economy unterstuetzt in dieser Phase nur online Spieler.");
    }

    private EconomyResponse negativeAmountFailure(OfflinePlayer player) {
        return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE,
                "Betrag darf nicht negativ sein.");
    }

    private EconomyResponse notImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "SkyKings unterstuetzt keine Bank-Accounts.");
    }
}
