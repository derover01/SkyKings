package net.skykings.core.economy;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;

import java.util.UUID;

public final class EconomyServiceImpl implements EconomyService {

    private final PlayerProfileService profileService;
    private final LoggingService loggingService;

    public EconomyServiceImpl(PlayerProfileService profileService, LoggingService loggingService) {
        this.profileService = profileService;
        this.loggingService = loggingService;
    }

    @Override
    public long getBalance(UUID uuid) {
        return requireProfile(uuid).getCoins();
    }

    @Override
    public boolean has(UUID uuid, long amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public void setBalance(UUID uuid, long amount, String actor, String reason) {
        // Kein Overflow-Risiko: amount wird direkt als neuer Kontostand uebernommen, es findet
        // keine Addition zweier Werte statt.
        if (amount < 0) {
            throw new IllegalArgumentException("Kontostand darf nicht negativ sein: " + amount);
        }
        PlayerProfile profile = requireProfile(uuid);
        long old;
        synchronized (profile) {
            old = profile.getCoins();
            profile.setCoins(amount);
        }
        profileService.save(uuid);
        loggingService.logEconomySet(uuid, old, amount, actor, reason);
    }

    @Override
    public void deposit(UUID uuid, long amount, String actor, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Einzahlungsbetrag muss positiv sein: " + amount);
        }
        PlayerProfile profile = requireProfile(uuid);
        long newBalance;
        synchronized (profile) {
            try {
                newBalance = Math.addExact(profile.getCoins(), amount);
            } catch (ArithmeticException e) {
                // Profil bewusst NICHT veraendert und KEIN Audit-Event geschrieben - die Transaktion
                // ist fehlgeschlagen, bevor irgendein Zustand mutiert wurde.
                throw new EconomyOverflowException("Einzahlung wuerde den gueltigen Wertebereich ueberschreiten: "
                        + "uuid=" + uuid + ", aktuellerKontostand=" + profile.getCoins() + ", betrag=" + amount, e);
            }
            profile.setCoins(newBalance);
        }
        profileService.save(uuid);
        loggingService.logEconomyDeposit(uuid, amount, newBalance, actor, reason);
    }

    @Override
    public boolean withdraw(UUID uuid, long amount, String actor, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Auszahlungsbetrag muss positiv sein: " + amount);
        }
        PlayerProfile profile = requireProfile(uuid);
        long newBalance;
        synchronized (profile) {
            // Kein Overflow-Risiko: amount ist hier immer positiv und durch die Pruefung direkt
            // darunter <= profile.getCoins() (>= 0), das Ergebnis liegt also stets in [0, coins].
            if (profile.getCoins() < amount) {
                return false;
            }
            newBalance = profile.getCoins() - amount;
            profile.setCoins(newBalance);
        }
        profileService.save(uuid);
        loggingService.logEconomyWithdraw(uuid, amount, newBalance, actor, reason);
        return true;
    }

    private PlayerProfile requireProfile(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) {
            throw new IllegalStateException("Kein geladenes PlayerProfile fuer " + uuid + " (Spieler online?).");
        }
        return profile;
    }
}
