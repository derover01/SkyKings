package net.skykings.combat.newbie;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.profile.PlayerProfileService;

import java.util.UUID;

public final class NewbieProtectionServiceImpl implements NewbieProtectionService {

    private final PlayerProfileService profileService;
    private final long protectionDurationMillis;

    public NewbieProtectionServiceImpl(PlayerProfileService profileService, long protectionDurationMillis) {
        if (protectionDurationMillis <= 0) {
            throw new IllegalArgumentException("protectionDurationMillis muss positiv sein: " + protectionDurationMillis);
        }
        this.profileService = profileService;
        this.protectionDurationMillis = protectionDurationMillis;
    }

    @Override
    public boolean isProtected(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) {
            return false;
        }
        if (profile.isNewbieProtectionDisabled()) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - profile.getCreatedAt();
        return elapsed >= 0 && elapsed < protectionDurationMillis;
    }

    @Override
    public void disableProtection(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) {
            return;
        }
        synchronized (profile) {
            if (profile.isNewbieProtectionDisabled()) {
                return;
            }
            profile.setNewbieProtectionDisabled(true);
        }
        profileService.save(uuid);
    }
}
