package net.skykings.combat.killstreak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class KillstreakServiceImpl implements KillstreakService {

    private final Map<UUID, AtomicInteger> streaks = new ConcurrentHashMap<>();
    private final List<KillstreakTier> tiers;

    /**
     * @param baseNetherstarsPerKill Reward fuer Streak 0-4 (config-Key {@code base-netherstars-per-kill}).
     * @param additionalTiers        weitere Stufen ab Streak &gt; 0 (config-Key {@code killstreak.tiers}),
     *                               muessen keinen threshold=0-Eintrag enthalten - der wird aus
     *                               {@code baseNetherstarsPerKill} synthetisiert.
     */
    public KillstreakServiceImpl(long baseNetherstarsPerKill, List<KillstreakTier> additionalTiers) {
        List<KillstreakTier> all = new ArrayList<>();
        all.add(new KillstreakTier(0, baseNetherstarsPerKill, 0));
        all.addAll(additionalTiers);
        all.sort(Comparator.comparingInt(KillstreakTier::getThreshold));
        this.tiers = Collections.unmodifiableList(all);
    }

    @Override
    public int getStreak(UUID uuid) {
        AtomicInteger streak = streaks.get(uuid);
        return streak == null ? 0 : streak.get();
    }

    @Override
    public void reset(UUID uuid) {
        streaks.remove(uuid);
    }

    @Override
    public KillstreakResult recordKill(UUID killerUuid) {
        int newStreak = streaks.computeIfAbsent(killerUuid, u -> new AtomicInteger()).incrementAndGet();
        KillstreakTier applicable = applicableTier(newStreak);
        long milestoneBonus = applicable.getThreshold() == newStreak ? applicable.getMilestoneBonus() : 0L;
        return new KillstreakResult(newStreak, applicable.getPerKill(), milestoneBonus);
    }

    private KillstreakTier applicableTier(int streak) {
        KillstreakTier applicable = tiers.get(0);
        for (KillstreakTier tier : tiers) {
            if (tier.getThreshold() <= streak) {
                applicable = tier;
            } else {
                break;
            }
        }
        return applicable;
    }
}
