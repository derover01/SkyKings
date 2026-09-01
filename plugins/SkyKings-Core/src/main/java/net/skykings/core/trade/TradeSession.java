package net.skykings.core.trade;

import java.util.UUID;

/** Eine laufende Zwei-Spieler-Handelssitzung. */
public final class TradeSession {
    private final UUID id = UUID.randomUUID();
    private final TradeOffer left;
    private final TradeOffer right;
    private boolean finished;
    /**
     * Jede Aenderung an Angebot oder Zustimmung erhoeht diese Revision. Ein Countdown darf
     * nur abschliessen, wenn exakt dieselbe Revision nach 3 Sekunden noch aktuell ist.
     */
    private long acceptanceRevision;

    public TradeSession(UUID leftPlayer, UUID rightPlayer) {
        this.left = new TradeOffer(leftPlayer);
        this.right = new TradeOffer(rightPlayer);
    }

    public UUID getId() { return id; }
    public TradeOffer getLeft() { return left; }
    public TradeOffer getRight() { return right; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public long getAcceptanceRevision() { return acceptanceRevision; }
    public long bumpAcceptanceRevision() { return ++acceptanceRevision; }

    public TradeOffer offerOf(UUID player) {
        if (left.getPlayer().equals(player)) return left;
        if (right.getPlayer().equals(player)) return right;
        return null;
    }

    public TradeOffer otherOf(UUID player) {
        if (left.getPlayer().equals(player)) return right;
        if (right.getPlayer().equals(player)) return left;
        return null;
    }

    public boolean bothAccepted() { return left.isAccepted() && right.isAccepted(); }
}
