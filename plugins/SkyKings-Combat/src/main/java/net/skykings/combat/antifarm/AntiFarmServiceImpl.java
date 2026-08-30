package net.skykings.combat.antifarm;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiFarmServiceImpl implements AntiFarmService {

    private static final class FarmState {
        private UUID lastVictim;
        private int consecutiveCount;
    }

    private final Map<UUID, FarmState> states = new ConcurrentHashMap<>();
    private final int fullRewardMaxKills;
    private final int halfRewardMaxKills;
    private final double halfRewardMultiplier;

    public AntiFarmServiceImpl(int fullRewardMaxKills, int halfRewardMaxKills, double halfRewardMultiplier) {
        if (fullRewardMaxKills < 0 || halfRewardMaxKills < fullRewardMaxKills) {
            throw new IllegalArgumentException("Ungueltige Anti-Farm-Konfiguration: fullRewardMaxKills="
                    + fullRewardMaxKills + ", halfRewardMaxKills=" + halfRewardMaxKills);
        }
        if (halfRewardMultiplier < 0.0 || halfRewardMultiplier > 1.0) {
            throw new IllegalArgumentException("halfRewardMultiplier muss zwischen 0.0 und 1.0 liegen: " + halfRewardMultiplier);
        }
        this.fullRewardMaxKills = fullRewardMaxKills;
        this.halfRewardMaxKills = halfRewardMaxKills;
        this.halfRewardMultiplier = halfRewardMultiplier;
    }

    @Override
    public double registerKillAndGetMultiplier(UUID killer, UUID victim) {
        FarmState state = states.computeIfAbsent(killer, k -> new FarmState());
        synchronized (state) {
            if (!victim.equals(state.lastVictim)) {
                state.lastVictim = victim;
                state.consecutiveCount = 0;
            }
            state.consecutiveCount++;
            return multiplierFor(state.consecutiveCount);
        }
    }

    @Override
    public void clear(UUID killer) {
        states.remove(killer);
    }

    private double multiplierFor(int consecutiveCount) {
        if (consecutiveCount <= fullRewardMaxKills) {
            return 1.0;
        }
        if (consecutiveCount <= halfRewardMaxKills) {
            return halfRewardMultiplier;
        }
        return 0.0;
    }
}
