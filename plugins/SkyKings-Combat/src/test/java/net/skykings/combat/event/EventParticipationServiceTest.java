package net.skykings.combat.event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventParticipationServiceTest {
    private EventParticipationService service;

    @Before
    public void setUp() {
        service = new EventParticipationService();
    }

    @After
    public void tearDown() {
        service.clear();
    }

    @Test
    public void playerCannotJoinTwoEventsAtOnce() {
        UUID player = UUID.randomUUID();
        assertTrue(service.join(player, EventParticipationService.Type.DUEL, "d-1"));
        assertFalse(service.join(player, EventParticipationService.Type.LMS, "lms-1"));

        EventParticipationService.Participation state = service.get(player);
        assertNotNull(state);
        assertEquals(EventParticipationService.Type.DUEL, state.getType());
        assertEquals("d-1", state.getSessionId());
    }

    @Test
    public void sameSessionRequiresTypeAndSessionId() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();

        assertTrue(service.join(first, EventParticipationService.Type.CLAN_WAR, "cw-42"));
        assertTrue(service.join(second, EventParticipationService.Type.CLAN_WAR, "cw-42"));
        assertTrue(service.join(third, EventParticipationService.Type.CLAN_WAR, "cw-99"));
        assertTrue(service.join(fourth, EventParticipationService.Type.DUEL, "cw-42"));

        assertTrue(service.isSameSession(first, second));
        assertFalse(service.isSameSession(first, third));
        assertFalse(service.isSameSession(first, fourth));
    }

    @Test
    public void leavingEventFreesPlayerForNextController() {
        UUID player = UUID.randomUUID();
        assertTrue(service.join(player, EventParticipationService.Type.LMS, "lms-1"));
        assertTrue(service.isInEvent(player));

        service.leave(player);
        assertFalse(service.isInEvent(player));
        assertNull(service.get(player));
        assertTrue(service.join(player, EventParticipationService.Type.CLAN_WAR, "cw-2"));
    }

    @Test
    public void invalidParticipationCannotBeRegistered() {
        UUID player = UUID.randomUUID();
        assertFalse(service.join(null, EventParticipationService.Type.DUEL, "d-1"));
        assertFalse(service.join(player, null, "d-1"));
        assertFalse(service.join(player, EventParticipationService.Type.DUEL, null));
        assertFalse(service.join(player, EventParticipationService.Type.DUEL, "   "));
        assertTrue(service.snapshot().isEmpty());
    }
}
