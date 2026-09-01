package net.skykings.core.trade;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TradeServiceTest {

    @Test
    public void acceptedRequestCreatesExactlyOneSharedSession() {
        TradeService service = new TradeService();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertTrue(service.request(a, b));
        TradeSession first = service.accept(b, a);
        assertNotNull(first);
        assertSame(first, service.get(a));
        assertSame(first, service.get(b));
        assertNull(service.accept(b, a));
    }

    @Test
    public void activePlayerCannotEnterSecondTrade() {
        TradeService service = new TradeService();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        assertTrue(service.request(a, b));
        assertNotNull(service.accept(b, a));
        assertFalse(service.request(a, c));
        assertFalse(service.request(c, b));
    }

    @Test
    public void finishReleasesBothPlayers() {
        TradeService service = new TradeService();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        assertTrue(service.request(a, b));
        TradeSession session = service.accept(b, a);
        assertNotNull(session);
        service.finish(session);

        assertNull(service.get(a));
        assertNull(service.get(b));
        assertTrue(service.request(a, c));
    }

    @Test
    public void offerOrAcceptanceRevisionInvalidatesOlderCountdownToken() {
        TradeSession session = new TradeSession(UUID.randomUUID(), UUID.randomUUID());
        long firstCountdown = session.getAcceptanceRevision();
        assertTrue(session.isAcceptanceRevision(firstCountdown));

        session.bumpAcceptanceRevision();
        assertFalse(session.isAcceptanceRevision(firstCountdown));

        long replacementCountdown = session.getAcceptanceRevision();
        assertTrue(session.isAcceptanceRevision(replacementCountdown));
        session.bumpAcceptanceRevision();
        assertFalse(session.isAcceptanceRevision(replacementCountdown));
    }
}
