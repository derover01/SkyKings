package net.skykings.core.netherstar;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;

import java.util.UUID;

public final class NetherstarServiceImpl implements NetherstarService {

    private final PlayerProfileService profileService;
    private final LoggingService loggingService;

    public NetherstarServiceImpl(PlayerProfileService profileService, LoggingService loggingService) {
        this.profileService = profileService;
        this.loggingService = loggingService;
    }

    @Override
    public long getBalance(UUID uuid) {
        return requireProfile(uuid).getNetherstars();
    }

    @Override
    public boolean has(UUID uuid, long amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public void setBalance(UUID uuid, long amount, String actor, String reason) {
        // Kein Overflow-Risiko: amount wird direkt als neuer Kontostand uebernommen (keine Addition).
        if (amount < 0) {
            throw new IllegalArgumentException("Netherstern-Kontostand darf nicht negativ sein: " + amount);
        }
        PlayerProfile profile = requireProfile(uuid);
        long old;
        synchronized (profile) {
            old = profile.getNetherstars();
            profile.setNetherstars(amount);
        }
        profileService.save(uuid);
        loggingService.logNetherstarSet(uuid, old, amount, actor, reason);
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
                newBalance = Math.addExact(profile.getNetherstars(), amount);
            } catch (ArithmeticException e) {
                // Profil bewusst NICHT veraendert und KEIN Audit-Event geschrieben.
                throw new NetherstarOverflowException("Einzahlung wuerde den gueltigen Wertebereich ueberschreiten: "
                        + "uuid=" + uuid + ", aktuellerKontostand=" + profile.getNetherstars() + ", betrag=" + amount, e);
            }
            profile.setNetherstars(newBalance);
        }
        profileService.save(uuid);
        loggingService.logNetherstarDeposit(uuid, amount, newBalance, actor, reason);
    }

    @Override
    public boolean withdraw(UUID uuid, long amount, String actor, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Auszahlungsbetrag muss positiv sein: " + amount);
        }
        PlayerProfile profile = requireProfile(uuid);
        long newBalance;
        synchronized (profile) {
            // Kein Overflow-Risiko: amount ist positiv und <= profile.getNetherstars() (>= 0).
            if (profile.getNetherstars() < amount) {
                return false;
            }
            newBalance = profile.getNetherstars() - amount;
            profile.setNetherstars(newBalance);
        }
        profileService.save(uuid);
        loggingService.logNetherstarWithdraw(uuid, amount, newBalance, actor, reason);
        return true;
    }

    @Override
    public boolean persistNow(UUID uuid) {
        return profileService.saveNow(uuid);
    }

    private PlayerProfile requireProfile(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) {
            throw new IllegalStateException("Kein geladenes PlayerProfile fuer " + uuid + " (Spieler online?).");
        }
        return profile;
    }
}
