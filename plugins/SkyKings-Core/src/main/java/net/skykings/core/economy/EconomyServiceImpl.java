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
        if (amount < 0) {
            throw new IllegalArgumentException("Kontostand darf nicht negativ sein: " + amount);
        }
        // Administrative Korrekturen duerfen auch bekannte, aktuell ausgeloggte Spieler treffen.
        // Unbekannte UUIDs erzeugen weiterhin niemals automatisch ein neues Profil.
        PlayerProfile profile = requireExistingProfile(uuid);
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
        // Serverseitige Credits (z. B. Jackpot, Admin-Credits oder Shop-Recovery) muessen auch
        // einen Spieler erreichen, der ausgeloggt hat. Nur bereits persistierte Profile werden geladen.
        PlayerProfile profile = requireExistingProfile(uuid);
        long newBalance;
        synchronized (profile) {
            try {
                newBalance = Math.addExact(profile.getCoins(), amount);
            } catch (ArithmeticException e) {
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

    private PlayerProfile requireExistingProfile(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) profile = profileService.loadExisting(uuid);
        if (profile == null) {
            throw new IllegalStateException("Kein bestehendes PlayerProfile fuer " + uuid + ".");
        }
        return profile;
    }
}
