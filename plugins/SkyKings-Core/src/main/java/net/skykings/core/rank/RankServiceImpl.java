package net.skykings.core.rank;

import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.PlayerProfileService;

import java.util.Objects;
import java.util.UUID;

public final class RankServiceImpl implements RankService {

    private final PlayerProfileService profileService;
    private final LoggingService loggingService;
    private final PermissionBridge permissionBridge;

    public RankServiceImpl(PlayerProfileService profileService, LoggingService loggingService,
                            PermissionBridge permissionBridge) {
        this.profileService = profileService;
        this.loggingService = loggingService;
        this.permissionBridge = permissionBridge;
    }

    @Override
    public Rank getRank(UUID uuid) {
        return requireProfile(uuid).getRank();
    }

    @Override
    public void setRank(UUID uuid, Rank rank) {
        setRank(uuid, rank, "SYSTEM");
    }

    @Override
    public void setRank(UUID uuid, Rank rank, String actor) {
        Objects.requireNonNull(rank, "rank");
        PlayerProfile profile = requireProfile(uuid);
        Rank old;
        synchronized (profile) {
            old = profile.getRank();
            if (old == rank) {
                return;
            }
            profile.setRank(rank);
        }
        profileService.save(uuid);
        loggingService.logRankChange(uuid, old, rank, actor);
        permissionBridge.syncRank(uuid, rank);
    }

    @Override
    public boolean hasAtLeast(UUID uuid, Rank rank) {
        return requireProfile(uuid).getRank().isAtLeast(rank);
    }

    private PlayerProfile requireProfile(UUID uuid) {
        PlayerProfile profile = profileService.getCached(uuid);
        if (profile == null) {
            throw new IllegalStateException("Kein geladenes PlayerProfile fuer " + uuid + " (Spieler online?).");
        }
        return profile;
    }
}
