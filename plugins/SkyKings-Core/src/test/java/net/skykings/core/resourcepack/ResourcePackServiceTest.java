package net.skykings.core.resourcepack;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ResourcePackServiceTest {

    @Test
    public void acceptsDirectAsciiHttpsUrl() {
        assertNull(ResourcePackService.validationError("https://cdn.skykings.de/SkyKings-ResourcePack-1.8.9.zip"));
    }

    @Test
    public void rejectsEmptyHttpAndFragmentUrls() {
        assertNotNull(ResourcePackService.validationError(""));
        assertNotNull(ResourcePackService.validationError("http://cdn.skykings.de/pack.zip"));
        assertNotNull(ResourcePackService.validationError("https://cdn.skykings.de/pack.zip#latest"));
    }

    @Test
    public void rejectsNonAsciiAndOverlongUrls() {
        assertNotNull(ResourcePackService.validationError("https://cdn.skykings.de/päck.zip"));
        StringBuilder longUrl = new StringBuilder("https://cdn.skykings.de/");
        while (longUrl.length() <= 255) longUrl.append('a');
        assertNotNull(ResourcePackService.validationError(longUrl.toString()));
    }
}
