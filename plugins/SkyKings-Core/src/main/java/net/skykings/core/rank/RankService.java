package net.skykings.core.rank;

import net.skykings.core.model.Rank;

import java.util.UUID;

public interface RankService {

    Rank getRank(UUID uuid);

    void setRank(UUID uuid, Rank rank);

    void setRank(UUID uuid, Rank rank, String actor);

    boolean hasAtLeast(UUID uuid, Rank rank);
}
